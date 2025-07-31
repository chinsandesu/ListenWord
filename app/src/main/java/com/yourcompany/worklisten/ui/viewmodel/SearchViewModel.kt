package com.yourcompany.worklisten.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.data.repository.BackgroundRepository
import com.yourcompany.worklisten.data.repository.SettingsRepository
import com.yourcompany.worklisten.data.repository.WordRepository
import com.yourcompany.worklisten.utils.FileImporter
import com.yourcompany.worklisten.utils.TtsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
	private val repository: WordRepository,
	private val settingsRepository: SettingsRepository,
	private val fileImporter: FileImporter,
	private val ttsHelper: TtsHelper,
	private val backgroundRepository: BackgroundRepository
) : ViewModel() {
	
	private val _uiState = MutableStateFlow(SearchUiState())
	val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
	
	// 更新搜索查询并执行搜索
	fun updateSearchQuery(query: String) {
		viewModelScope.launch {
			_uiState.update { it.copy(
				searchQuery = query,
				isSearching = true
			) }
			
			if (query.isBlank()) {
				_uiState.update { it.copy(
					searchResults = emptyList(),
					isSearching = false,
					showResults = false
				) }
				return@launch
			}
			
			try {
				val results = repository.searchAllLibraries(query)
				// 根据单词和意思去重
				val distinctResults = results.distinctBy { it.word to it.meaning }
				_uiState.update { it.copy(
					searchResults = distinctResults,
					isSearching = false,
					showResults = true
				) }
			} catch (e: Exception) {
				_uiState.update { it.copy(
					searchResults = emptyList(),
					isSearching = false,
					errorMessage = "搜索失败，请重试",
					showResults = true
				) }
			}
		}
	}
	
	// 切换搜索框显示状态
	fun toggleSearchVisibility() {
		_uiState.update { it.copy(
			isSearchVisible = !it.isSearchVisible,
			// 重置搜索状态
			searchQuery = "",
			searchResults = emptyList(),
			showResults = false
		) }
	}
	
	// 发音功能
	fun speakWord(word: Word) {
		viewModelScope.launch {
			if (!ttsHelper.isLanguageSupported(word.language)) {
				_uiState.update { it.copy(
					ttsError = "不支持的语言: ${word.language}"
				) }
				return@launch
			}
			val repeatCount = repository.getPlaybackProgressOnce()?.wordRepeatCount ?: 1
			ttsHelper.speakWord(word, 1.0f, word.language)
		}
	}
	
	// 清除错误信息
	fun clearError() {
		_uiState.update { it.copy(
			errorMessage = null,
			ttsError = null
		) }
	}
	
	class Factory(
	private val repository: WordRepository,
	private val settingsRepository: SettingsRepository,
	private val fileImporter: FileImporter,
	private val ttsHelper: TtsHelper,
	private val backgroundRepository: BackgroundRepository
) : ViewModelProvider.Factory {
	@Suppress("UNCHECKED_CAST")
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
			return SearchViewModel(repository, settingsRepository, fileImporter, ttsHelper, backgroundRepository) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}
}

// 搜索UI状态数据类
data class SearchUiState(
	val searchQuery: String = "",
	val searchResults: List<Word> = emptyList(),
	val isSearching: Boolean = false,
	val isSearchVisible: Boolean = false,
	val showResults: Boolean = false,
	val errorMessage: String? = null,
	val ttsError: String? = null
)
