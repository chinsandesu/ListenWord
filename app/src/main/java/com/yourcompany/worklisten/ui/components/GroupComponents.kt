package com.yourcompany.worklisten.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.data.local.model.WordGroup
import android.text.format.DateFormat

/**
 * 单词组卡片
 */
@Composable
fun GroupCard(
	group: WordGroup,
	isSelected: Boolean,
	onToggle: () -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(4.dp)
			.clickable(onClick = onToggle),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.padding(8.dp)
		) {
			Checkbox(
				checked = isSelected,
				onCheckedChange = { onToggle() }
			)
			
			Spacer(modifier = Modifier.width(8.dp))
			
			// 组信息
			Row(
				modifier = Modifier.weight(1f),
				verticalAlignment = Alignment.CenterVertically
			) {
				// 组号和单词数
				Text(
					text = stringResource(R.string.group, group.groupId + 1),
					style = MaterialTheme.typography.titleMedium
				)
				
				Spacer(modifier = Modifier.width(8.dp))
				
				Text(
					text = stringResource(R.string.words_count, group.wordCount),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
				)
				
				// 填充空间
				Spacer(modifier = Modifier.weight(1f))
				
				// 学习次数 - 始终显示（即使为0）
				Text(
					text = "${group.totalStudyCount}次",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
				)
				
				Spacer(modifier = Modifier.width(8.dp))
				
				// 最后学习时间 - 始终显示（未学习时显示"未学习"）
				Text(
					text = if (group.lastStudyTime > 0) {
						DateFormat.format("MM-dd", group.lastStudyTime).toString()
					} else {
						"未学习"
					},
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
				)
				
				Spacer(modifier = Modifier.width(8.dp))
				
				// 今日标记
				if (group.listenedToday) {
					StatusDot(
						color = colorResource(R.color.listened_mark),
						contentDesc = stringResource(R.string.listened)
					)
				}
				
				if (group.reviewedToday) {
					Spacer(modifier = Modifier.width(4.dp))
					
					StatusDot(
						color = colorResource(R.color.accent_light),
						contentDesc = stringResource(R.string.reviewed)
					)
				}
			}
		}
	}
}

/**
 * 状态点
 */
@Composable
private fun StatusDot(
	color: androidx.compose.ui.graphics.Color,
	contentDesc: String
) {
	Box(
		modifier = Modifier
			.size(8.dp)
			.clip(CircleShape)
			.background(color)
			.semantics { contentDescription = contentDesc }
	)
}

/**
 * 全选/全不选按钮
 */
@Composable
fun SelectAllButton(
	allSelected: Boolean,
	onSelectAll: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically
		) {
			Checkbox(
				checked = allSelected,
				onCheckedChange = { onSelectAll() }
			)
			
			Spacer(modifier = Modifier.width(8.dp))
			
			Text(
				text = if (allSelected) "全不选" else "全选",
				style = MaterialTheme.typography.bodyLarge
			)
		}
	}
}