package com.yourcompany.worklisten.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.data.local.model.WordGroup
import com.yourcompany.worklisten.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yourcompany.worklisten.data.repository.BackgroundRepository
import com.yourcompany.worklisten.data.repository.SettingsRepository
import com.yourcompany.worklisten.utils.TtsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine // 引入 combine

/**
 * 复习模式视图模型
 */
class ReviewViewModel(
	private val repository: WordRepository,
	private val ttsHelper: TtsHelper,
	private val backgroundRepository: BackgroundRepository,
	private val settingsRepository: SettingsRepository
) : ViewModel() {


    // UI状态
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    // 用于触发分页数据加载的流
    private val _currentLibraryAndGroups = MutableStateFlow<Pair<Long, List<Int>>?>(null)

    // 分页数据流，已缓存
    val pagedWords: Flow<PagingData<Word>> = _currentLibraryAndGroups
        .filterNotNull()
        .flatMapLatest { (libraryId, groupIds) -> repository.getPagedWordsFromGroups(libraryId, groupIds) }
        .cachedIn(viewModelScope)
    // 选中的组ID
    private var selectedGroupIds = listOf<Int>()
    
    init {
        viewModelScope.launch {
            // 初始化默认播放进度
            repository.initDefaultPlaybackProgress()

            // 合并多个Flow来更新UI状态
            combine(
                repository.getPlaybackProgress(),
                settingsRepository.backgroundFileName, // 观察背景文件名
                backgroundRepository.observeBackgroundFiles() // 观察背景文件列表
            ) { progress, bgFileName, bgFiles ->
                val bgImagePath = bgFileName?.let { fileName ->
                    bgFiles.find { it.name == fileName }?.absolutePath
                }

                if (progress == null) {
                    ReviewUiState(isLoading = false, hasNoLibrary = true, hasNoWords = true, backgroundImagePath = bgImagePath)
                } else {
                    val libraryId = progress.activeLibraryId
                    selectedGroupIds = if (progress.selectedGroups.isBlank()) {
                        emptyList()
                    } else {
                        progress.selectedGroups.split(",").mapNotNull { it.toIntOrNull() }
                    }

                    if (libraryId != -1L && selectedGroupIds.isNotEmpty()) {
                        loadWordsAndGroups(libraryId) // 触发单词和组的加载
                        ReviewUiState(
                            isLoading = false,
                            hasNoLibrary = false,
                            hasNoWords = false,
                            backgroundImagePath = bgImagePath
                        )
                    } else {
                        ReviewUiState(
                            isLoading = false,
                            hasNoWords = selectedGroupIds.isEmpty(),
                            hasNoLibrary = libraryId == -1L,
                            backgroundImagePath = bgImagePath
                        )
                    }
                }
            }.collectLatest { state ->
                _uiState.update { it.copy(
                    isLoading = state.isLoading,
                    hasNoWords = state.hasNoWords,
                    hasNoLibrary = state.hasNoLibrary,
                    backgroundImagePath = state.backgroundImagePath,
                    // 保持 displayMode 和 hasScrolledToBottom 不变，因为它们由其他操作更新
                    displayMode = _uiState.value.displayMode,
                    hasScrolledToBottom = _uiState.value.hasScrolledToBottom
                ) }
            }
        }
    }
    
    /**
     * 加载单词和组
     */
    private fun loadWordsAndGroups(libraryId: Long) {
        // 更新参数流，这将触发 pagedWords 的更新
        _currentLibraryAndGroups.value = Pair(libraryId, selectedGroupIds)
        
        viewModelScope.launch {
            repository.getGroupsForLibrary(libraryId).collectLatest { groups ->
                val selectedGroups = groups.filter { it.groupId in selectedGroupIds }
                
                _uiState.update { state ->
                    state.copy(
                        selectedGroups = selectedGroups
                    )
                }
            }
        }
    }
    
    /**
     * 更新显示模式
     */
    fun updateDisplayMode(mode: ReviewDisplayMode) {
        // 修复：隐藏意思时不隐藏单词
        _uiState.update { it.copy(displayMode = mode) }
    }
    
    /**
     * 朗读单词
     */
    fun speakWord(word: Word) {
        viewModelScope.launch {
            ttsHelper.stop() // 停止之前的朗读
            val speed = repository.getPlaybackProgressOnce()?.playbackSpeed ?: 1.0f
            val repeatCount = settingsRepository.repeatCount.first()
            ttsHelper.speakWord(word, speed, word.language)
        }
    }
	
	/**
 * 标记选中的组为已复习
 */
fun markGroupsAsReviewed() {
    viewModelScope.launch {
        val groupIds = uiState.value.selectedGroups.map { it.id }
        groupIds.forEach { groupId ->
            repository.updateGroupReviewed(groupId, true)
        }
    }
}
	
	/**
     * 设置滚动到底部状态
     */
    fun setScrolledToBottom(scrolled: Boolean) {
        if (scrolled && !uiState.value.hasScrolledToBottom) {
            _uiState.update { it.copy(hasScrolledToBottom = true) }
            markGroupsAsReviewed()
        }
    }
	
	/**
	 * 在ViewModel销毁时调用，确保TTS停止播放
	 */
	override fun onCleared() {
		ttsHelper.stop() // 确保在ViewModel销毁时停止TTS
		super.onCleared()
	}
	
}

/**
 * 复习模式UI状态
 */
data class ReviewUiState(
    val isLoading: Boolean = true,
    val hasNoWords: Boolean = false,
    val hasNoLibrary: Boolean = false,
    val words: List<Word> = emptyList(),
    val selectedGroups: List<WordGroup> = emptyList(),
    val displayMode: ReviewDisplayMode = ReviewDisplayMode.SHOW_ALL,
    val hasScrolledToBottom: Boolean = false,
    val backgroundImagePath: String? = null // 添加背景图片路径
)

/**
 * 显示模式
 */
enum class ReviewDisplayMode {
    SHOW_ALL,    // 显示所有
    HIDE_WORD,   // 隐藏单词
    HIDE_MEANING // 隐藏意思
}
