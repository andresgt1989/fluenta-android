package com.alturya.fluenta.conversation

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.audio.TtsPlayer
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.ConvoEndBody
import com.alturya.fluenta.network.ConvoStartBody
import com.alturya.fluenta.network.ConvoTurnBody
import com.alturya.fluenta.network.GuestConvoStartBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConvoMessage(val fromPartner: Boolean, val text: String, val translit: String? = null, val correction: String? = null, val tip: String? = null, val scriptTip: String? = null)

enum class ConvoPhase { CONNECTING, SPEAKING, YOUR_TURN, LISTENING, THINKING, ENDED, ERROR }

data class ConvoUiState(
    val phase: ConvoPhase = ConvoPhase.CONNECTING,
    val messages: List<ConvoMessage> = emptyList(),
    val partial: String = "",
    val error: String? = null,
    val spokenPhrases: Int = 0,
    val xpEarned: Int = 0,
    val suggestion: String = "",   // frase sugerida para que el principiante no se congele
    val suggestionTranslit: String = "",
)

// The voice-conversation wedge. Uses the device SpeechRecognizer (free, on-device)
// for STT → /conversation/turn → plays the partner's reply via TtsPlayer (ElevenLabs).
class ConversationViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(ConvoUiState())
    val state = _state.asStateFlow()

    private var sessionId: String? = null
    private var recognizer: SpeechRecognizer? = null
    // STT language = the language being learned (L2). es→en MVP.
    private val sttLang = "en-US"

    // Guest mode: el wedge debe ser jugable SIN cuenta (activación en <60s).
    private var guest = false
    private var gl1 = "es"
    private var gl2 = "en"
    private var lastScenario: String? = null
    private var lastGoal: String? = null
    private var lastLessonId: String? = null
    // Activación: el primer turno hablado del usuario. Se registra una sola vez.
    private var firstTurnTracked = false

    fun start(scenario: String?, goal: String?, lessonId: String?, guest: Boolean = false, l1: String = "es", l2: String = "en") {
        this.guest = guest; this.gl1 = l1; this.gl2 = l2
        this.lastScenario = scenario; this.lastGoal = goal; this.lastLessonId = lessonId
        viewModelScope.launch {
            _state.value = ConvoUiState(phase = ConvoPhase.CONNECTING)
            try {
                val res = if (guest)
                    ApiClient.apiNoAuth.guestConvoStart(GuestConvoStartBody(l1, l2, scenario, goal))
                else
                    ApiClient.api.convoStart(ConvoStartBody(lessonId, scenario, goal))
                sessionId = res.sessionId
                Analytics.track(getApplication(), Analytics.WEDGE_START, mapOf("l2" to gl2, "guest" to guest.toString()))
                _state.value = _state.value.copy(
                    phase = ConvoPhase.SPEAKING,
                    messages = listOf(ConvoMessage(true, res.reply, translit = res.replyTranslit)),
                    suggestion = res.suggestion ?: "",
                    suggestionTranslit = res.suggestionTranslit ?: "",
                )
                TtsPlayer.play(getApplication(), res.reply)
                _state.value = _state.value.copy(phase = ConvoPhase.YOUR_TURN)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    phase = ConvoPhase.ERROR,
                    error = "No se pudo iniciar la conversación. Revisa tu conexión.",
                )
            }
        }
    }

    fun startListening() {
        val ctx = getApplication<Application>()
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            _state.value = _state.value.copy(error = "El reconocimiento de voz no está disponible en este dispositivo.")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = _state.value.copy(phase = ConvoPhase.LISTENING, partial = "")
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {
                    val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!t.isNullOrBlank()) _state.value = _state.value.copy(partial = t)
                }
                override fun onResults(results: Bundle?) {
                    val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    _state.value = _state.value.copy(partial = "")
                    if (t.isBlank()) _state.value = _state.value.copy(phase = ConvoPhase.YOUR_TURN)
                    else sendTurn(t)
                }
                override fun onError(error: Int) {
                    _state.value = _state.value.copy(phase = ConvoPhase.YOUR_TURN, partial = "")
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sttLang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    private fun sendTurn(text: String) {
        val sid = sessionId ?: return
        if (!firstTurnTracked) {
            firstTurnTracked = true
            // Activación: el usuario habló de verdad y obtuvo respuesta del tutor.
            Analytics.track(getApplication(), Analytics.WEDGE_FIRST_TURN, mapOf("l2" to gl2, "guest" to guest.toString()))
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(
                phase = ConvoPhase.THINKING,
                messages = _state.value.messages + ConvoMessage(false, text),
            )
            try {
                val res = if (guest) ApiClient.apiNoAuth.guestConvoTurn(ConvoTurnBody(sid, text))
                          else ApiClient.api.convoTurn(ConvoTurnBody(sid, text))
                val msgs = _state.value.messages.toMutableList()
                if (!res.correction.isNullOrBlank()) {
                    val li = msgs.indexOfLast { !it.fromPartner }
                    if (li >= 0) msgs[li] = msgs[li].copy(correction = res.correction, tip = res.tip)
                }
                msgs.add(ConvoMessage(true, res.reply, translit = res.replyTranslit, scriptTip = res.scriptTip))
                _state.value = _state.value.copy(
                    messages = msgs,
                    phase = ConvoPhase.SPEAKING,
                    suggestion = res.suggestion ?: "",
                    suggestionTranslit = res.suggestionTranslit ?: "",
                )
                TtsPlayer.play(getApplication(), res.reply)
                if (res.complete) end() else _state.value = _state.value.copy(phase = ConvoPhase.YOUR_TURN)
            } catch (e: Exception) {
                _state.value = _state.value.copy(phase = ConvoPhase.YOUR_TURN, error = "Error de conexión, intenta de nuevo.")
            }
        }
    }

    fun end() {
        val sid = sessionId ?: return
        sessionId = null
        viewModelScope.launch {
            try {
                val res = if (guest) ApiClient.apiNoAuth.guestConvoEnd(ConvoEndBody(sid))
                          else ApiClient.api.convoEnd(ConvoEndBody(sid))
                _state.value = _state.value.copy(
                    phase = ConvoPhase.ENDED,
                    spokenPhrases = res.spokenPhrases,
                    xpEarned = res.xpEarned,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(phase = ConvoPhase.ENDED)
            }
        }
    }

    fun retry() {
        start(lastScenario, lastGoal, lastLessonId, guest, gl1, gl2)
    }

    override fun onCleared() {
        recognizer?.destroy()
        recognizer = null
    }
}
