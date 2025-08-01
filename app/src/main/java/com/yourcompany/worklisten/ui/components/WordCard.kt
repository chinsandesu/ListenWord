package com.yourcompany.worklisten.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.viewmodel.PlaybackMode

@Composable
fun WordCard(
    word: Word?,
    playbackMode: PlaybackMode,
    showMeaning: Boolean,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 8.dp)
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // 降低透明度以减少对背景的遮挡
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = word,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "WordCardContent"
            ) {
                targetWord ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 单词区域
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (targetWord != null && playbackMode != PlaybackMode.CN_ONLY) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // 日语汉字（如果有）
                                if (targetWord.isJapanese && targetWord.originalWord?.isNotBlank() == true) {
                                    Text(
                                        text = targetWord.originalWord,
                                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                                // 单词/假名
                                Text(
                                    text = targetWord.word,
                                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                // 词性
                                targetWord.wordType?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 释义区域
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (targetWord != null && showMeaning && playbackMode != PlaybackMode.WORD_ONLY) {
                            Text(
                                text = targetWord.meaning,
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
