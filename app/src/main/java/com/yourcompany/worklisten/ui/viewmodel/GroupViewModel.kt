package com.yourcompany.worklisten.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourcompany.worklisten.data.local.model.WordChapter
import com.yourcompany.worklisten.data.local.model.WordGroup
import com.yourcompany.worklisten.data.local.model.WordLibrary
import com.yourcompany.worklisten.data.repository.BackgroundRepository
import com.yourcompany.worklisten.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 组选择视图模型
 */
class GroupViewModel(
    private val repository: WordRepository,
    private val backgroundRepository: BackgroundRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getPlaybackProgress().combine(repository.getAllLibraries()) { progress, libraries ->
                Pair(progress, libraries)
            }.collect { (progress, libraries) ->
                if (progress == null || libraries.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, activeLibrary = null) }
                    return@collect
                }

                val activeLibrary = libraries.find { it.id == progress.activeLibraryId }
                if (activeLibrary == null) {
                    _uiState.update { it.copy(isLoading = false, activeLibrary = null) }
                    return@collect
                }
                
                val chapters = repository.getChaptersForLibraryOnce(activeLibrary.id)
                val groups = repository.getGroupsForLibraryOnce(activeLibrary.id)
                val groupsByChapter = groups.groupBy { it.chapterId }
                
                // 从数据库获取选中的组
                val selectedGroups = repository.getSelectedGroupsForLibrary(activeLibrary.id)
                var initialSelection = selectedGroups.map { it.groupId }.toSet()

                // 如果没有选中的组，默认选择第一组
                if (initialSelection.isEmpty() && groups.isNotEmpty()) {
                    initialSelection = setOf(groups.first().groupId)
                    // 同时更新数据库，将第一组设为选中
                    viewModelScope.launch {
                        repository.updateAllGroupsSelected(activeLibrary.id, false)
                        repository.updateGroupsSelected(listOf(groups.first().id), true)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeLibrary = activeLibrary,
                        chapters = chapters,
                        groupsByChapter = groupsByChapter,
                        selectedGroupIds = initialSelection,
                        initialSelectedGroupIds = initialSelection
                    )
                }
            }
        }
    }

    fun toggleChapterExpansion(chapterId: Long) {
        _uiState.update { currentState ->
            val newExpandedIds = currentState.expandedChapterIds.toMutableSet()
            if (newExpandedIds.contains(chapterId)) {
                newExpandedIds.remove(chapterId)
            } else {
                newExpandedIds.add(chapterId)
            }
            currentState.copy(expandedChapterIds = newExpandedIds)
        }
    }

    fun toggleGroupSelection(groupId: Int) {
        _uiState.update { currentState ->
            val newSelection = currentState.selectedGroupIds.toMutableSet()
            if (newSelection.contains(groupId)) {
                newSelection.remove(groupId)
            } else {
                newSelection.add(groupId)
            }
            currentState.copy(selectedGroupIds = newSelection)
        }
    }
    
    fun toggleSelectAll() {
        _uiState.update { currentState ->
            // 找出所有展开章节中的组ID
            val groupIdsInExpandedChapters = currentState.expandedChapterIds
                .flatMap { chapterId ->
                    currentState.groupsByChapter[chapterId] ?: emptyList()
                }
                .map { it.groupId }
                .toSet()

            if (groupIdsInExpandedChapters.isEmpty()) {
                return@update currentState // 如果没有展开的章节，则不执行任何操作
            }
            
            val currentSelection = currentState.selectedGroupIds.toMutableSet()
            
            // 检查展开的组是否已全部被选中
            val areAllExpandedSelected = currentSelection.containsAll(groupIdsInExpandedChapters)

            val newSelection = if (areAllExpandedSelected) {
                // 如果已全选，则取消选择这些组
                currentSelection.apply { removeAll(groupIdsInExpandedChapters) }
            } else {
                // 否则，添加所有这些组到选择中
                currentSelection.apply { addAll(groupIdsInExpandedChapters) }
            }
            
            currentState.copy(selectedGroupIds = newSelection)
        }
    }

    fun saveSelection() {
        viewModelScope.launch {
            val activeLibrary = _uiState.value.activeLibrary ?: return@launch
            val allGroups = _uiState.value.allGroups
            val selectedGroupIds = _uiState.value.selectedGroupIds

            // 获取选中组的主键id列表
            val selectedGroupPrimaryIds = allGroups
                .filter { it.groupId in selectedGroupIds }
                .map { it.id }

            // 先将该词库所有组设为未选中
            repository.updateAllGroupsSelected(activeLibrary.id, false)
            
            // 如果有选中的组，更新选中状态；否则默认选中第一组
            if (selectedGroupPrimaryIds.isNotEmpty()) {
                repository.updateGroupsSelected(selectedGroupPrimaryIds, true)
            } else if (allGroups.isNotEmpty()) {
                repository.updateGroupsSelected(listOf(allGroups.first().id), true)
            }

            // 更新全局选中组信息
            // 将Set<Int>转换为List<Int>以匹配方法参数要求
            repository.saveSelectedGroups(selectedGroupIds.toList())
            
            // 更新UI状态
            _uiState.update { it.copy(initialSelectedGroupIds = it.selectedGroupIds) }
        }
    }

    fun updateFilter(filter: GroupFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    /**
     * 视图模型工厂
     */
    class Factory(
        private val repository: WordRepository,
        private val backgroundRepository: BackgroundRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
                return GroupViewModel(repository, backgroundRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class GroupUiState(
    val isLoading: Boolean = true,
    val activeLibrary: WordLibrary? = null,
    val chapters: List<WordChapter> = emptyList(),
    val groupsByChapter: Map<Long, List<WordGroup>> = emptyMap(),
    val expandedChapterIds: Set<Long> = emptySet(),
    val selectedGroupIds: Set<Int> = emptySet(),
    val initialSelectedGroupIds: Set<Int> = emptySet(),
    val filter: GroupFilter = GroupFilter.ALL
) {
    val hasSelectionChanged: Boolean
        get() = selectedGroupIds != initialSelectedGroupIds
    
    val allGroups: List<WordGroup>
        get() = groupsByChapter.values.flatten()

    val isAllSelected: Boolean
        get() {
            val groupIdsInExpandedChapters = expandedChapterIds
                .flatMap { chapterId -> groupsByChapter[chapterId] ?: emptyList() }
                .map { it.groupId }
                .toSet()
            return groupIdsInExpandedChapters.isNotEmpty() && selectedGroupIds.containsAll(groupIdsInExpandedChapters)
        }

    fun getFilteredGroupsForChapter(chapterId: Long): List<WordGroup> {
        val groups = groupsByChapter[chapterId] ?: emptyList()
        return when (filter) {
            GroupFilter.ALL -> groups
            GroupFilter.NOT_LISTENED_TODAY -> groups.filter { !it.listenedToday }
            GroupFilter.LISTENED_TODAY -> groups.filter { it.listenedToday }
            GroupFilter.REVIEWED_TODAY -> groups.filter { it.reviewedToday }
        }
    }
} 

enum class GroupFilter {
    ALL,
    NOT_LISTENED_TODAY,
    LISTENED_TODAY,
    REVIEWED_TODAY
}