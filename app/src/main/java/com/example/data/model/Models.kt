package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val name: String = "Commander",
    val language: String = "en", // "en" or "ur"
    val voiceEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val hudTheme: String = "cyan_arc", // "cyan_arc", "emerald_matrix", "crimson_core"
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user", // "user" or "assistant"
    val content: String = "",
    val language: String = "en",
    val actionExecuted: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuickNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "General",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class HudThemeType(val id: String, val title: String) {
    CYAN_ARC("cyan_arc", "Arc Reactor (Cyan)"),
    EMERALD_MATRIX("emerald_matrix", "Emerald Matrix (Green)"),
    CRIMSON_CORE("crimson_core", "Crimson Core (Red)")
}

sealed interface DeviceAction {
    data class OpenApp(val appName: String, val packageName: String?, val fallbackIntentAction: String? = null) : DeviceAction
    data class SetTimer(val seconds: Int, val message: String = "Robby Timer") : DeviceAction
    data class ToggleFlashlight(val turnOn: Boolean) : DeviceAction
    data class SearchWeb(val query: String) : DeviceAction
    data class TakeNote(val title: String, val content: String) : DeviceAction
    data object CheckBattery : DeviceAction
    data object OpenDialer : DeviceAction
    data class DownloadPc(val platform: String = "Windows", val url: String = "https://jarvis.adreesumer.com") : DeviceAction
    data class OpenWebUrl(val url: String) : DeviceAction
    data class GeneralResponse(val text: String) : DeviceAction
    data class PcCommand(val action: String, val target: String = "", val extra: String = "") : DeviceAction
}
