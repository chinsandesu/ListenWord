package com.yourcompany.worklisten.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.ui.components.DisplayModeSelector
import com.yourcompany.worklisten.ui.components.EmptyState
import com.yourcompany.worklisten.ui.components.LoadingIndicator
import com.yourcompany.worklisten.ui.viewmodel.ReviewViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color

import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.viewmodel.ReviewDisplayMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yourcompany.worklisten.utils.LanguageDisplayHelper
import androidx.compose.ui.text.AnnotatedString


@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 移除了自己实现的背景图片显示逻辑
        
        when {
            uiState.isLoading -> {
                LoadingIndicator()
            }
            uiState.hasNoLibrary -> {
                EmptyState(message = stringResource(id = R.string.no_library))
            }
            uiState.hasNoWords -> {
                EmptyState(message = stringResource(id = R.string.no_words))
            }
            else -> {
                // 修复：分页流收集方式，使用collectAsLazyPagingItems
                val pagingItems = viewModel.pagedWords.collectAsLazyPagingItems()
                ReviewContent(
                    pagingItems = pagingItems,
                    displayMode = uiState.displayMode,
                    onDisplayModeChanged = viewModel::updateDisplayMode,
                    onSpeak = viewModel::speakWord,
                    onScrolledToBottom = viewModel::setScrolledToBottom
                )
            }
        }
    }
}

@Composable
private fun ReviewContent(
    pagingItems: androidx.paging.compose.LazyPagingItems<Word>,
    displayMode: ReviewDisplayMode,
    onDisplayModeChanged: (ReviewDisplayMode) -> Unit,
    onSpeak: (Word) -> Unit,
    onScrolledToBottom: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 标题
        Text(
            text = "刷词模式",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        
        // 显示模式选择器
        DisplayModeSelector(
            currentMode = displayMode,
            onModeSelected = onDisplayModeChanged
        )
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
        
        // 单词列表
        val listState = rememberLazyListState()
        
        // 检测是否已滚动到底部
        val isAtBottom by remember {
            derivedStateOf {
                val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isEmpty()) {
                    false
                } else {
                    val lastVisibleItem = visibleItemsInfo.last()
                    val lastIndex = pagingItems.itemCount - 1
                    lastVisibleItem.index == lastIndex && lastVisibleItem.offset + lastVisibleItem.size <= listState.layoutInfo.viewportEndOffset
                }
            }
        }
        
        // 当滚动到底部时通知ViewModel
        LaunchedEffect(isAtBottom) {
            if (isAtBottom) {
                onScrolledToBottom(true)
            }
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            items(pagingItems.itemCount) {
                val word = pagingItems[it]
                word?.let {
                    ReviewWordCard(
                        word = it,
                        displayMode = displayMode,
                        onSpeak = { onSpeak(it) } // Correctly call onSpeak
                    )
                    Spacer(modifier = Modifier.height(2.dp)) // 减小单词间距
                }
            }
        }
    }
}

@Composable
fun ReviewWordCard(
    word: Word,
    displayMode: ReviewDisplayMode,
    onSpeak: (Word) -> Unit
) {
    val formattedText = LanguageDisplayHelper.getFormattedWordDisplay(word, displayMode)
        // 刷词模式，将isReviewMode设置为true
        val textStyleInfo = LanguageDisplayHelper.getTextStyleInfo(word, true)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .defaultMinSize(minHeight = 100.dp) // 缩小卡片高度
                .padding(horizontal = 12.dp, vertical = 4.dp) // 减小卡片边距
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), // 降低透明度到0.4以减少对背景的遮挡
                    RoundedCornerShape(12.dp)
                )
                .clickable { onSpeak(word) }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = textStyleInfo.mainTextSize.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
}
