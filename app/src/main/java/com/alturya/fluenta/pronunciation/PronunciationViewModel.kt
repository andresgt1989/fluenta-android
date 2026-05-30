package com.alturya.fluenta.pronunciation

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alturya.fluenta.audio.TtsPlayer
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.PhonemeDrill
import com.alturya.fluenta.network.PronunciationAssessResponse
import com.alturya.fluenta.network.WordResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class AssessResult(
    val score: Int,
    val transcript: String,
    val feedback: String,
    val wordResults: List<WordResult>,
    val xpEarned: Int,
)

data class PronState(
    val loading: Boolean = true,
    val drill: PhonemeDrill? = null,
    val source: String? = null,
    val playing: String? = null,
    val recording: String? = null,   // phrase being recorded
    val assessing: String? = null,   // phrase being assessed
    val assessResults: Map<String, AssessResult> = emptyMap(),
    val error: String? = null
)

class PronunciationViewModel : ViewModel() {

    private val _state = MutableStateFlow(PronState())
    val state = _state.asStateFlow()
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null

    init { loadDrill(null) }

    fun loadDrill(phoneme: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val res = ApiClient.api.getDrill(phoneme)
                _state.value = PronState(loading = false, drill = res.drill, source = res.source)
            } catch (e: Exception) {
                _state.value = PronState(loading = false, error = "No se pudo cargar el ejercicio")
            }
        }
    }

    fun listen(context: Context, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(playing = text)
            val res = TtsPlayer.play(context, text)
            _state.value = if (res.isSuccess) _state.value.copy(playing = null)
            else _state.value.copy(playing = null, error = "No se pudo reproducir el audio")
        }
    }

    fun startRecording(context: Context, phrase: String) {
        stopRecording()
        val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.mp4")
        recordingFile = file
        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        _state.value = _state.value.copy(recording = phrase, error = null)
    }

    fun stopAndAssess(l2: String) {
        val phrase = _state.value.recording ?: return
        val file = recordingFile ?: return
        stopRecording()
        _state.value = _state.value.copy(assessing = phrase)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val audioPart = MultipartBody.Part.createFormData(
                        "audio", file.name,
                        file.asRequestBody("audio/mp4".toMediaType())
                    )
                    val textPart = phrase.toRequestBody("text/plain".toMediaType())
                    val l2Part = l2.toRequestBody("text/plain".toMediaType())
                    ApiClient.api.assessPronunciation(audioPart, textPart, l2Part)
                }
                val newResults = _state.value.assessResults.toMutableMap()
                newResults[phrase] = AssessResult(
                    score = result.score,
                    transcript = result.transcript,
                    feedback = result.feedback,
                    wordResults = result.wordResults,
                    xpEarned = result.xpEarned,
                )
                _state.value = _state.value.copy(assessing = null, assessResults = newResults)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    assessing = null,
                    error = "Error al evaluar: ${e.message?.take(60)}",
                )
            } finally {
                file.delete()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.let { runCatching { it.stop(); it.release() } }
        mediaRecorder = null
        if (_state.value.recording != null) {
            _state.value = _state.value.copy(recording = null)
        }
    }

    override fun onCleared() {
        TtsPlayer.stop()
        stopRecording()
    }
}
