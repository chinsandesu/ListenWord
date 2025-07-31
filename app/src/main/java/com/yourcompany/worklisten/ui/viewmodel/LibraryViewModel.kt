package com.yourcompany.worklisten.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourcompany.worklisten.data.local.model.WordLibrary
import com.yourcompany.worklisten.data.repository.BackgroundRepository
import com.yourcompany.worklisten.data.repository.WordRepository
import com.yourcompany.worklisten.utils.FileImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 词库管理视图模型
 */
class LibraryViewModel(
    private val repository: WordRepository,
    private val fileImporter: FileImporter,
    private val backgroundRepository: BackgroundRepository
) : ViewModel() {
    
    // UI 状态
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    // 选中的URI
    private var selectedUri: Uri? = null
    
    init {
        loadLibraries()
    }
    
    /**
     * 加载所有词库
     */
    private fun loadLibraries() {
        viewModelScope.launch {
            repository.getAllLibraries().collect { libraries ->
                _uiState.update { it.copy(
                    libraries = libraries,
                    isLoading = false
                ) }
            }
        }
    }
    
    /**
     * 设置选中的URI
     */
    fun setSelectedUri(uri: Uri) {
        selectedUri = uri
    }
    
    /**
     * 获取选中的URI
     */
    fun getSelectedUri(): Uri? {
        return selectedUri
    }
    
    /**
     * 导入词库文件
     */
    fun importLibrary(uri: Uri, name: String, delimiter: Char = ',') {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                importResult = null
            ) }
            
            when (val result = fileImporter.importFile(uri, name, delimiter)) {
                is FileImporter.ImportResult.Success -> {
                    val libraryId = repository.importLibraryFromResult(result)
                    
                    // 如果还没有激活的词库，将这个词库激活
                    if (repository.getActiveLibrary() == null) {
                        repository.activateLibrary(libraryId)
                    }
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        importResult = ImportResult.Success(
                            importedCount = result.importedCount,
                            skippedCount = result.skippedCount
                        )
                    ) }
                }
                is FileImporter.ImportResult.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        importResult = ImportResult.Error(result.message)
                    ) }
                }
            }
        }
    }
    
    /**
     * 激活词库
     */
    fun activateLibrary(library: WordLibrary) {
        viewModelScope.launch {
            repository.activateLibrary(library.id)
        }
    }
    
    /**
     * 删除词库
     */
    fun deleteLibrary(library: WordLibrary) {
        viewModelScope.launch {
            repository.deleteLibrary(library)
        }
    }
    
    /**
     * 清除导入结果
     */
    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }
    
    /**
     * 视图模型工厂
     */
    class Factory(
        private val repository: WordRepository,
        private val fileImporter: FileImporter,
        private val backgroundRepository: BackgroundRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                return LibraryViewModel(repository, fileImporter, backgroundRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * 词库UI状态
 */
data class LibraryUiState(
    val libraries: List<WordLibrary> = emptyList(),
    val isLoading: Boolean = true,
    val importResult: ImportResult? = null
)

/**
 * 导入结果
 */
sealed class ImportResult {
    data class Success(val importedCount: Int, val skippedCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}