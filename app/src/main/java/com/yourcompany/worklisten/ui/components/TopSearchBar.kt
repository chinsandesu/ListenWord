package com.yourcompany.worklisten.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.worklisten.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSearchBar(
	title: String,
	modifier: Modifier = Modifier,
	searchQuery: String = "",
	onSearchQueryChange: (String) -> Unit = {},
	showSearchBar: Boolean = false,
	onSearchClick: (() -> Unit)? = null,
	onBackClick: (() -> Unit)? = null
) {
	val focusRequester = remember { FocusRequester() }
	val focusManager = LocalFocusManager.current
	
	// 当搜索框显示时请求焦点，弹出键盘
	LaunchedEffect(showSearchBar) {
		if (showSearchBar) {
			focusRequester.requestFocus()
		} else {
			focusManager.clearFocus()
		}
	}
	
	Column(modifier = modifier.fillMaxWidth()) {
		// 顶部导航栏
		CenterAlignedTopAppBar(
			title = {
				if (!showSearchBar) {
					Text(title)
				}
			},
			navigationIcon = {
				if (showSearchBar) {
					IconButton(onClick = { onBackClick?.invoke() }) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "返回"
						)
					}
				}
			},
			actions = {
				if (!showSearchBar) {
					IconButton(onClick = { onSearchClick?.invoke() }) {
						Icon(
							imageVector = Icons.Default.Search,
							contentDescription = "搜索"
						)
					}
				}
			},
			modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
		)
		
		// 搜索框（仅当 showSearchBar 为 true 时显示）
		if (showSearchBar) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.background(MaterialTheme.colorScheme.background)
					.padding(16.dp)
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.background(
							color = MaterialTheme.colorScheme.surfaceVariant,
							shape = RoundedCornerShape(24.dp)
						)
						.padding(horizontal = 16.dp, vertical = 8.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
						imageVector = Icons.Default.Search,
						contentDescription = "搜索",
						tint = MaterialTheme.colorScheme.onSurfaceVariant
					)
					
					BasicTextField(
						value = searchQuery,
						onValueChange = onSearchQueryChange,
						modifier = Modifier
							.weight(1f)
							.padding(horizontal = 16.dp)
							.focusRequester(focusRequester),
						textStyle = TextStyle(
							fontSize = 16.sp,
							color = MaterialTheme.colorScheme.onSurface
						),
						singleLine = true,
						decorationBox = { innerTextField ->
							if (searchQuery.isEmpty()) {
								Text(
									text = "搜索单词、汉字或释义...",
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
							}
							innerTextField()
						}
					)
					
					if (searchQuery.isNotEmpty()) {
						IconButton(onClick = { onSearchQueryChange("") }) {
							Icon(
								imageVector = Icons.Default.Clear,
								contentDescription = "清除",
								tint = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
					}
				}
			}
		}
	}
}
