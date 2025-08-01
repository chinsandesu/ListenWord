package com.yourcompany.worklisten.ui.viewmodel

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.data.repository.BackgroundRepository
import com.yourcompany.worklisten.data.repository.SettingsRepository
import com.yourcompany.worklisten.data.repository.WordRepository
import com.yourcompany.worklisten.utils.SpeakResult // 引入 SpeakResult
import com.yourcompany.worklisten.utils.TtsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay // 引入 delay

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlayerViewModel(
	private val repository: WordRepository,
	private val settingsRepository: SettingsRepository,
	private val ttsHelper: TtsHelper,
	private val backgroundRepository: BackgroundRepository
) : ViewModel() {
	
	private val _uiState = MutableStateFlow(
		PlayerUiState(ttsError = null)
	)
	val uiState = _uiState.asStateFlow()
	
	private var playbackJob: Job? = null
	
	init {
		viewModelScope.launch {
			repository.initDefaultPlaybackProgress()
		}
		
		viewModelScope.launch {
			combine(
				repository.getPlaybackProgress(),
				settingsRepository.backgroundFileName,
				backgroundRepository.observeBackgroundFiles(),
				settingsRepository.repeatCount // 合并单词重复次数的 Flow
			) { progress, bgFileName, bgFiles, repeatCountFromSettings -> // 接收 repeatCountFromSettings
				// 注意：这里的 progress.wordRepeatCount 是数据库中保存的上次播放的重复次数，
				// settingsRepository.repeatCount 是用户设置的全局重复次数。
				// 实际播放时应该以 settingsRepository.repeatCount 为准。
				Triple(progress, bgFileName, bgFiles to repeatCountFromSettings) // 传递 repeatCountFromSettings
			}.flatMapLatest { (progress, bgFileName, bgFilesAndRepeatCount) ->
				val (bgFiles, repeatCountFromSettings) = bgFilesAndRepeatCount
				flow {
					val bgImagePath = bgFileName?.let { fileName ->
						bgFiles.find { it.name == fileName }?.absolutePath
					}
					
					if (progress == null) {
						emit(
							PlayerUiState(
								backgroundImagePath = bgImagePath,
								ttsError = null,
								wordRepeatCount = repeatCountFromSettings // 初始状态使用全局设置的重复次数
							)
						)
						return@flow
					}
					
					val activeLibrary = try {
						repository.getActiveLibrary()
					} catch (e: Exception) {
						e.printStackTrace()
						null
					}
					
					val words = if (activeLibrary != null && progress.selectedGroups.isNotBlank()) {
						try {
							val selectedGroupIds =
								progress.selectedGroups.split(",").mapNotNull { it.toIntOrNull() }
							repository.getWordsFromGroupsOnce(activeLibrary.id, selectedGroupIds)
						} catch (e: Exception) {
							e.printStackTrace()
							emptyList<Word>()
						}
					} else {
						emptyList()
					}
					
					emit(
						PlayerUiState(
							isLoading = false,
							words = words,
							currentWord = words.getOrNull(progress.currentIndex),
							currentIndex = progress.currentIndex,
							playbackMode = PlaybackMode.fromString(progress.playbackMode),
							isRandom = progress.isRandom,
							isLoop = progress.isLoop,
							playbackSpeed = progress.playbackSpeed,
							playbackInterval = progress.playbackInterval,
							wordRepeatCount = repeatCountFromSettings, // 使用全局设置的重复次数更新UI状态
							activeLibraryName = activeLibrary?.name ?: "未选择词库",
							hasWords = words.isNotEmpty(),
							backgroundImagePath = bgImagePath,
							ttsError = null
						)
					)
				}
			}.collectLatest { state ->
				_uiState.update {
					it.copy(
						isLoading = state.isLoading,
						words = state.words,
						currentWord = state.currentWord,
						currentIndex = state.currentIndex,
						playbackMode = state.playbackMode,
						isRandom = state.isRandom,
						isLoop = state.isLoop,
						playbackSpeed = state.playbackSpeed,
						playbackInterval = state.playbackInterval,
						wordRepeatCount = state.wordRepeatCount, // 确保 UI 状态更新了重复次数
						activeLibraryName = state.activeLibraryName,
						hasWords = state.hasWords,
						backgroundImagePath = state.backgroundImagePath,
						ttsError = state.ttsError
					)
				}
				// 只有当 isPlaying 为 true 且当前单词有效时才尝试启动播放
				if (_uiState.value.isPlaying && _uiState.value.currentWord != null) {
					startPlayback()
				}
			}
		}
	}
	
	private fun startPlayback() {
		stopPlayback() // 先停止任何正在进行的播放任务
		
		// 启动一个新的播放协程
		playbackJob = viewModelScope.launch {
			_uiState.update { it.copy(isPlaying = true) } // 明确设置播放状态为 true
			
			// 主播放循环
			while (true) {
				val currentState = _uiState.value // 获取当前最新的 UI 状态
				val words = currentState.words
				val currentIndex = currentState.currentIndex
				
				if (words.isEmpty() || currentIndex < 0 || currentIndex >= words.size) {
					// 如果没有单词或索引越界，则停止播放
					_uiState.update { it.copy(isPlaying = false) }
					break
				}
				
				val currentWord = words[currentIndex]
				val repeatCount = currentState.wordRepeatCount // 获取当前设置的重复次数
				val playbackMode = currentState.playbackMode
				val playbackSpeed = currentState.playbackSpeed
				val playbackInterval = currentState.playbackInterval
				val activeLibrary = repository.getActiveLibrary()
				val language = activeLibrary?.language ?: "en"
				
				// 设置是否显示中文含义
				val shouldShowMeaning = when (playbackMode) {
					PlaybackMode.CN_ONLY, PlaybackMode.WORD_TO_CN -> true
					else -> false
				}
				_uiState.update { it.copy(showMeaning = shouldShowMeaning) }
				
				// 核心修复：先循环朗读单词 repeatCount 次
				for (i in 0 until repeatCount) {
					if (playbackMode == PlaybackMode.HIDE_ALL || playbackMode == PlaybackMode.WORD_ONLY || playbackMode == PlaybackMode.WORD_TO_CN) {
						ttsHelper.speakWord(currentWord, playbackSpeed, language)
					}
					// 如果不是最后一次单词重复，且不是 HIDE_ALL 模式，则在单词之间添加间隔
					if (i < repeatCount - 1 && playbackMode != PlaybackMode.HIDE_ALL) {
						ttsHelper.playSilence((playbackInterval * 1000).toLong())
					}
				}

				// 朗读完单词重复后，根据模式朗读意思
				when (playbackMode) {
					PlaybackMode.CN_ONLY -> {
						// 中文朗读速度乘以1.5倍
                        ttsHelper.speakMeaning(currentWord, playbackSpeed * 1.5f)
					}
					PlaybackMode.WORD_TO_CN -> {
						// 单词和意思之间的间隔
						ttsHelper.playSilence((playbackInterval * 1000).toLong())
						// 中文朗读速度乘以1.5倍
                        ttsHelper.speakMeaning(currentWord, playbackSpeed * 1.5f)
					}
					else -> { /* HIDE_ALL 和 WORD_ONLY 模式不朗读意思 */ }
				}

				// 朗读完所有重复次数和意思后，切换到下一个单词
				val nextIndex = if (currentState.isRandom) {
					if (words.size <= 1) {
						0
					} else {
						(0 until words.size).filter { it != currentIndex }.randomOrNull() ?: 0
					}
				} else {
					if (currentIndex < words.size - 1) currentIndex + 1 else if (currentState.isLoop) 0 else -1
				}
				
				if (nextIndex == -1) {
					// 播放结束
					_uiState.update { it.copy(isPlaying = false) }
					break // 退出主播放循环
				} else {
					// 更新数据库中的索引
					repository.updateCurrentIndex(nextIndex)
					repository.updateLastPlayedTime() // 更新最后播放时间
					// _uiState 会通过 combine flow 自动更新，无需手动更新 currentWord 和 currentIndex
					// 循环会自动从 Flow 收集到新的状态并继续播放下一个单词
				}
				// 为了防止在极短间隔内连续更新 currentIndex 导致问题，
				// 可以在这里加一个非常短的延迟，确保 UI 和数据层有时间同步，
				// 但通常 combine + collectLatest 已经处理得很好。
				// delay(50) // 可选：短暂延迟，让UI有时间响应
			}
		}
	}
	
	
	fun stopPlayback() {
		playbackJob?.cancel() // 取消播放任务
		ttsHelper.stop() // 停止 TTS 朗读
		_uiState.update { it.copy(isPlaying = false) } // 明确设置播放状态为 false
	}
	
	fun togglePlayPause() {
		// 根据当前播放状态来决定是开始还是停止
		if (_uiState.value.isPlaying) {
			stopPlayback()
		} else {
			// startPlayback 会在内部设置 isPlaying = true
			startPlayback()
		}
	}
	
	fun nextWord() {
		// 在切换单词前停止当前播放
		stopPlayback() // 先停止，避免当前单词还在重复
		val state = _uiState.value
		if (state.words.isEmpty()) return
		
		val newIndex = if (state.isRandom) {
			if (state.words.size <= 1) {
				0
			} else {
				(0 until state.words.size).filter { it != state.currentIndex }.randomOrNull() ?: 0
			}
		} else {
			if (state.currentIndex < state.words.size - 1) state.currentIndex + 1 else if (state.isLoop) 0 else -1
		}
		updateCurrentIndexAndResumePlayback(newIndex)
	}
	
	fun previousWord() {
		// 在切换单词前停止当前播放
		stopPlayback() // 先停止，避免当前单词还在重复
		val state = _uiState.value
		if (state.words.isEmpty()) return
		
		val newIndex = if (state.isRandom) {
			if (state.words.size <= 1) {
				0
			} else {
				(0 until state.words.size).filter { it != state.currentIndex }.randomOrNull() ?: 0
			}
		} else {
			if (state.currentIndex > 0) state.currentIndex - 1 else if (state.isLoop) state.words.size - 1 else state.currentIndex
		}
		updateCurrentIndexAndResumePlayback(newIndex)
	}
	
	// 统一处理更新索引并决定是否恢复播放的逻辑
	private fun updateCurrentIndexAndResumePlayback(index: Int) {
		viewModelScope.launch {
			repository.updateCurrentIndex(index)
			repository.updateLastPlayedTime() // 更新时间戳
			// 当 currentIndex 更新后，_uiState 会自动通过 combine flow 更新
			// 此时，如果 isPlaying 状态为 true，startPlayback 会在 collectLatest 内部被再次调用，继续播放
		}
	}
	
	fun updateBackgroundImage(uri: Uri) {
		viewModelScope.launch {
			val fileName = backgroundRepository.saveBackground(uri)
			fileName?.let {
				settingsRepository.saveBackgroundFileName(it)
				// 立即更新UI状态，触发重新加载
				_uiState.update { it.copy(backgroundImagePath = null) }
				// 等待文件保存完成和文件系统更新
				delay(500)
				// 再次更新UI状态，确保获取最新的文件路径
				backgroundRepository.observeBackgroundFiles().firstOrNull()?.let { files ->
					val bgImagePath = files.find { file -> file.name == it }?.absolutePath
					android.util.Log.d("BackgroundUpdate", "Updating background image path: $bgImagePath")
					_uiState.update {
						it.copy(backgroundImagePath = bgImagePath)
					}
				}
			}
		}
	}
	
	fun removeBackgroundImage() {
		viewModelScope.launch {
			settingsRepository.getBackgroundFileName()?.let {
				backgroundRepository.deleteBackground(it)
			}
			settingsRepository.clearBackgroundSetting()
			
			// 立即更新UI状态，触发默认背景显示
			_uiState.update { it.copy(backgroundImagePath = null) }
			// 等待文件删除完成
			delay(500)
			// 再次更新UI状态，确保清除生效
			android.util.Log.d("BackgroundUpdate", "Removing background image")
			_uiState.update { it.copy(backgroundImagePath = null) }
		}
	}
	
	fun onCardClicked() {
		_uiState.update { it.copy(showMeaning = !it.showMeaning) }
	}
	
	fun setPlaybackMode(mode: PlaybackMode) =
		viewModelScope.launch { repository.updatePlaybackMode(mode.name) }
	
	fun setPlaybackSpeed(speed: Float) =
		viewModelScope.launch { repository.updatePlaybackSpeed(speed) }
	
	fun setPlaybackInterval(interval: Float) =
		viewModelScope.launch { repository.updatePlaybackInterval(interval) }
	
	fun setWordRepeatCount(count: Int) = viewModelScope.launch {
		android.util.Log.d("PlayerViewModel", "Setting word repeat count to: $count")
		settingsRepository.saveRepeatCount(count) // 只保存到 settingsRepository，UI 会自动更新
		// 注意：这里不再需要 repository.updateWordRepeatCount(count)
		// 因为 PlaybackProgress 里的 wordRepeatCount 只是上一次播放时的状态，
		// 真正的用户设置应该从 SettingsRepository 中获取。
		// 播放逻辑会直接读取 settingsRepository 的值。
		_uiState.update { it.copy(wordRepeatCount = count) } // 立即更新 UI 状态
		// 如果正在播放，停止并重新开始，以应用新的重复次数
		if (_uiState.value.isPlaying) {
			startPlayback()
		}
	}
	
	
	fun toggleRandomMode() =
		viewModelScope.launch { repository.updateRandomMode(!_uiState.value.isRandom) }
	
	fun toggleLoopMode() =
		viewModelScope.launch { repository.updateLoopMode(!_uiState.value.isLoop) }
	
	
	fun updateSearchQuery(query: String) {
		viewModelScope.launch {
			_uiState.update { it.copy(searchQuery = query) }
			if (query.isBlank()) {
				_uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
				return@launch
			}
			_uiState.update { it.copy(isSearching = true) }
			try {
				val results = repository.searchAllLibraries(query)
				_uiState.update { it.copy(searchResults = results, isSearching = false) }
			} catch (e: Exception) {
				_uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
			}
		}
	}
	
	fun clearSearchResults() {
		_uiState.update {
			it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false)
		}
	}
	
	fun toggleSearchVisibility() {
		_uiState.update { currentState ->
			currentState.copy(
				isSearchVisible = !currentState.isSearchVisible,
				searchQuery = "",
				searchResults = emptyList(),
				isSearching = false
			)
		}
	}
	
	override fun onCleared() {
		stopPlayback()
		super.onCleared()
	}
	
	class Factory(
		private val repository: WordRepository,
		private val settingsRepository: SettingsRepository,
		private val ttsHelper: TtsHelper,
		private val backgroundRepository: BackgroundRepository
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
				return PlayerViewModel(
					repository,
					settingsRepository,
					ttsHelper,
					backgroundRepository
				) as T
			}
			throw IllegalArgumentException("Unknown ViewModel class")
		}
	}
}

