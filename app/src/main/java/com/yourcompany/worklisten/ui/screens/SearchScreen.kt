package com.yourcompany.worklisten.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.yourcompany.worklisten.ui.components.SearchResults
import com.yourcompany.worklisten.ui.components.TopSearchBar
import com.yourcompany.worklisten.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
	viewModel: SearchViewModel,
	navController: NavHostController
) {
	Scaffold { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
		) {
			// 使用TopSearchBar组件作为搜索页面的顶部栏
			TopSearchBar(
				title = "单词搜索",
				searchQuery = viewModel.uiState.collectAsState().value.searchQuery,
				onSearchQueryChange = { viewModel.updateSearchQuery(it) },
				showSearchBar = true, // 始终显示搜索框
				onBackClick = {
					// 返回到上一个页面
					navController.popBackStack()
				}
			)
			
			// 搜索结果显示区域
			SearchResults(
				searchViewModel = viewModel,
				modifier = Modifier.fillMaxSize()
			)
		}
	}
}
