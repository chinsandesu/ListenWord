package com.yourcompany.worklisten.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign

import coil.compose.AsyncImage
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.components.EmptyState
import com.yourcompany.worklisten.ui.components.LoadingIndicator
import com.yourcompany.worklisten.ui.components.PlayerControls
import com.yourcompany.worklisten.ui.components.PlayerSettingsSheet

import com.yourcompany.worklisten.ui.components.TopSearchBar
import com.yourcompany.worklisten.ui.viewmodel.PlayerViewModel
import com.yourcompany.worklisten.ui.viewmodel.SearchViewModel
import com.yourcompany.worklisten.ui.viewmodel.PlaybackMode
import com.yourcompany.worklisten.utils.FormatUtils
import com.yourcompany.worklisten.utils.LanguageDisplayHelper

import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
	viewModel: PlayerViewModel = viewModel(),
	searchViewModel: SearchViewModel,
	navController: NavController
) {
	val uiState by viewModel.uiState.collectAsState()
	val scope = rememberCoroutineScope()
	val sheetState = rememberModalBottomSheetState()
	var showSheet by remember { mutableStateOf(false) }
	val context = LocalContext.current

	// 监听页面生命周期，在离开时停止播放
	val lifecycleOwner = LocalLifecycleOwner.current
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver {
			_, event ->
			when (event) {
				Lifecycle.Event.ON_PAUSE -> {
					// 页面失去焦点时停止播放
					viewModel.stopPlayback()
				}
				Lifecycle.Event.ON_RESUME -> {
					// 页面恢复焦点时不自动播放，保持停止状态
				}
				else -> {}
			}
		}

		lifecycleOwner.lifecycle.addObserver(observer)

		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
			// 页面销毁时确保停止播放
			viewModel.stopPlayback()
		}
	}
	
	// 图片选择器 - 获取内容并申请持久权限
	val imagePickerLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenDocument()
	) { uri: Uri? ->
		uri?.let {
			// 申请永久访问权限
			val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
			context.contentResolver.takePersistableUriPermission(uri, takeFlags)
			viewModel.updateBackgroundImage(uri)
		}
	}
	
	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			TopSearchBar(
				title = "随身听",
				showSearchBar = false, // PlayerScreen 不显示搜索框
				onSearchClick = {
					// 当搜索图标被点击时，导航到搜索屏幕
					navController.navigate("search_screen")
				}
			)
		}
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(it)
		) {
			// 移除了重复的Box和背景图片显示逻辑
			
			// 主内容区域
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				when {
					uiState.isLoading -> LoadingIndicator()
					!uiState.hasWords -> EmptyState(message = stringResource(R.string.no_words_in_group))
					else -> {
						Column(
							modifier = Modifier.fillMaxSize(),
							horizontalAlignment = Alignment.CenterHorizontally,
							verticalArrangement = Arrangement.Center
						) {
							key(uiState.currentWord?.id) {
								WordDisplayCard(
									word = uiState.currentWord,
									playbackMode = uiState.playbackMode,
									onCardClicked = { viewModel.onCardClicked() }
								)
							}
							Spacer(modifier = Modifier.height(32.dp))
							PlayerControls(
								isPlaying = uiState.isPlaying,
								onPlayPause = { viewModel.togglePlayPause() },
								onNext = { viewModel.nextWord() },
								onPrevious = { viewModel.previousWord() },
								onSettings = { showSheet = true }
							)
						}
					}
				}
			}
			// 设置底部弹窗
			if (showSheet) {
				PlayerSettingsSheet(
					sheetState = sheetState,
					playbackMode = uiState.playbackMode,
					isRandom = uiState.isRandom,
					isLoop = uiState.isLoop,
					playbackSpeed = uiState.playbackSpeed,
					playbackInterval = uiState.playbackInterval,
					wordRepeatCount = uiState.wordRepeatCount,
					onDismiss = {
						scope.launch {
							sheetState.hide()
							showSheet = false
						}
					},
					onModeChange = viewModel::setPlaybackMode,
					onRandomToggle = viewModel::toggleRandomMode,
					onLoopToggle = viewModel::toggleLoopMode,
					onSpeedChange = viewModel::setPlaybackSpeed,
					onIntervalChange = viewModel::setPlaybackInterval,
					onWordRepeatCountChange = viewModel::setWordRepeatCount,
					onLaunchImagePicker = { imagePickerLauncher.launch(arrayOf("image/*")) },
					onRemoveBackgroundImage = {
						viewModel.removeBackgroundImage()
						scope.launch {
							sheetState.hide()
							showSheet = false
						}
					}
				)
			}
		}
	}
}

@Composable
fun WordDisplayCard(
	word: Word?,
	playbackMode: PlaybackMode,
	onCardClicked: () -> Unit
) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.wrapContentHeight()
			.defaultMinSize(minHeight = 300.dp)
			.padding(16.dp)
			.background(
				MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), // 降低透明度到0.4以减少对背景的遮挡
				RoundedCornerShape(16.dp)
			)
			.clickable(onClick = onCardClicked)
			.padding(16.dp),
		contentAlignment = Alignment.Center
	) {
		if (word != null) {
			// 随身听模式，将isReviewMode设置为false
			val textStyleInfo = LanguageDisplayHelper.getTextStyleInfo(word, false)
			
			Column(
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.CenterHorizontally,
				modifier = Modifier.padding(16.dp)
			) {
				// 单词区域
				val showWordContent = playbackMode != PlaybackMode.CN_ONLY && playbackMode != PlaybackMode.HIDE_ALL
				if (showWordContent) {
					val wordToDisplay = if (word.isJapanese) {
						// 对于日语，如果单词可见，始终显示汉字和假名
						val kanji = word.originalWord
						val kana = word.word
						if (kanji?.isNotBlank() == true) {
							"$kanji\n$kana"
						} else {
							word.word
						}
					} else {
						word.word
					}
					Text(
					text = wordToDisplay,
					style = MaterialTheme.typography.displayMedium.copy(
						fontSize = textStyleInfo.mainTextSize.sp,
						fontWeight = FontWeight.Bold,
						color = FormatUtils.WORD_COLOR
					),
					textAlign = TextAlign.Center,
					modifier = Modifier.fillMaxWidth()
				)
					
					word.wordType?.let {
						Text(
						text = FormatUtils.PartOfSpeechHelper.getChinesePartOfSpeech(it),
						style = MaterialTheme.typography.titleMedium.copy(
							fontSize = (textStyleInfo.subTextSize + 4).sp,
							color = FormatUtils.getColorForPartOfSpeech(it)
						),
						textAlign = TextAlign.Center,
						modifier = Modifier.fillMaxWidth()
					)
					}
				}
				
				// 释义区域
				val showMeaningContent = playbackMode != PlaybackMode.WORD_ONLY && playbackMode != PlaybackMode.HIDE_ALL
				if (showMeaningContent) {
					// 只有当单词和释义都可见时才添加间距
					if (showWordContent) {
						Spacer(modifier = Modifier.height(16.dp))
					}
					Text(
					text = word.meaning,
					style = MaterialTheme.typography.titleLarge.copy(
						fontSize = (textStyleInfo.meaningTextSize + 4).sp,
						color = MaterialTheme.colorScheme.onSurface
					),
					textAlign = TextAlign.Center,
					modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
				)
				}
			}
		}
	}
}
