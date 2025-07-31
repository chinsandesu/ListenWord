package com.yourcompany.worklisten.ui.components


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.yourcompany.worklisten.data.repository.BackgroundRepository
import com.yourcompany.worklisten.data.repository.SettingsRepository

@Composable
fun AppBackground(
	settingsRepository: SettingsRepository,
	backgroundRepository: BackgroundRepository,
	content: @Composable () -> Unit
) {
	var backgroundBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
	val backgroundFileName = settingsRepository.getBackgroundFileName()
	
	// 加载保存的背景图片
	LaunchedEffect(backgroundFileName) {
		backgroundBitmap = if (backgroundFileName != null) {
			backgroundRepository.getBackground(backgroundFileName)
		} else {
			null
		}
	}
	
	Box(modifier = Modifier.fillMaxSize()) {
		// 显示背景图片（如果有）
		backgroundBitmap?.let { bitmap ->
			Image(
				bitmap = bitmap.asImageBitmap(),
				contentDescription = "App background",
				modifier = Modifier.fillMaxSize(),
				contentScale = ContentScale.Crop // 按比例裁剪填满屏幕
			)
		}
		
		// 显示主要内容
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.TopStart
		) {
			content()
		}
	}
}