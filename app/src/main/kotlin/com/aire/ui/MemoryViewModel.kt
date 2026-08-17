package com.aire.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aire.claude.*
import com.aire.data.*
import com.aire.domain.MemoryRecord
import com.aire.domain.SourceType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen { HOME, LENS, SETTINGS }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val image: Bitmap? = null,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val response: AssistantResponse? = null,
)

/** Transient UI state for Assistant interactions. */
data class MemoryUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val isThinking: Boolean = false,
    val isListening: Boolean = false,
    val partialTranscription: String = "",
    val isAiAvailable: Boolean = true,
    val capturedImage: Bitmap? = null,
    val chatHistory: List<ChatMessage> = emptyList(),
    val currentLocation: DeviceLocation? = null,
    val error: String? = null,
    val aiModel: String = "claude-3-5-haiku-latest",
    val appearance: String = "System"
)

/**
 * Unified ViewModel for the AI Assistant. Handles chat history, multimodal input,
 * and context-aware interactions via [AssistantService].
 */
class MemoryViewModel(
    private val dao: MemoryDao,
    private val settings: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val integrationManager: IntegrationManager
) : ViewModel() {

    private val _records = dao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val records: StateFlow<List<MemoryRecord>> = _records

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settings.anthropicApiKey, settings.aiModel, settings.appearance) { key, model, appearance ->
                Triple(key, model, appearance)
            }.collect { (key, model, appearance) ->
                _uiState.update { it.copy(
                    isAiAvailable = !key.isNullOrBlank(),
                    aiModel = model,
                    appearance = appearance
                ) }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    /** Called when the camera shutter is pressed. */
    fun onImageCaptured(bitmap: Bitmap) {
        _uiState.update { it.copy(capturedImage = bitmap, currentScreen = AppScreen.HOME) }
    }

    fun clearCapturedImage() {
        _uiState.update { it.copy(capturedImage = null) }
    }

    /** Refresh current location context. Call this when starting a conversation or capture. */
    fun refreshLocation() {
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            _uiState.update { it.copy(currentLocation = location) }
        }
    }

    fun onActionClicked(action: AssistantAction, response: AssistantResponse) {
        when (action.type) {
            "SAVE_MEMORY" -> {
                saveMemoryFromResponse(response)
            }
            else -> {
                // Delegate to integration manager for system-level actions (MAPS_SEARCH, etc.)
                integrationManager.execute(action)
            }
        }
    }

    private fun saveMemoryFromResponse(response: AssistantResponse) {
        val fields = response.extractedFields ?: return
        val loc = uiState.value.currentLocation
        val record = MemoryRecord(
            id = UUID.randomUUID().toString(),
            category = fields.category,
            title = fields.title,
            summary = fields.summary,
            occurredOn = fields.occurredOn,
            attributes = fields.attributes,
            tags = fields.tags,
            capturedAt = System.currentTimeMillis(),
            sourceText = response.explanation,
            sourceType = SourceType.TEXT,
            locationName = loc?.name,
            latitude = loc?.latitude,
            longitude = loc?.longitude
        )
        viewModelScope.launch {
            dao.insert(MemoryRecordEntity.fromDomain(record))
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() && (uiState.value.capturedImage == null)) return
        
        val userImage = uiState.value.capturedImage
        val userMessage = ChatMessage(text = text, image = userImage, isUser = true)
        
        _uiState.update { it.copy(
            chatHistory = it.chatHistory + userMessage,
            isThinking = true,
            error = null,
            capturedImage = null
        ) }

        viewModelScope.launch {
            try {
                val apiKey = settings.anthropicApiKey.first()
                if (!apiKey.isNullOrBlank()) {
                    val config = ClaudeConfig(
                        useProxy = false, // Direct mode for user-provided key
                        proxyBaseUrl = "",
                        directApiKey = apiKey,
                        proxyAuthToken = "",
                        model = uiState.value.aiModel
                    )
                    val client = AnthropicClientProvider.get(config)
                    val assistant = AssistantService(client, config.model)
                    
                    val context = buildAssistantContext()
                    val response = assistant.interact(text, userImage, context)
                    val assistantMessage = ChatMessage(text = response.explanation, isUser = false, response = response)
                    _uiState.update { it.copy(chatHistory = it.chatHistory + assistantMessage, isThinking = false) }
                } else {
                    val assistantMessage = ChatMessage(text = "Please add your Anthropic API key in Settings to enable AI.", isUser = false)
                    _uiState.update { it.copy(chatHistory = it.chatHistory + assistantMessage, isThinking = false) }
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isThinking = false, error = t.friendlyMessage()) }
            }
        }
    }

    private fun buildAssistantContext(): String {
        val memories = records.value.asSequence().take(10).joinToString("\n") { it.toRecallSummary() }
        val locationText = uiState.value.currentLocation?.let { 
            "User's Current Location: ${it.name ?: "Unknown area"} (${it.latitude}, ${it.longitude})" 
        } ?: "User's Location: Unknown (Permission not granted or GPS unavailable)"

        return """
            Recent Memories:
            $memories
            
            $locationText
            Current Time: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}
        """.trimIndent()
    }

    private var voiceRecognizer: VoiceRecognizer? = null

    fun startListening(context: android.content.Context) {
        _uiState.update { it.copy(isListening = true, partialTranscription = "", error = null) }
        voiceRecognizer = VoiceRecognizer(
            context = context,
            onPartialResults = { partial ->
                _uiState.update { it.copy(partialTranscription = partial) }
            },
            onFinalResults = { final ->
                _uiState.update { it.copy(isListening = false, partialTranscription = "") }
                sendMessage(final)
            },
            onError = { err ->
                _uiState.update { it.copy(isListening = false, error = err) }
            }
        ) {
            _uiState.update { it.copy(isListening = false) }
        }
        voiceRecognizer?.start()
    }

    fun stopListening() {
        voiceRecognizer?.stop()
        voiceRecognizer = null
        _uiState.update { it.copy(isListening = false) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    
    // Settings Actions
    fun updateApiKey(key: String) = viewModelScope.launch { settings.setApiKey(key) }
    fun updateGoogleApiKey(key: String) = viewModelScope.launch { settings.setGoogleApiKey(key) }
    fun updateModel(model: String) = viewModelScope.launch { settings.setModel(model) }
    fun updateAppearance(appearance: String) = viewModelScope.launch { settings.setAppearance(appearance) }

    private fun Throwable.friendlyMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "Something went wrong (${this::class.simpleName})."

    class Factory(
        private val dao: MemoryDao,
        private val settings: SettingsRepository,
        private val locationProvider: LocationProvider,
        private val integrationManager: IntegrationManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MemoryViewModel(
                dao = dao,
                settings = settings,
                locationProvider = locationProvider,
                integrationManager = integrationManager
            ) as T
        }
    }
}