data class PlayerUiState(
	val isLoading: Boolean = true,
	val words: List<Word> = emptyList(),
	val currentWord: Word? = null,
	val currentIndex: Int = 0,
	val isPlaying: Boolean = false,
	val showMeaning: Boolean = false,
	val playbackMode: PlaybackMode = PlaybackMode.WORD_TO_CN,
	val isRandom: Boolean = false,
	val isLoop: Boolean = false,
	val playbackSpeed: Float = 1.0f,
	val playbackInterval: Float = 1.0f,
	val wordRepeatCount: Int = 1,
	val activeLibraryName: String = "未选择词库",
	val hasWords: Boolean = false,
	val backgroundImagePath: String? = null,
	val searchQuery: String = "",
	val searchResults: List<Word> = emptyList(),
	val isSearching: Boolean = false,
	val isSearchVisible: Boolean = false,
	val ttsError: String?
)

enum class PlaybackMode(@StringRes val displayNameResId: Int) {
	HIDE_ALL(R.string.playback_mode_hide_all),
	CN_ONLY(R.string.playback_mode_cn_only),
	WORD_ONLY(R.string.playback_mode_word_only),
	WORD_TO_CN(R.string.playback_mode_word_to_cn);
	
	companion object {
		fun fromString(name: String?): PlaybackMode {
			return entries.find { it.name == name } ?: WORD_TO_CN // 更简洁的写法
		}
	}
	
}
