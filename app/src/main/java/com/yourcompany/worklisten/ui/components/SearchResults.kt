package com.yourcompany.worklisten.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.viewmodel.SearchViewModel
import com.yourcompany.worklisten.utils.FormatUtils
import com.yourcompany.worklisten.utils.LanguageDisplayHelper


@Composable
fun SearchResults(
	searchViewModel: SearchViewModel,
	modifier: Modifier = Modifier
) {
	val uiState by searchViewModel.uiState.collectAsState()
	val scope = rememberCoroutineScope()
	
	// 仅当需要显示结果时才渲染
	if (uiState.showResults) {
		Box(
			modifier = modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
		) {
			Column(modifier = Modifier.fillMaxSize()) {
				// 结果标题
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = "搜索结果 (${uiState.searchResults.size})",
						style = MaterialTheme.typography.titleMedium
					)
					Button(
						onClick = {
							searchViewModel.toggleSearchVisibility()
						},
						colors = ButtonDefaults.buttonColors(
							containerColor = MaterialTheme.colorScheme.primary
						)
					) {
						Text("关闭")
					}
				}
				
				// 结果内容
				when {
					uiState.isSearching -> {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							CircularProgressIndicator()
						}
					}
					uiState.errorMessage != null -> {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							Text(
								text = uiState.errorMessage!!,
								color = MaterialTheme.colorScheme.error,
								fontSize = 16.sp
							)
						}
					}
					uiState.searchResults.isEmpty() -> {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							Text(
								text = "未找到匹配的单词",
								style = MaterialTheme.typography.bodyLarge,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
					}
					else -> {
						LazyColumn(
							modifier = Modifier
								.fillMaxSize()
								.padding(horizontal = 16.dp),
							verticalArrangement = Arrangement.spacedBy(12.dp),
							contentPadding = PaddingValues(bottom = 32.dp)
						) {
							items(uiState.searchResults) { word ->
								SearchResultCard(
									word = word,
									onSpeakClick = { searchViewModel.speakWord(word) }
								)
							}
						}
					}
				}
			}
			
			// TTS错误提示
			uiState.ttsError?.let { error ->
				Box(
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.padding(24.dp)
				) {
					Text(
						text = error,
						color = MaterialTheme.colorScheme.error,
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier
							.background(MaterialTheme.colorScheme.surface)
							.padding(8.dp)
							.clip(RoundedCornerShape(4.dp))
					)
				}
				LaunchedEffect(Unit) {
					kotlinx.coroutines.delay(3000)
					searchViewModel.clearError()
				}
			}
		}
	}
}

// 搜索结果卡片
@Composable
private fun SearchResultCard(
	word: Word,
	onSpeakClick: () -> Unit
) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp)
		) {
			// 单词显示（根据语言类型展示不同格式）
			// 单词显示
			if (word.isJapanese) {
				word.originalWord?.let {
					Text(
						text = it,
						style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
						modifier = Modifier.padding(vertical = 2.dp)
					)
				}
				Text(
					text = word.word,
					style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
					modifier = Modifier.padding(vertical = 2.dp)
				)
			} else {
				Text(
					text = word.word,
					style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
					modifier = Modifier.padding(vertical = 2.dp)
				)
			}
			
			// 词性显示
			word.wordType?.let {
				Text(
					text = FormatUtils.PartOfSpeechHelper.getChinesePartOfSpeech(it),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant, // 词性颜色
					modifier = Modifier.padding(vertical = 2.dp)
				)
			}
			
			// 释义显示
			Text(
				text = word.meaning,
				style = MaterialTheme.typography.bodyMedium,
				modifier = Modifier.padding(vertical = 2.dp)
			)
			// 发音按钮
			IconButton(
				onClick = onSpeakClick,
				modifier = Modifier.align(Alignment.End)
			) {
				Icon(
					imageVector = Icons.Default.VolumeUp,
					contentDescription = "发音",
					tint = MaterialTheme.colorScheme.primary
				)
			}
		}
	}
}
