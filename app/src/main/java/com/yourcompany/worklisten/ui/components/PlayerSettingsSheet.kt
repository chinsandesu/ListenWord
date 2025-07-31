package com.yourcompany.worklisten.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yourcompany.worklisten.ui.viewmodel.PlaybackMode
import com.yourcompany.worklisten.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsSheet(
    sheetState: SheetState,
    playbackMode: PlaybackMode,
    isRandom: Boolean,
    isLoop: Boolean,
    playbackSpeed: Float,
    playbackInterval: Float,
    wordRepeatCount: Int,
    onDismiss: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
    onRandomToggle: () -> Unit,
    onLoopToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onIntervalChange: (Float) -> Unit,
    onWordRepeatCountChange: (Int) -> Unit,
    onLaunchImagePicker: () -> Unit,
    onRemoveBackgroundImage: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Text("播放设置", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = stringResource(id = playbackMode.displayNameResId),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    PlaybackMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(stringResource(id = mode.displayNameResId)) },
                            onClick = {
                                onModeChange(mode)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("随机播放")
                Switch(checked = isRandom, onCheckedChange = { onRandomToggle() })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("循环播放")
                Switch(checked = isLoop, onCheckedChange = { onLoopToggle() })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("语速: ${"%.1f".format(playbackSpeed)}x")
            Slider(
                value = playbackSpeed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..3.0f,
                steps = 14
            )

            Text("播放间隔: ${String.format("%.1f", playbackInterval)}s")
            Slider(
                value = playbackInterval,
                onValueChange = onIntervalChange,
                valueRange = 0.08f..5f,
                steps = 49 // (5 - 0.08) / 0.1 = 49.2 steps, rounded to 49
            )
            
            // 添加单词重复次数设置
            var repeatCount by remember(wordRepeatCount) { mutableStateOf(wordRepeatCount) }
            Text("单词重复次数: $repeatCount")
            Slider(
                value = repeatCount.toFloat(),
                onValueChange = { 
                    repeatCount = it.toInt()
                    onWordRepeatCountChange(repeatCount)
                },
                valueRange = 1f..5f,
                steps = 4, // 1, 2, 3, 4, 5
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Background Image Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = onLaunchImagePicker) {
                    Text("更换背景")
                }
                Button(
                    onClick = onRemoveBackgroundImage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("恢复默认")
                }
            }
        }
    }
}
