// 文件: TtsHelper.kt

package com.yourcompany.worklisten.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yourcompany.worklisten.data.local.model.Word
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import android.os.Handler
import android.os.Looper
import com.yourcompany.worklisten.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine // 引入这个
import kotlin.coroutines.resume

// 定义一个结果密封类，用于 TTS 朗读完成或取消
sealed class SpeakResult {
	object Success : SpeakResult()
	object Cancelled : SpeakResult()
	data class Error(val message: String) : SpeakResult()
}

private enum class RequestType {
	SPEAK,
	PAUSE
}

// 修改 SpeakRequest，移除 onDone，添加 continuation
private data class SpeakRequest(
	val type: RequestType,
	val text: String = "",
	val locale: Locale = Locale.getDefault(),
	val speed: Float = 1.0f,
	val pauseDuration: Long = 0,
	val utteranceId: String,
	val continuation: kotlinx.coroutines.CancellableContinuation<SpeakResult> // 用于 suspend 函数的恢复
)

class TtsHelper(
	context: Context,
	private val settingsRepository: SettingsRepository
) : TextToSpeech.OnInitListener {
	
	private var tts: TextToSpeech
	private val mainHandler = Handler(Looper.getMainLooper())
	private val requestQueue = ConcurrentLinkedQueue<SpeakRequest>()
	private var isProcessingQueue = false
	private val _isTtsReady = MutableLiveData(false)
	val isTtsReady: LiveData<Boolean> = _isTtsReady
	
	init {
		tts = TextToSpeech(context, this)
	}
	
	override fun onInit(status: Int) {
		if (status == TextToSpeech.SUCCESS) {
			// 设置默认语言
			val result = tts.setLanguage(Locale.US) // 默认设置为美式英语
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				// 处理语言缺失或不支持的情况
				_isTtsReady.postValue(false)
			} else {
				_isTtsReady.postValue(true)
			}
			
			tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
				override fun onStart(utteranceId: String?) {
					// Log.d("TtsHelper", "onStart: $utteranceId")
				}
				
				override fun onDone(utteranceId: String?) {
				// Log.d("TtsHelper", "onDone: $utteranceId")
				val completedRequest = requestQueue.peek()
				if (completedRequest != null && completedRequest.utteranceId == utteranceId) {
					requestQueue.poll() // 从队列中移除已完成的请求
					mainHandler.post {
						completedRequest.continuation.resume(SpeakResult.Success) // 恢复协程
						isProcessingQueue = false
						processQueue()
					}
				}
			}
				
				override fun onError(utteranceId: String?) {
				// Log.e("TtsHelper", "onError: $utteranceId")
				val erroredRequest = requestQueue.peek()
				if (erroredRequest != null && erroredRequest.utteranceId == utteranceId) {
					requestQueue.poll()
					mainHandler.post {
						erroredRequest.continuation.resume(SpeakResult.Error("TTS error for $utteranceId")) // 恢复协程并传递错误
						isProcessingQueue = false
						processQueue()
					}
				}
			}
				
				@Deprecated("Deprecated in Java")
				override fun onStop(utteranceId: String?, interrupted: Boolean) {
					super.onStop(utteranceId, interrupted)
					val stoppedRequest = requestQueue.peek()
					if (stoppedRequest != null && stoppedRequest.utteranceId == utteranceId) {
						requestQueue.poll()
						mainHandler.post {
							stoppedRequest.continuation.resume(SpeakResult.Cancelled) // 恢复协程并表示取消
							isProcessingQueue = false
							processQueue()
						}
					}
				}
			})
		} else {
			_isTtsReady.postValue(false)
		}
	}
	
	private fun processQueue() {
		if (isProcessingQueue || requestQueue.isEmpty()) {
			return
		}
		
		val request = requestQueue.peek() // Peek at the next request
		request?.let {
			isProcessingQueue = true
			when (it.type) {
				RequestType.SPEAK -> {
					selectVoiceForLanguage(it.locale)
					tts.language = it.locale
					tts.setSpeechRate(it.speed)
					tts.speak(it.text, TextToSpeech.QUEUE_ADD, null, it.utteranceId)
				}
				RequestType.PAUSE -> {
					tts.playSilentUtterance(it.pauseDuration, TextToSpeech.QUEUE_ADD, it.utteranceId)
				}
			}
		}
	}
	

	suspend fun speakWord(word: Word, speed: Float, language: String): SpeakResult =
		suspendCancellableCoroutine { continuation ->
			// 添加空值检查，防止空指针异常
			if (word.word.isBlank()) {
				continuation.resume(SpeakResult.Error("Word is empty"))
				return@suspendCancellableCoroutine
			}
			
			val locale = if (language == "ja") Locale.JAPANESE else Locale.US
			val utteranceId = UUID.randomUUID().toString()
			val request = SpeakRequest(
				type = RequestType.SPEAK,
				text = word.word,
				locale = locale,
				speed = speed,
				utteranceId = utteranceId,
				continuation = continuation
			)
			requestQueue.offer(request)
			processQueue()
			
			// 当协程被取消时，尝试停止 TTS
			continuation.invokeOnCancellation {
				stop() // 停止所有 TTS
				requestQueue.clear() // 清空队列
			}
		}
	
	// 新增 speakMeaning 方法为 suspend 函数
	suspend fun speakMeaning(word: Word, speed: Float): SpeakResult =
		suspendCancellableCoroutine { continuation ->
			val locale = Locale.CHINESE // 假设含义总是中文
			val utteranceId = UUID.randomUUID().toString()
			val request = SpeakRequest(
				type = RequestType.SPEAK,
				text = word.meaning,
				locale = locale,
				speed = speed,
				utteranceId = utteranceId,
				continuation = continuation
			)
			requestQueue.offer(request)
			processQueue()
			
			continuation.invokeOnCancellation {
				stop()
				requestQueue.clear()
			}
		}
	private fun selectVoiceForLanguage(locale: Locale) {
		// 尝试根据 locale 选择最佳语音
		val voices = tts.voices // 获取所有可用的语音
		val selectedVoice = voices.firstOrNull { voice ->
			// 优先选择与传入 locale 完全匹配的语音
			voice.locale == locale && !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
		} ?: voices.firstOrNull { voice ->
			// 如果没有完全匹配的，尝试匹配语言部分（例如，en-US 和 en-GB 都匹配 'en'）
			voice.locale.language == locale.language && !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
		}
		
		selectedVoice?.let {
			tts.voice = it // 设置找到的最佳语音
			// Log.d("TtsHelper", "Selected voice for ${locale.displayLanguage}: ${it.name}")
		} ?: run {
			// 如果没有找到合适的语音，则回退到默认设置
			tts.language = locale
			// Log.w("TtsHelper", "No suitable voice found for ${locale.displayLanguage}, falling back to default language setting.")
		}
	}
	
	// 修改 playSilence 方法为 suspend 函数，移除 onDone 回调
	suspend fun playSilence(durationInMillis: Long): SpeakResult =
		suspendCancellableCoroutine { continuation ->
			if (durationInMillis <= 0) { // 如果持续时间为0或负数，则立即完成
				continuation.resume(SpeakResult.Success)
				return@suspendCancellableCoroutine
			}
			val utteranceId = UUID.randomUUID().toString()
			val request = SpeakRequest(
				type = RequestType.PAUSE,
				pauseDuration = durationInMillis,
				utteranceId = utteranceId,
				continuation = continuation
			)
			requestQueue.offer(request)
			processQueue()
			
			continuation.invokeOnCancellation {
				stop()
				requestQueue.clear()
			}
		}
	
	fun stop() {
		requestQueue.clear()
		if (tts.isSpeaking) {
			tts.stop()
		}
		isProcessingQueue = false
	}
	
	fun warmUp() {
		// Play a very short silent utterance to warm up the engine
		// 注意：这里也应该使用 suspend 版本的 playSilence，但在 warmUp 中直接调用 suspend 会阻塞，
		// 通常 warmUp 会在 init 块或单独的协程中调用。此处为示例，实际可能需要调整。
		// 确保 tts 是 ready 的。
		@OptIn(DelicateCoroutinesApi::class)
		GlobalScope.launch { // 临时使用 GlobalScope，实际生产代码中应使用 ViewModelScope 或合适的 Scope
			try {
				if (_isTtsReady.value == true) {
					playSilence(1) // Play a very short silent utterance
				}
			} catch (e: Exception) {
				// Handle warming up error
			}
		}
	}
	
	fun isLanguageSupported(language: String): Boolean {
		val locale = when (language) {
			"ja" -> Locale.JAPANESE
			else -> Locale.US
		}
		return tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
	}
	
	fun shutdown() {
		tts.shutdown()
	}
}