package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.RobbyAiService
import com.example.data.model.ChatMessage
import com.example.data.model.DeviceAction
import com.example.data.model.QuickNote
import com.example.data.model.UserProfile
import com.example.data.repository.RobbyRepository
import com.example.service.DeviceActionExecutor
import com.example.service.DeviceTelemetry
import com.example.service.ExecutionResult
import com.example.service.PcConnectionState
import com.example.service.PcController
import com.example.service.PcResult
import com.example.service.PcVoiceDirective
import com.example.service.VoiceManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AiProcessingStage {
    IDLE,
    LISTENING,
    ANALYZING,
    EXECUTING,
    SPEAKING
}

class RobbyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RobbyRepository(application)
    private val aiService = RobbyAiService()
    val actionExecutor = DeviceActionExecutor(application)
    val voiceManager = VoiceManager(application)
    val pcController = PcController(application)
    private var currentExecutionJob: Job? = null

    private val _userProfile = MutableStateFlow(
        UserProfile(
            userId = Firebase.auth.currentUser?.uid.orEmpty(),
            name = Firebase.auth.currentUser?.displayName ?: "Commander",
            language = "en",
            voiceEnabled = true,
            hapticsEnabled = true,
            hudTheme = "cyan_arc"
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _telemetry = MutableStateFlow(actionExecutor.getDeviceTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _statusBanner = MutableStateFlow("ROBBY AI Ready // Say a command in Urdu or English")
    val statusBanner: StateFlow<String> = _statusBanner.asStateFlow()

    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    private val _aiProcessingStage = MutableStateFlow(AiProcessingStage.IDLE)
    val aiProcessingStage: StateFlow<AiProcessingStage> = _aiProcessingStage.asStateFlow()

    private val _isHudOverlayVisible = MutableStateFlow(false)
    val isHudOverlayVisible: StateFlow<Boolean> = _isHudOverlayVisible.asStateFlow()

    private val _currentDirective = MutableStateFlow("")
    val currentDirective: StateFlow<String> = _currentDirective.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = repository.observeChatMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val quickNotes: StateFlow<List<QuickNote>> = repository.observeQuickNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        // Observe profile from Firestore
        viewModelScope.launch {
            try {
                repository.observeUserProfile().collect { profile ->
                    if (profile != null) {
                        _userProfile.value = profile
                    } else {
                        // Initialize default profile in Firestore
                        val initial = UserProfile(
                            userId = repository.requireUserId(),
                            name = Firebase.auth.currentUser?.displayName ?: "Commander",
                            language = "en",
                            voiceEnabled = true,
                            hudTheme = "cyan_arc"
                        )
                        repository.saveUserProfile(initial)
                        _userProfile.value = initial
                    }
                }
            } catch (e: Exception) {
                // Keep local defaults
            }
        }

        // Voice speech results listener
        voiceManager.onSpeechResult = { recognizedText ->
            executeUserCommand(recognizedText)
        }

        voiceManager.onError = { error ->
            _statusBanner.value = "Voice Warning: $error"
        }

        refreshTelemetry()
    }

    fun refreshTelemetry() {
        _telemetry.value = actionExecutor.getDeviceTelemetry()
    }

    fun toggleVoiceListening() {
        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
            if (_aiProcessingStage.value == AiProcessingStage.LISTENING) {
                _aiProcessingStage.value = AiProcessingStage.IDLE
            }
        } else {
            actionExecutor.vibrateShort()
            val lang = _userProfile.value.language
            _currentDirective.value = if (lang == "ur") "صوتی ہدایت کا انتظار..." else "Acoustic Interface Listening..."
            _aiProcessingStage.value = AiProcessingStage.LISTENING
            _isHudOverlayVisible.value = true
            voiceManager.startListening(if (lang == "ur") "ur-PK" else "en-US")
            _statusBanner.value = if (lang == "ur") "سُن رہا ہوں... براہ کرم بولیے" else "Listening... At your command, Commander."
        }
    }

    fun executeUserCommand(query: String) {
        if (query.isBlank()) return
        val currentLang = if (query.any { it in '\u0600'..'\u06FF' }) "ur" else _userProfile.value.language

        currentExecutionJob?.cancel()
        currentExecutionJob = viewModelScope.launch {
            _isProcessingAi.value = true
            _isHudOverlayVisible.value = true
            _currentDirective.value = query

            // ====================================================================
            // FAST-PATH 1: Direct Voice Command for PC / Computer Control
            // ====================================================================
            val pcDirective = pcController.parseVoicePcDirective(query)
            if (pcDirective != null) {
                _aiProcessingStage.value = AiProcessingStage.EXECUTING
                val feedback = if (currentLang == "ur") pcDirective.feedbackMessageUr else pcDirective.feedbackMessageEn
                _statusBanner.value = feedback
                if (_userProfile.value.voiceEnabled) {
                    _aiProcessingStage.value = AiProcessingStage.SPEAKING
                    voiceManager.speak(feedback)
                }

                val pcRes = pcController.sendCommand(pcDirective.action, pcDirective.target, pcDirective.extra)
                val resSummary = when (pcRes) {
                    is PcResult.Success -> pcRes.message
                    is PcResult.Failure -> pcRes.error
                }

                try {
                    repository.addChatMessage(role = "user", content = query, language = currentLang)
                    repository.addChatMessage(
                        role = "assistant",
                        content = "$feedback\n[$resSummary]",
                        language = currentLang,
                        actionExecuted = resSummary
                    )
                } catch (e: Exception) {
                    // continue
                }

                _statusBanner.value = resSummary
                _isProcessingAi.value = false
                _aiProcessingStage.value = AiProcessingStage.IDLE
                delay(2200L)
                if (_aiProcessingStage.value == AiProcessingStage.IDLE && !voiceManager.isSpeaking.value) {
                    _isHudOverlayVisible.value = false
                }
                return@launch
            }

            // ====================================================================
            // FAST-PATH 2: Direct App Launch (Instant Native Execution)
            // ====================================================================
            val directApp = extractDirectAppCommand(query)
            if (directApp != null) {
                _aiProcessingStage.value = AiProcessingStage.EXECUTING
                val confirmMsg = if (currentLang == "ur") "$directApp کھولی جا رہی ہے..." else "Opening $directApp..."
                _statusBanner.value = confirmMsg
                if (_userProfile.value.voiceEnabled) {
                    _aiProcessingStage.value = AiProcessingStage.SPEAKING
                    voiceManager.speak(confirmMsg)
                }

                val execResult = actionExecutor.openApp(directApp)
                val summary = when (execResult) {
                    is ExecutionResult.Success -> execResult.message
                    is ExecutionResult.Failure -> execResult.error
                }

                try {
                    repository.addChatMessage(role = "user", content = query, language = currentLang)
                    repository.addChatMessage(
                        role = "assistant",
                        content = "$confirmMsg\n[$summary]",
                        language = currentLang,
                        actionExecuted = summary
                    )
                } catch (e: Exception) {
                    // continue
                }

                _statusBanner.value = summary
                _isProcessingAi.value = false
                _aiProcessingStage.value = AiProcessingStage.IDLE
                delay(2200L)
                if (_aiProcessingStage.value == AiProcessingStage.IDLE && !voiceManager.isSpeaking.value) {
                    _isHudOverlayVisible.value = false
                }
                return@launch
            }

            // ====================================================================
            // FAST-PATH 3: Instant Flashlight & Hardware Toggles
            // ====================================================================
            val lowerQ = query.lowercase().trim()
            if (lowerQ.contains("torch") || lowerQ.contains("flashlight") || lowerQ.contains("ٹارچ") || lowerQ.contains("فلیش")) {
                val turnOn = !lowerQ.contains("off") && !lowerQ.contains("بند")
                _aiProcessingStage.value = AiProcessingStage.EXECUTING
                val res = actionExecutor.toggleFlashlight(turnOn)
                val msg = when (res) {
                    is ExecutionResult.Success -> res.message
                    is ExecutionResult.Failure -> res.error
                }
                if (_userProfile.value.voiceEnabled) {
                    _aiProcessingStage.value = AiProcessingStage.SPEAKING
                    voiceManager.speak(msg)
                }
                try {
                    repository.addChatMessage(role = "user", content = query, language = currentLang)
                    repository.addChatMessage(role = "assistant", content = msg, language = currentLang, actionExecuted = msg)
                } catch (e: Exception) {
                    // continue
                }
                _statusBanner.value = msg
                _isProcessingAi.value = false
                _aiProcessingStage.value = AiProcessingStage.IDLE
                return@launch
            }

            // ====================================================================
            // Standard Contextual AI Reasoning via Gemini
            // ====================================================================
            _aiProcessingStage.value = AiProcessingStage.ANALYZING
            _statusBanner.value = if (currentLang == "ur") "نیورل تجزیہ کیا جا رہا ہے..." else "Analyzing neural directives..."

            // 1. Save user query to Firestore
            try {
                repository.addChatMessage(
                    role = "user",
                    content = query,
                    language = currentLang
                )
            } catch (e: Exception) {
                // Log and continue
            }

            // 2. Query Gemini 3.8 Flash via RobbyAiService
            val contextHistory = chatMessages.value.takeLast(4).joinToString("\n") {
                "${it.role}: ${it.content}"
            }
            val result = aiService.processCommand(query, contextHistory)

            // 3. Execute identified device action if any
            var actionSummary = ""
            result.action?.let { act ->
                _aiProcessingStage.value = AiProcessingStage.EXECUTING
                _statusBanner.value = if (currentLang == "ur") "حکم پر عمل ہو رہا ہے..." else "Dispatching system directive..."
                actionSummary = handleDeviceAction(act)
            }

            // 4. Save AI Response to Firestore
            val finalDisplay = if (actionSummary.isNotBlank()) {
                "${result.displayText}\n[$actionSummary]"
            } else {
                result.displayText
            }

            try {
                repository.addChatMessage(
                    role = "assistant",
                    content = finalDisplay,
                    language = currentLang,
                    actionExecuted = actionSummary
                )
            } catch (e: Exception) {
                // continue
            }

            // 5. Speak out loud if voice is enabled
            if (_userProfile.value.voiceEnabled) {
                _aiProcessingStage.value = AiProcessingStage.SPEAKING
                voiceManager.speak(result.spokenText)
            }

            _statusBanner.value = result.displayText
            _isProcessingAi.value = false
            refreshTelemetry()

            // Transition to IDLE and gracefully hide HUD after brief telemetry dwell
            _aiProcessingStage.value = AiProcessingStage.IDLE
            delay(2200L)
            if (_aiProcessingStage.value == AiProcessingStage.IDLE && !voiceManager.isSpeaking.value) {
                _isHudOverlayVisible.value = false
            }
        }
    }

    private fun extractDirectAppCommand(query: String): String? {
        val q = query.lowercase().trim()
        val openPrefixRegex = Regex("""^(open|launch|start|run|chalao|kholo|کھولو|چلائیں)\s+(.+)""")
        val kholoSuffixRegex = Regex("""^(.+)\s+(kholo|chalao|open karo|khol do|on karo|کھولو|چلائیں)$""")

        val prefixMatch = openPrefixRegex.find(q)
        if (prefixMatch != null) {
            val app = prefixMatch.groupValues[2].trim()
            if (app.isNotBlank()) return app
        }

        val suffixMatch = kholoSuffixRegex.find(q)
        if (suffixMatch != null) {
            val app = suffixMatch.groupValues[1].trim()
            if (app.isNotBlank()) return app
        }

        // Direct app names without prefix/suffix
        return when {
            q.contains("youtube") || q.contains("یوٹیوب") -> "YouTube"
            q.contains("chrome") || q.contains("کروم") -> "Chrome"
            q.contains("camera") || q.contains("کیمرہ") -> "Camera"
            q.contains("whatsapp") || q.contains("واٹس ایپ") -> "WhatsApp"
            q.contains("settings") || q.contains("سیٹنگ") -> "Settings"
            q.contains("calculator") || q.contains("کیلکولیٹر") -> "Calculator"
            q.contains("clock") || q.contains("alarm") || q.contains("گھڑی") -> "Clock"
            q.contains("dialer") || q.contains("phone") || q.contains("فون") || q.contains("کال") -> "Phone"
            q.contains("maps") || q.contains("نقشہ") -> "Maps"
            else -> null
        }
    }

    private suspend fun handleDeviceAction(action: DeviceAction): String {
        return when (action) {
            is DeviceAction.OpenApp -> {
                when (val res = actionExecutor.openApp(action.appName, action.packageName)) {
                    is ExecutionResult.Success -> res.message
                    is ExecutionResult.Failure -> res.error
                }
            }
            is DeviceAction.PcCommand -> {
                when (val res = pcController.sendCommand(action.action, action.target, action.extra)) {
                    is PcResult.Success -> res.message
                    is PcResult.Failure -> res.error
                }
            }
            is DeviceAction.ToggleFlashlight -> {
                when (val res = actionExecutor.toggleFlashlight(action.turnOn)) {
                    is ExecutionResult.Success -> res.message
                    is ExecutionResult.Failure -> res.error
                }
            }
            is DeviceAction.SetTimer -> {
                when (val res = actionExecutor.setTimer(action.seconds, action.message)) {
                    is ExecutionResult.Success -> res.message
                    is ExecutionResult.Failure -> res.error
                }
            }
            is DeviceAction.SearchWeb -> {
                when (val res = actionExecutor.searchWeb(action.query)) {
                    is ExecutionResult.Success -> res.message
                    is ExecutionResult.Failure -> res.error
                }
            }
            is DeviceAction.TakeNote -> {
                try {
                    repository.addQuickNote(title = action.title, body = action.content)
                    "Note saved to cloud storage: ${action.title}"
                } catch (e: Exception) {
                    "Failed to save note: ${e.message}"
                }
            }
            is DeviceAction.CheckBattery -> {
                val t = actionExecutor.getDeviceTelemetry()
                "Battery: ${t.batteryLevel}% (${if (t.isCharging) "Charging" else "Discharging"})"
            }
            is DeviceAction.OpenDialer -> {
                when (val res = actionExecutor.openDialer()) {
                    is ExecutionResult.Success -> res.message
                    is ExecutionResult.Failure -> res.error
                }
            }
            is DeviceAction.DownloadPc -> {
                when (val res = actionExecutor.downloadPcCompanion(action.platform, action.url)) {
                    is ExecutionResult.Success -> res.message
                    is ExecutionResult.Failure -> res.error
                }
            }
            is DeviceAction.OpenWebUrl -> {
                when (val res = actionExecutor.openUrl(action.url)) {
                    is ExecutionResult.Success -> res.message
                    is ExecutionResult.Failure -> res.error
                }
            }
            is DeviceAction.GeneralResponse -> {
                action.text
            }
        }
    }

    fun toggleLanguage() {
        val newLang = if (_userProfile.value.language == "ur") "en" else "ur"
        updateProfile(_userProfile.value.copy(language = newLang))
    }

    fun updateHudTheme(themeId: String) {
        updateProfile(_userProfile.value.copy(hudTheme = themeId))
    }

    fun toggleVoiceEnabled(enabled: Boolean) {
        updateProfile(_userProfile.value.copy(voiceEnabled = enabled))
        if (!enabled) {
            voiceManager.stopSpeaking()
        }
    }

    private fun updateProfile(profile: UserProfile) {
        _userProfile.value = profile
        viewModelScope.launch {
            try {
                repository.saveUserProfile(profile)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun createNote(title: String, body: String, category: String) {
        viewModelScope.launch {
            try {
                repository.addQuickNote(title, body, category)
                _statusBanner.value = "Note \"$title\" saved successfully."
            } catch (e: Exception) {
                _statusBanner.value = "Failed to create note: ${e.message}"
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                repository.deleteQuickNote(noteId)
                _statusBanner.value = "Note removed."
            } catch (e: Exception) {
                _statusBanner.value = "Failed to delete note: ${e.message}"
            }
        }
    }

    fun showTacticalHud() {
        _isHudOverlayVisible.value = true
        if (_currentDirective.value.isBlank()) {
            _currentDirective.value = if (_userProfile.value.language == "ur") "ٹیکٹیکل ایچ یو ڈی سسٹمز آن لائن" else "Tactical HUD Systems Online"
        }
    }

    fun dismissHudOverlay() {
        _isHudOverlayVisible.value = false
    }

    fun abortAiProcessing() {
        currentExecutionJob?.cancel()
        currentExecutionJob = null
        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
        }
        if (voiceManager.isSpeaking.value) {
            voiceManager.stopSpeaking()
        }
        _isProcessingAi.value = false
        _aiProcessingStage.value = AiProcessingStage.IDLE
        _statusBanner.value = if (_userProfile.value.language == "ur") "حکم منسوخ کر دیا گیا" else "Directive Aborted."
        _isHudOverlayVisible.value = false
    }

    fun toggleHaptics(enabled: Boolean) {
        updateProfile(_userProfile.value.copy(hapticsEnabled = enabled))
    }

    fun simulateHudPulseDemo() {
        currentExecutionJob?.cancel()
        currentExecutionJob = viewModelScope.launch {
            _isHudOverlayVisible.value = true
            _currentDirective.value = if (_userProfile.value.language == "ur") "ٹیکٹیکل نیورل ڈائیگنوسٹک" else "Tactical Neural Diagnostics"
            _aiProcessingStage.value = AiProcessingStage.ANALYZING
            _statusBanner.value = "Calibrating quantum synapses..."
            delay(1500L)
            _aiProcessingStage.value = AiProcessingStage.EXECUTING
            _statusBanner.value = "Dispatching telemetry bus..."
            delay(1400L)
            _aiProcessingStage.value = AiProcessingStage.SPEAKING
            _statusBanner.value = "Neural telemetry synchronized."
            delay(1500L)
            _aiProcessingStage.value = AiProcessingStage.IDLE
            _statusBanner.value = "Diagnostic cycle verified."
            delay(1800L)
            _isHudOverlayVisible.value = false
        }
    }

    fun speakText(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
