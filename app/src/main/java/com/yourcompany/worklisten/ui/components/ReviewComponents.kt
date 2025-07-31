package com.yourcompany.worklisten.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.viewmodel.ReviewDisplayMode

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.heightIn
import com.yourcompany.worklisten.utils.LanguageDisplayHelper
import androidx.compose.ui.text.font.FontWeight
import com.yourcompany.worklisten.utils.FormatUtils

/**
 * 复习单词卡片
 * 根据单词的语言类型调整显示方式：
 * - 日语：如果有汉字则三行显示（汉字、假名、词性+意思），如果没有汉字则两行显示（假名、词性+意思）
 * - 英语和法语：两行显示（单词、意思 或 单词、词性+意思）
 */




/**
 * 显示模式选择栏
 */
@Composable
fun DisplayModeSelector(
    currentMode: ReviewDisplayMode,
    onModeSelected: (ReviewDisplayMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),  // 减少垂直间距
        horizontalArrangement = Arrangement.spacedBy(8.dp),  // 增加选项间距
        verticalAlignment = Alignment.CenterVertically
    ) {
        DisplayModeOption(
            text = "显示全部",
            selected = currentMode == ReviewDisplayMode.SHOW_ALL,
            onClick = { onModeSelected(ReviewDisplayMode.SHOW_ALL) }
        )

        DisplayModeOption(
            text = "隐藏单词",
            selected = currentMode == ReviewDisplayMode.HIDE_WORD,
            onClick = { onModeSelected(ReviewDisplayMode.HIDE_WORD) }
        )

        DisplayModeOption(
            text = "隐藏意思",
            selected = currentMode == ReviewDisplayMode.HIDE_MEANING,
            onClick = { onModeSelected(ReviewDisplayMode.HIDE_MEANING) }
        )
    }
}

/**
 * 显示模式选项
 */
@Composable
private fun DisplayModeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),  // 缩小字体
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}
