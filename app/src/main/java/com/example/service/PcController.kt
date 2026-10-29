package com.example.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class PcConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

sealed interface PcResult {
    data class Success(val message: String) : PcResult
    data class Failure(val error: String) : PcResult
}

data class PcVoiceDirective(
    val action: String,
    val target: String,
    val extra: String = "",
    val feedbackMessageEn: String,
    val feedbackMessageUr: String
)

class PcController(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    // 10.0.2.2 is the special alias to host computer when running on Android emulator;
    // On physical Android devices, users enter their PC's local Wi-Fi IP (e.g. 192.168.1.15)
    private val _pcHost = MutableStateFlow("10.0.2.2")
    val pcHost: StateFlow<String> = _pcHost.asStateFlow()

    private val _pcPort = MutableStateFlow(8765)
    val pcPort: StateFlow<Int> = _pcPort.asStateFlow()

    private val _connectionState = MutableStateFlow(PcConnectionState.DISCONNECTED)
    val connectionState: StateFlow<PcConnectionState> = _connectionState.asStateFlow()

    private val _lastStatusMessage = MutableStateFlow("PC Bridge Standby. Start robby_pc_bridge.py on PC.")
    val lastStatusMessage: StateFlow<String> = _lastStatusMessage.asStateFlow()

    fun updatePcAddress(host: String, port: Int = 8765) {
        _pcHost.value = host.trim()
        _pcPort.value = port
    }

    suspend fun pingPc(targetHost: String? = null, targetPort: Int? = null): PcResult {
        val host = (targetHost ?: _pcHost.value).trim().ifBlank { "10.0.2.2" }
        val port = targetPort ?: _pcPort.value
        _pcHost.value = host
        _pcPort.value = port
        _connectionState.value = PcConnectionState.CONNECTING
        _lastStatusMessage.value = "Pinging PC at $host:$port..."

        return withContext(Dispatchers.IO) {
            try {
                val url = "http://$host:$port/status"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        var osName = "Remote PC"
                        try {
                            val json = JSONObject(body)
                            osName = json.optString("os", "Remote PC")
                        } catch (e: Exception) {
                            // ignore
                        }
                        _connectionState.value = PcConnectionState.CONNECTED
                        val msg = "Connected to PC ($osName) at $host:$port"
                        _lastStatusMessage.value = msg
                        PcResult.Success(msg)
                    } else {
                        _connectionState.value = PcConnectionState.ERROR
                        val msg = "PC responded with code ${response.code}"
                        _lastStatusMessage.value = msg
                        PcResult.Failure(msg)
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = PcConnectionState.ERROR
                val err = "Cannot reach PC at $host:$port. Ensure robby_pc_bridge.py is running."
                _lastStatusMessage.value = err
                Log.w("PcController", "Ping failed: ${e.message}")
                PcResult.Failure(err)
            }
        }
    }

    suspend fun sendCommand(action: String, target: String = "", extra: String = ""): PcResult {
        val host = _pcHost.value.trim().ifBlank { "10.0.2.2" }
        val port = _pcPort.value

        return withContext(Dispatchers.IO) {
            try {
                val url = "http://$host:$port/api/command"
                val jsonPayload = JSONObject().apply {
                    put("action", action)
                    put("target", target)
                    put("extra", extra)
                }

                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val respString = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        _connectionState.value = PcConnectionState.CONNECTED
                        var serverMessage = "PC action executed: $action $target"
                        try {
                            val json = JSONObject(respString)
                            if (json.has("message")) {
                                serverMessage = json.getString("message")
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                        _lastStatusMessage.value = serverMessage
                        PcResult.Success(serverMessage)
                    } else {
                        _connectionState.value = PcConnectionState.ERROR
                        val err = "PC rejected command (${response.code})"
                        _lastStatusMessage.value = err
                        PcResult.Failure(err)
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = PcConnectionState.ERROR
                val err = "Could not connect to PC at $host:$port. Run robby_pc_bridge.py on PC."
                _lastStatusMessage.value = err
                Log.w("PcController", "Command failed: ${e.message}")
                PcResult.Failure(err)
            }
        }
    }

    fun parseVoicePcDirective(query: String): PcVoiceDirective? {
        val q = query.lowercase().trim()
        val isPcTarget = q.contains("pc") || q.contains("computer") || q.contains("کمپیوٹر") || q.contains("laptop")

        if (!isPcTarget) return null

        return when {
            q.contains("youtube") || q.contains("یوٹیوب") -> {
                PcVoiceDirective(
                    action = "open_url",
                    target = "https://www.youtube.com",
                    feedbackMessageEn = "Opening YouTube on your Computer...",
                    feedbackMessageUr = "کمپیوٹر پر یوٹیوب کھولا جا رہا ہے..."
                )
            }
            q.contains("chrome") || q.contains("کروم") || q.contains("browser") -> {
                PcVoiceDirective(
                    action = "open_app",
                    target = "chrome",
                    feedbackMessageEn = "Opening Google Chrome on your Computer...",
                    feedbackMessageUr = "کمپیوٹر پر گوگل کروم کھولا جا رہا ہے..."
                )
            }
            q.contains("notepad") || q.contains("نوٹ پیڈ") -> {
                PcVoiceDirective(
                    action = "open_app",
                    target = "notepad",
                    feedbackMessageEn = "Launching Notepad on PC...",
                    feedbackMessageUr = "کمپیوٹر پر نوٹ پیڈ کھولا جا رہا ہے..."
                )
            }
            q.contains("calc") || q.contains("کیلکولیٹر") -> {
                PcVoiceDirective(
                    action = "open_app",
                    target = "calc",
                    feedbackMessageEn = "Opening Calculator on PC...",
                    feedbackMessageUr = "کمپیوٹر پر کیلکولیٹر کھولا جا رہا ہے..."
                )
            }
            q.contains("explorer") || q.contains("files") || q.contains("فائل") -> {
                PcVoiceDirective(
                    action = "open_app",
                    target = "explorer",
                    feedbackMessageEn = "Opening File Explorer on PC...",
                    feedbackMessageUr = "کمپیوٹر پر فائل مینیجر کھولا جا رہا ہے..."
                )
            }
            q.contains("lock") || q.contains("لاک") -> {
                PcVoiceDirective(
                    action = "power",
                    target = "lock",
                    feedbackMessageEn = "Locking PC workstation screen now.",
                    feedbackMessageUr = "کمپیوٹر اسکرین لاک کی جا رہی ہے۔"
                )
            }
            q.contains("volume up") || q.contains("awaz barhao") || q.contains("آواز بڑھاؤ") || q.contains("sound up") -> {
                PcVoiceDirective(
                    action = "media",
                    target = "vol_up",
                    feedbackMessageEn = "Increasing PC Master Volume.",
                    feedbackMessageUr = "کمپیوٹر کی آواز بڑھائی جا رہی ہے۔"
                )
            }
            q.contains("volume down") || q.contains("awaz kam") || q.contains("آواز کم") || q.contains("sound down") -> {
                PcVoiceDirective(
                    action = "media",
                    target = "vol_down",
                    feedbackMessageEn = "Lowering PC Master Volume.",
                    feedbackMessageUr = "کمپیوٹر کی آواز کم کی جا رہی ہے۔"
                )
            }
            q.contains("mute") || q.contains("میوٹ") || q.contains("silent") -> {
                PcVoiceDirective(
                    action = "media",
                    target = "mute",
                    feedbackMessageEn = "Muting PC audio.",
                    feedbackMessageUr = "کمپیوٹر کی آواز میوٹ کی جا رہی ہے۔"
                )
            }
            q.contains("play") || q.contains("pause") || q.contains("روکو") || q.contains("چلاؤ") -> {
                PcVoiceDirective(
                    action = "media",
                    target = "play_pause",
                    feedbackMessageEn = "Toggling PC media playback.",
                    feedbackMessageUr = "کمپیوٹر پر میڈیا پلے/پاز کیا جا رہا ہے۔"
                )
            }
            q.contains("shutdown") || q.contains("band karo") || q.contains("بند کرو") || q.contains("off karo") -> {
                PcVoiceDirective(
                    action = "power",
                    target = "shutdown",
                    feedbackMessageEn = "Initiating PC shutdown sequence in 60s.",
                    feedbackMessageUr = "کمپیوٹر شٹ ڈاؤن کا عمل شروع کیا جا رہا ہے۔"
                )
            }
            q.contains("restart") || q.contains("ری سٹارٹ") -> {
                PcVoiceDirective(
                    action = "power",
                    target = "restart",
                    feedbackMessageEn = "Initiating PC restart sequence in 60s.",
                    feedbackMessageUr = "کمپیوٹر ری اسٹارٹ کیا جا رہا ہے۔"
                )
            }
            q.contains("type") || q.contains("ٹائپ") -> {
                val toType = q.substringAfter("type").substringAfter("ٹائپ").trim()
                PcVoiceDirective(
                    action = "type",
                    target = toType,
                    feedbackMessageEn = "Typing \"$toType\" on PC...",
                    feedbackMessageUr = "کمپیوٹر پر ٹائپ کیا جا رہا ہے..."
                )
            }
            else -> null
        }
    }

    fun getPythonBridgeScript(): String {
        return """
# ============================================================
# 🤖 ROBBY AI — PC REMOTE CONTROL & BRIDGE SERVER
# Fully controls Windows / Mac / Linux from ROBBY AI Android!
# ============================================================
# Run this once in Python (No pip installs needed!):
#   python robby_pc_bridge.py
# ============================================================

import http.server
import socketserver
import json
import os
import sys
import platform
import subprocess
import webbrowser
import socket

PORT = 8765

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

class RobbyBridgeHandler(http.server.BaseHTTPRequestHandler):
    def _send_json(self, code, payload):
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()
        self.wfile.write(json.dumps(payload).encode("utf-8"))

    def do_OPTIONS(self):
        self._send_json(200, {"status": "ok"})

    def do_GET(self):
        if self.path in ["/", "/status", "/ping"]:
            self._send_json(200, {
                "status": "online",
                "service": "ROBBY_AI_PC_BRIDGE",
                "os": platform.system(),
                "hostname": socket.gethostname()
            })
        else:
            self._send_json(404, {"error": "Not Found"})

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length).decode("utf-8")
        try:
            req = json.loads(body) if body else {}
        except Exception:
            req = {}

        action = req.get("action", "").lower().strip()
        target = req.get("target", "").strip()
        extra = req.get("extra", "").strip()
        os_sys = platform.system().lower()
        msg = "Executed successfully"

        try:
            if action == "open_app":
                if "chrome" in target:
                    if "windows" in os_sys:
                        os.system("start chrome")
                    elif "darwin" in os_sys:
                        os.system("open -a 'Google Chrome'")
                    else:
                        os.system("google-chrome &")
                elif "calc" in target:
                    if "windows" in os_sys:
                        subprocess.Popen("calc.exe")
                    elif "darwin" in os_sys:
                        os.system("open -a Calculator")
                    else:
                        subprocess.Popen("gnome-calculator")
                elif "notepad" in target:
                    if "windows" in os_sys:
                        subprocess.Popen("notepad.exe")
                    elif "darwin" in os_sys:
                        os.system("open -a TextEdit")
                    else:
                        subprocess.Popen("gedit")
                elif "explorer" in target or "files" in target:
                    if "windows" in os_sys:
                        subprocess.Popen("explorer.exe")
                    elif "darwin" in os_sys:
                        os.system("open .")
                    else:
                        subprocess.Popen("nautilus .")
                elif "cmd" in target or "terminal" in target:
                    if "windows" in os_sys:
                        subprocess.Popen("start cmd.exe", shell=True)
                    elif "darwin" in os_sys:
                        os.system("open -a Terminal")
                    else:
                        subprocess.Popen("x-terminal-emulator")
                elif "vscode" in target:
                    subprocess.Popen("code")
                elif "spotify" in target:
                    if "windows" in os_sys:
                        os.system("start spotify:")
                    elif "darwin" in os_sys:
                        os.system("open -a Spotify")
                else:
                    if "windows" in os_sys:
                        os.system(f"start {target}")
                    else:
                        subprocess.Popen([target])
                msg = f"Opened {target} on PC"

            elif action == "open_url":
                url = target if target.startswith("http") else f"https://{target}"
                webbrowser.open(url)
                msg = f"Opened {url} in PC browser"

            elif action == "media":
                if "windows" in os_sys:
                    vk_map = {"vol_up": 175, "vol_down": 174, "mute": 173, "play_pause": 179, "next": 176, "prev": 177}
                    vk = vk_map.get(target, 175)
                    subprocess.run(["powershell", "-c", f"(new-object -com wscript.shell).SendKeys([char]{vk})"], check=False)
                elif "darwin" in os_sys:
                    if target == "vol_up":
                        os.system("osascript -e 'set volume output volume (output volume of (get volume settings) + 10)'")
                    elif target == "vol_down":
                        os.system("osascript -e 'set volume output volume (output volume of (get volume settings) - 10)'")
                    elif target == "mute":
                        os.system("osascript -e 'set volume output muted (not (output muted of (get volume settings)))'")
                    elif target == "play_pause":
                        os.system("osascript -e 'tell application \"System Events\" to key code 49'")
                msg = f"PC Media {target} executed"

            elif action == "power":
                if target == "lock":
                    if "windows" in os_sys:
                        os.system("rundll32.exe user32.dll,LockWorkStation")
                    elif "darwin" in os_sys:
                        os.system("pmset displaysleepnow")
                    else:
                        os.system("loginctl lock-session")
                    msg = "PC Screen Locked"
                elif target == "sleep":
                    if "windows" in os_sys:
                        os.system("rundll32.exe powrprof.dll,SetSuspendState 0,1,0")
                    msg = "PC Sleep triggered"
                elif target == "shutdown":
                    if "windows" in os_sys:
                        os.system("shutdown /s /t 60")
                    else:
                        os.system("shutdown -h +1")
                    msg = "PC Shutdown in 60s"
                elif target == "restart":
                    if "windows" in os_sys:
                        os.system("shutdown /r /t 60")
                    else:
                        os.system("shutdown -r +1")
                    msg = "PC Restart in 60s"

            elif action == "type":
                text = target or extra
                if "windows" in os_sys:
                    safe = text.replace("'", "''")
                    subprocess.run(["powershell", "-c", f"(new-object -com wscript.shell).SendKeys('{safe}')"], check=False)
                msg = f"Typed to PC: {text}"

            elif action == "key":
                if "windows" in os_sys:
                    key_map = {"enter": "{ENTER}", "esc": "{ESC}", "space": " ", "desktop": "^{ESC}", "tab": "{TAB}"}
                    k = key_map.get(target, "{ENTER}")
                    subprocess.run(["powershell", "-c", f"(new-object -com wscript.shell).SendKeys('{k}')"], check=False)
                msg = f"Sent key {target} to PC"

            self._send_json(200, {"success": True, "message": msg})
        except Exception as err:
            self._send_json(500, {"success": False, "error": str(err)})

if __name__ == "__main__":
    local_ip = get_local_ip()
    print("=" * 60)
    print("🤖 ROBBY AI — PC REMOTE BRIDGE SERVER IS ACTIVE!")
    print(f"Server Port: {PORT}")
    print(f"Your PC IP Address: {local_ip}")
    print("=" * 60)
    print("Step 1: In ROBBY AI Android App, open 'Apps/PC' Tab")
    print(f"Step 2: Enter this IP: {local_ip} and tap 'Ping'")
    print("Step 3: Full Computer Control is now ACTIVE!")
    print("=" * 60)
    server = socketserver.TCPServer(("0.0.0.0", PORT), RobbyBridgeHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping ROBBY AI PC Bridge.")
""".trimIndent()
    }
}
