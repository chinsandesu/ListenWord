package com.yourcompany.worklisten.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourcompany.worklisten.data.local.model.WordChapter
import com.yourcompany.worklisten.ui.components.EmptyState
import com.yourcompany.worklisten.ui.components.GroupCard
import com.yourcompany.worklisten.ui.components.LoadingIndicator
import com.yourcompany.worklisten.ui.components.SelectAllButton
import com.yourcompany.worklisten.ui.viewmodel.GroupFilter
import com.yourcompany.worklisten.ui.viewmodel.GroupViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupScreen(viewModel: GroupViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.padding(top = 16.dp))
                SelectAllButton(
                    allSelected = uiState.isAllSelected,
                    onSelectAll = { viewModel.toggleSelectAll() }
                )
                HorizontalDivider()
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        GroupFilter.ALL to "全部",
                        GroupFilter.NOT_LISTENED_TODAY to "未听过",
                        GroupFilter.LISTENED_TODAY to "已听过",
                        GroupFilter.REVIEWED_TODAY to "已刷过"
                    )
                    filters.forEach { (filter, title) ->
                        val selected = uiState.filter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updateFilter(filter) },
                            label = { Text(title) }
                        )
                    }
                }
                HorizontalDivider()
            }
        },
        bottomBar = {
            if (uiState.hasSelectionChanged) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.saveSelection() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("保存选择")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.activeLibrary == null -> EmptyState("请先在“词库”页面选择一个激活的词库")
            else -> {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.chapters, key = { it.id }) { chapter ->
                        val isExpanded = uiState.expandedChapterIds.contains(chapter.id)
                        ChapterHeader(
                            chapter = chapter,
                            isExpanded = isExpanded,
                            onToggle = { viewModel.toggleChapterExpansion(chapter.id) }
                        )

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                val groupsInChapter = uiState.getFilteredGroupsForChapter(chapter.id)
                                if (groupsInChapter.isNotEmpty()) {
                                    groupsInChapter.forEach { group ->
                                        GroupCard(
                                            group = group,
                                            isSelected = uiState.selectedGroupIds.contains(group.groupId),
                                            onToggle = { viewModel.toggleGroupSelection(group.groupId) }
                                        )
                                    }
                                } else {
                                    Text(
                                        "无符合条件的组",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterHeader(
    chapter: WordChapter,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = chapter.title,
            style = MaterialTheme.typography.titleMedium
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "折叠" else "展开",
            modifier = Modifier.size(24.dp)
        )
    }
}
