package com.example.ai

import android.util.Log
import com.example.data.model.DeviceAction
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class RobbyAiService {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val systemPrompt = """
        You are ROBBY AI, a sophisticated personal AI assistant designed with a futuristic JARVIS-like persona.
        You seamlessly understand and speak both Urdu (اردو) and English.
        - If the user talks to you in Urdu, reply in elegant, natural Urdu (اردو).
        - If the user talks to you in English, reply in sharp, polite, and confident English (e.g. "At your command, Sir.", "Right away, Commander.").
        - You can perform device operations. When you identify a user request that involves a device operation, include a structured tag at the very end of your response:
          - Opening apps: [ACTION:OPEN_APP:chrome], [ACTION:OPEN_APP:youtube], [ACTION:OPEN_APP:camera], [ACTION:OPEN_APP:whatsapp], [ACTION:OPEN_APP:settings], [ACTION:OPEN_APP:maps], [ACTION:OPEN_APP:phone]
          - Setting timers: [ACTION:SET_TIMER:300] (number of seconds)
          - Flashlight: [ACTION:FLASHLIGHT:ON] or [ACTION:FLASHLIGHT:OFF]
          - Web Search: [ACTION:SEARCH:search terms]
          - Taking notes: [ACTION:NOTE:title|content]
          - System status: [ACTION:STATUS]
          - PC Companion / Desktop download: [ACTION:DOWNLOAD_PC:windows]
        Keep your verbal reply concise (1-3 sentences) so it sounds natural when spoken aloud via Text-to-Speech.
    """.trimIndent()

    suspend fun processCommand(userQuery: String, conversationHistory: String = ""): AiAssistantResult {
        return try {
            val fullPrompt = buildString {
                append(systemPrompt)
                append("\n\n")
                if (conversationHistory.isNotBlank()) {
                    append("Recent Context:\n")
                    append(conversationHistory.takeLast(1000))
                    append("\n\n")
                }
                append("User: ")
                append(userQuery)
                append("\nRobby AI: ")
            }

            val response = withTimeout(60_000L) {
                model.generateContent(fullPrompt)
            }

            val rawText = response.text.orEmpty().trim()
            if (rawText.isBlank()) {
                val fallback = if (userQuery.any { it in '\u0600'..'\u06FF' }) {
                    "معذرت، میں آپ کا حکم نہیں سمجھ سکا۔ براہ کرم دوبارہ فرمائیں۔"
                } else {
                    "I am standing by. Could you please repeat your command?"
                }
                return AiAssistantResult(displayText = fallback, spokenText = fallback, action = null)
            }

            parseResponseAndAction(rawText, userQuery)
        } catch (e: TimeoutCancellationException) {
            Log.e("RobbyAiService", "AI generation timed out", e)
            val msg = if (userQuery.any { it in '\u0600'..'\u06FF' }) {
                "سسٹم کنکشن میں تاخیر ہو رہی ہے۔ براہ کرم دوبارہ کوشش کریں۔"
            } else {
                "Neural link timed out. Please try again."
            }
            AiAssistantResult(displayText = msg, spokenText = msg, action = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("RobbyAiService", "Firebase AI generation failed: ${e.message}", e)
            // Even if offline or API quota issue, provide smart offline heuristic fallback so Robby AI always responds!
            fallbackHeuristicCommand(userQuery)
        }
    }

    private fun parseResponseAndAction(rawText: String, userQuery: String): AiAssistantResult {
        var cleanText = rawText
        var action: DeviceAction? = null

        val actionRegex = Regex("""\[ACTION:([^\]]+)\]""")
        val match = actionRegex.find(rawText)

        if (match != null) {
            cleanText = rawText.replace(match.value, "").trim()
            val actionPayload = match.groupValues[1]
            val parts = actionPayload.split(":", limit = 3)
            val type = parts.getOrNull(0)?.uppercase()

            when (type) {
                "OPEN_APP" -> {
                    val app = parts.getOrNull(1)?.lowercase() ?: ""
                    action = DeviceAction.OpenApp(appName = app, packageName = null)
                }
                "SET_TIMER" -> {
                    val seconds = parts.getOrNull(1)?.toIntOrNull() ?: 60
                    action = DeviceAction.SetTimer(seconds = seconds)
                }
                "FLASHLIGHT" -> {
                    val state = parts.getOrNull(1)?.uppercase() == "ON"
                    action = DeviceAction.ToggleFlashlight(turnOn = state)
                }
                "SEARCH" -> {
                    val q = parts.getOrNull(1).orEmpty()
                    action = DeviceAction.SearchWeb(query = q.ifBlank { userQuery })
                }
                "NOTE" -> {
                    val noteParts = (parts.getOrNull(1) ?: "").split("|", limit = 2)
                    val title = noteParts.getOrNull(0)?.ifBlank { "Voice Note" } ?: "Voice Note"
                    val body = noteParts.getOrNull(1)?.ifBlank { userQuery } ?: userQuery
                    action = DeviceAction.TakeNote(title = title, content = body)
                }
                "STATUS" -> {
                    action = DeviceAction.CheckBattery
                }
                "PC_CONTROL" -> {
                    val act = parts.getOrNull(1)?.lowercase() ?: "ping"
                    val param = parts.getOrNull(2).orEmpty()
                    action = DeviceAction.PcCommand(action = act, target = param)
                }
            }
        } else {
            // Check heuristic if no action tag was generated
            action = detectImplicitAction(userQuery)
        }

        return AiAssistantResult(
            displayText = cleanText,
            spokenText = cleanText,
            action = action
        )
    }

    private fun detectImplicitAction(query: String): DeviceAction? {
        val q = query.lowercase().trim()

        // 1. PC Specific Directives
        val isPc = q.contains("pc") || q.contains("computer") || q.contains("کمپیوٹر") || q.contains("laptop")
        if (isPc) {
            return when {
                q.contains("youtube") || q.contains("یوٹیوب") -> DeviceAction.PcCommand("open_url", "https://www.youtube.com")
                q.contains("chrome") || q.contains("کروم") || q.contains("browser") -> DeviceAction.PcCommand("open_app", "chrome")
                q.contains("notepad") || q.contains("نوٹ پیڈ") -> DeviceAction.PcCommand("open_app", "notepad")
                q.contains("calc") || q.contains("کیلکولیٹر") -> DeviceAction.PcCommand("open_app", "calc")
                q.contains("explorer") || q.contains("file") || q.contains("فائل") -> DeviceAction.PcCommand("open_app", "explorer")
                q.contains("lock") || q.contains("لاک") -> DeviceAction.PcCommand("power", "lock")
                q.contains("vol_up") || q.contains("volume up") || q.contains("awaz barhao") || q.contains("آواز بڑھاؤ") -> DeviceAction.PcCommand("media", "vol_up")
                q.contains("vol_down") || q.contains("volume down") || q.contains("awaz kam") || q.contains("آواز کم") -> DeviceAction.PcCommand("media", "vol_down")
                q.contains("mute") || q.contains("میوٹ") -> DeviceAction.PcCommand("media", "mute")
                q.contains("play") || q.contains("pause") || q.contains("روکو") || q.contains("چلاؤ") -> DeviceAction.PcCommand("media", "play_pause")
                q.contains("shutdown") || q.contains("band karo") || q.contains("بند کرو") -> DeviceAction.PcCommand("power", "shutdown")
                q.contains("restart") || q.contains("ری سٹارٹ") -> DeviceAction.PcCommand("power", "restart")
                q.contains("type") || q.contains("ٹائپ") -> DeviceAction.PcCommand("type", q.substringAfter("type").substringAfter("ٹائپ").trim())
                q.contains("download") || q.contains("ڈاؤنلوڈ") -> DeviceAction.DownloadPc()
                else -> DeviceAction.PcCommand("ping")
            }
        }

        // 2. Mobile App & Hardware Directives
        return when {
            q.contains("chrome") || q.contains("کروم") || q.contains("browser") ->
                DeviceAction.OpenApp("Chrome", "com.android.chrome")
            q.contains("youtube") || q.contains("یوٹیوب") ->
                DeviceAction.OpenApp("YouTube", "com.google.android.youtube")
            q.contains("camera") || q.contains("کیمرہ") || q.contains("photo") ->
                DeviceAction.OpenApp("Camera", null)
            q.contains("whatsapp") || q.contains("واٹس ایپ") ->
                DeviceAction.OpenApp("WhatsApp", "com.whatsapp")
            q.contains("settings") || q.contains("سیٹنگ") ->
                DeviceAction.OpenApp("Settings", "com.android.settings")
            q.contains("map") || q.contains("نقشہ") || q.contains("location") ->
                DeviceAction.OpenApp("Maps", "com.google.android.apps.maps")
            q.contains("calc") || q.contains("کیلکولیٹر") ->
                DeviceAction.OpenApp("Calculator", "com.google.android.calculator")
            q.contains("clock") || q.contains("alarm") || q.contains("گھڑی") ->
                DeviceAction.OpenApp("Clock", "com.google.android.deskclock")
            q.contains("dialer") || q.contains("call") || q.contains("فون") || q.contains("کال") ->
                DeviceAction.OpenDialer
            q.contains("flashlight") || q.contains("ٹارچ") || q.contains("فلیش") ->
                DeviceAction.ToggleFlashlight(turnOn = !q.contains("off") && !q.contains("بند"))
            q.contains("timer") || q.contains("ٹائمر") ->
                DeviceAction.SetTimer(seconds = 300)
            q.contains("note") || q.contains("نوٹ") || q.contains("یاد دہانی") ->
                DeviceAction.TakeNote(title = "Smart Note", content = query)
            // Catch dynamic commands like "open [app]" or "[app] kholo" / "[app] chalao"
            Regex("""^(open|launch|kholo|chalao|start|کھولو|چلائیں)\s+(.+)""").find(q) != null -> {
                val app = Regex("""^(open|launch|kholo|chalao|start|کھولو|چلائیں)\s+(.+)""").find(q)?.groupValues?.getOrNull(2) ?: ""
                DeviceAction.OpenApp(app.trim(), null)
            }
            Regex("""(.+)\s+(kholo|chalao|open karo|کھولو|چلائیں)$""").find(q) != null -> {
                val app = Regex("""(.+)\s+(kholo|chalao|open karo|کھولو|چلائیں)$""").find(q)?.groupValues?.getOrNull(1) ?: ""
                DeviceAction.OpenApp(app.trim(), null)
            }
            else -> null
        }
    }

    private fun fallbackHeuristicCommand(query: String): AiAssistantResult {
        val q = query.lowercase().trim()
        val isUrdu = query.any { it in '\u0600'..'\u06FF' }

        val action = detectImplicitAction(query)
        val text = when {
            action is DeviceAction.DownloadPc -> {
                if (isUrdu) "پی سی ڈیسک ٹاپ کلائنٹ ڈاؤن لوڈ کا پورٹل کھولا جا رہا ہے..." else "Opening ROBBY AI PC Desktop download portal at jarvis.adreesumer.com..."
            }
            action is DeviceAction.OpenApp -> {
                if (isUrdu) "${action.appName} کھولی جا رہی ہے..." else "Opening ${action.appName} for you now, Commander."
            }
            action is DeviceAction.ToggleFlashlight -> {
                if (isUrdu) "فلیش لائٹ ٹوگل کی جا رہی ہے۔" else "Toggling device flashlight."
            }
            action is DeviceAction.SetTimer -> {
                if (isUrdu) "ٹائمر سیٹ کیا جا رہا ہے۔" else "Setting timer per your instructions."
            }
            action is DeviceAction.TakeNote -> {
                if (isUrdu) "آپ کا نوٹ محفوظ کیا جا رہا ہے۔" else "Note has been logged to your secure cloud storage."
            }
            isUrdu -> {
                "جی جناب! روبائی اے آئی حاضر ہے۔ آپ کا حکم: \"$query\""
            }
            else -> {
                "Robby AI operational. Processing command: \"$query\""
            }
        }

        return AiAssistantResult(
            displayText = text,
            spokenText = text,
            action = action
        )
    }
}

data class AiAssistantResult(
    val displayText: String,
    val spokenText: String,
    val action: DeviceAction?
)
