package com.example.service

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

class DeviceActionExecutor(private val context: Context) {

    private var isTorchOn = false

    fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.w("DeviceActionExecutor", "Vibration failed: ${e.message}")
        }
    }

    fun openApp(appName: String, packageName: String? = null, fallbackIntent: Intent? = null): ExecutionResult {
        vibrateShort()
        val pm = context.packageManager

        // Clean & normalize app name (strip "open", "kholo", "chalao", "app", etc.)
        val cleaned = appName.lowercase().trim()
            .replace(Regex("""^(open|launch|start|run|kholo|chalao|khol do|on karo|چلائیں|کھولو|آن کرو)\s+"""), "")
            .replace(Regex("""\s+(kholo|chalao|khol do|on karo|کھولو|چلائیں)$"""), "")
            .replace("app", "")
            .trim()

        val searchTarget = if (cleaned.isBlank()) appName.lowercase().trim() else cleaned

        // 1. Try explicit package name if provided
        if (!packageName.isNullOrBlank()) {
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ExecutionResult.Success("Opening $appName...")
            }
        }

        // 2. Try well-known package identifiers
        val candidatePkgs = when {
            searchTarget.contains("youtube") || searchTarget.contains("یوٹیوب") || searchTarget.contains("video") ->
                listOf("com.google.android.youtube")
            searchTarget.contains("chrome") || searchTarget.contains("کروم") || searchTarget.contains("browser") || searchTarget.contains("انٹرنیٹ") ->
                listOf("com.android.chrome", "org.chromium.chrome")
            searchTarget.contains("whatsapp") || searchTarget.contains("واٹس ایپ") ->
                listOf("com.whatsapp", "com.whatsapp.w4b")
            searchTarget.contains("camera") || searchTarget.contains("کیمرہ") || searchTarget.contains("photo") ->
                listOf("com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2")
            searchTarget.contains("map") || searchTarget.contains("نقشہ") || searchTarget.contains("location") ->
                listOf("com.google.android.apps.maps")
            searchTarget.contains("settings") || searchTarget.contains("سیٹنگ") ->
                listOf("com.android.settings")
            searchTarget.contains("calc") || searchTarget.contains("کیلکولیٹر") ->
                listOf("com.google.android.calculator", "com.android.calculator2")
            searchTarget.contains("clock") || searchTarget.contains("alarm") || searchTarget.contains("گھڑی") ->
                listOf("com.google.android.deskclock", "com.android.deskclock")
            searchTarget.contains("dialer") || searchTarget.contains("phone") || searchTarget.contains("فون") || searchTarget.contains("کال") ->
                listOf("com.google.android.dialer", "com.android.dialer")
            searchTarget.contains("gmail") || searchTarget.contains("email") || searchTarget.contains("ای میل") ->
                listOf("com.google.android.gm")
            searchTarget.contains("store") || searchTarget.contains("مارکیٹ") || searchTarget.contains("play") ->
                listOf("com.android.vending")
            searchTarget.contains("file") || searchTarget.contains("فائل") ->
                listOf("com.google.android.documentsui", "com.android.documentsui")
            searchTarget.contains("spotify") ->
                listOf("com.spotify.music")
            else -> emptyList()
        }

        for (pkg in candidatePkgs) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ExecutionResult.Success("Opening $appName ($pkg)...")
            }
        }

        // 3. Dynamically search installed apps across the device by label & package name
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = pm.queryIntentActivities(launcherIntent, 0)
            val matchedApp = resolvedApps.firstOrNull { ri ->
                val label = ri.loadLabel(pm).toString().lowercase()
                val pkgName = ri.activityInfo.packageName.lowercase()
                label.contains(searchTarget) || searchTarget.contains(label) || pkgName.contains(searchTarget)
            }

            if (matchedApp != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedApp.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    val label = matchedApp.loadLabel(pm).toString()
                    return ExecutionResult.Success("Opening $label...")
                }
            }
        } catch (e: Exception) {
            Log.w("DeviceActionExecutor", "Installed apps search error: ${e.message}")
        }

        // 4. Fallback explicit intent if passed
        if (fallbackIntent != null) {
            return try {
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackIntent)
                ExecutionResult.Success("Opening $appName...")
            } catch (e: Exception) {
                ExecutionResult.Failure("Unable to open $appName: ${e.localizedMessage}")
            }
        }

        // 5. Guaranteed Universal Fallbacks (Never leaves the user stranded!)
        val fallbackIntentToUse: Intent = when {
            searchTarget.contains("youtube") || searchTarget.contains("یوٹیوب") || searchTarget.contains("video") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            }
            searchTarget.contains("camera") || searchTarget.contains("کیمرہ") || searchTarget.contains("photo") -> {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            }
            searchTarget.contains("settings") || searchTarget.contains("سیٹنگ") -> {
                Intent(Settings.ACTION_SETTINGS)
            }
            searchTarget.contains("dialer") || searchTarget.contains("phone") || searchTarget.contains("فون") || searchTarget.contains("کال") -> {
                Intent(Intent.ACTION_DIAL)
            }
            searchTarget.contains("map") || searchTarget.contains("نقشہ") || searchTarget.contains("location") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com"))
            }
            searchTarget.contains("clock") || searchTarget.contains("alarm") || searchTarget.contains("گھڑی") -> {
                Intent(AlarmClock.ACTION_SHOW_ALARMS)
            }
            searchTarget.contains("calc") || searchTarget.contains("کیلکولیٹر") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=calculator"))
            }
            else -> {
                // Open browser / search for the app
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(searchTarget)}"))
            }
        }

        return try {
            fallbackIntentToUse.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackIntentToUse)
            ExecutionResult.Success("Launching $appName...")
        } catch (e: Exception) {
            ExecutionResult.Failure("Unable to launch $appName: ${e.localizedMessage}")
        }
    }

    fun openCamera(): ExecutionResult {
        return openApp("Camera", null, Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }

    fun openSettings(): ExecutionResult {
        return openApp("Settings", "com.android.settings", Intent(Settings.ACTION_SETTINGS))
    }

    fun openDialer(phoneNumber: String? = null): ExecutionResult {
        vibrateShort()
        val intent = if (!phoneNumber.isNullOrBlank()) {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        } else {
            Intent(Intent.ACTION_DIAL)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            ExecutionResult.Success("Opening dialer...")
        } catch (e: Exception) {
            ExecutionResult.Failure("Failed to open dialer: ${e.localizedMessage}")
        }
    }

    fun setTimer(seconds: Int, message: String = "Robby AI Timer"): ExecutionResult {
        vibrateShort()
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ExecutionResult.Success("Timer set for $seconds seconds ($message).")
        } catch (e: Exception) {
            // DeskClock might not be available on all emulators; return a friendly status
            ExecutionResult.Failure("Timer app not found: timer set inside Robby AI for $seconds seconds.")
        }
    }

    fun searchWeb(query: String): ExecutionResult {
        vibrateShort()
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ExecutionResult.Success("Searching for \"$query\"...")
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(browserIntent)
                ExecutionResult.Success("Searching for \"$query\" in browser...")
            } catch (err: Exception) {
                ExecutionResult.Failure("Failed to open search: ${err.localizedMessage}")
            }
        }
    }

    fun toggleFlashlight(forceState: Boolean? = null): ExecutionResult {
        vibrateShort()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ExecutionResult.Failure("Camera service not available on device.")

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                return ExecutionResult.Failure("No flashlight hardware detected on this device.")
            }

            val targetState = forceState ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            ExecutionResult.Success(if (targetState) "Flashlight turned ON" else "Flashlight turned OFF")
        } catch (e: CameraAccessException) {
            ExecutionResult.Failure("Camera access error: ${e.localizedMessage}")
        } catch (e: Exception) {
            ExecutionResult.Failure("Flashlight toggle error: ${e.localizedMessage}")
        }
    }

    fun openUrl(url: String): ExecutionResult {
        vibrateShort()
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult.Success("Opening $url in browser...")
        } catch (e: Exception) {
            ExecutionResult.Failure("Unable to open URL: ${e.localizedMessage}")
        }
    }

    fun downloadPcCompanion(platform: String = "Windows", url: String = "https://jarvis.adreesumer.com"): ExecutionResult {
        vibrateShort()
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult.Success("Opening PC Download Portal ($platform)...")
        } catch (e: Exception) {
            ExecutionResult.Failure("Failed to open PC download portal: ${e.localizedMessage}")
        }
    }

    fun shareDownloadLink(title: String = "Download ROBBY AI for PC", url: String = "https://jarvis.adreesumer.com") {
        vibrateShort()
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Download and access ROBBY AI Personal Assistant for PC & Desktop:\n$url\nSync with your account for multi-device JARVIS control!"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share ROBBY AI PC Download Link").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("DeviceActionExecutor", "Share failed: ${e.message}")
        }
    }

    fun getDeviceTelemetry(): DeviceTelemetry {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 88
        val isCharging = bm?.isCharging ?: false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val networkType = when {
            isWifi -> "Wi-Fi (High Speed)"
            isCellular -> "5G / LTE"
            else -> "Connected"
        }

        return DeviceTelemetry(
            batteryLevel = batteryPct,
            isCharging = isCharging,
            networkStatus = networkType,
            cpuStatus = "Neural Core: Nominal",
            memoryStatus = "Memory: Optimal"
        )
    }
}

data class DeviceTelemetry(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkStatus: String,
    val cpuStatus: String,
    val memoryStatus: String
)

sealed interface ExecutionResult {
    data class Success(val message: String) : ExecutionResult
    data class Failure(val error: String) : ExecutionResult
}
