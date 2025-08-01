package com.yourcompany.worklisten.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.ui.components.ConfirmationDialog
import com.yourcompany.worklisten.ui.components.EmptyState
import com.yourcompany.worklisten.ui.components.ImportLibraryDialog
import com.yourcompany.worklisten.ui.components.ImportResultDialog
import com.yourcompany.worklisten.ui.components.LibraryCard
import com.yourcompany.worklisten.ui.components.LoadingIndicator
import com.yourcompany.worklisten.ui.viewmodel.ImportResult
import com.yourcompany.worklisten.ui.viewmodel.LibraryViewModel

@Composable
private fun ImportInstructions() {
	var showDialog by remember { mutableStateOf(false) }

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = "词库导入说明",
			style = MaterialTheme.typography.titleMedium,
		)
		Spacer(modifier = Modifier.width(8.dp))
		IconButton(
			onClick = { showDialog = true }
		) {
			Icon(
				imageVector = Icons.Default.Info,
				contentDescription = "导入说明",
				tint = MaterialTheme.colorScheme.primary
			)
		}
	}

	// 点击外部关闭对话框
	if (showDialog) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black.copy(alpha = 0.3f))
				.clickable {
					showDialog = false
				},
			contentAlignment = Alignment.Center
		) {
			Card(
				modifier = Modifier
					.width(300.dp)
					.padding(16.dp),
				shape = MaterialTheme.shapes.medium,
				colors = CardDefaults.cardColors(
					containerColor = MaterialTheme.colorScheme.surface
				)
			) {
				Column(
					modifier = Modifier.fillMaxWidth()
				) {
					Text(
						text = "文件导入说明",
						style = MaterialTheme.typography.titleMedium,
						modifier = Modifier.padding(bottom = 8.dp)
					)
					Text(
						text = "支持 TXT, CSV 文件。格式规则：使用英文逗号分隔各个字段。",
						style = MaterialTheme.typography.bodyMedium,
						lineHeight = 20.sp,
						modifier = Modifier.padding(bottom = 8.dp)
					)
					Text(
						text = "内容格式示例 (每行一个单词):",
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.padding(bottom = 4.dp)
					)
					Text(
						text = "• 英语: apple,n.,苹果\n" +
								"• 法语: bonheur,n.m.,幸福；快乐\n" +
								"• 日语: あいさつ,[名],问候；寒暄",
						style = MaterialTheme.typography.bodySmall,
						lineHeight = 18.sp,
						modifier = Modifier.padding(start = 8.dp)
					)
					Spacer(modifier = Modifier.height(16.dp))
					TextButton(
						onClick = { showDialog = false },
						modifier = Modifier.align(Alignment.End)
					) {
						Text("知道了")
					}
				}
			}
		}
	}
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // 对话框状态
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var libraryToDelete by remember { mutableStateOf<com.yourcompany.worklisten.data.local.model.WordLibrary?>(null) }
    
    // 文件选择器启动器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            // 确保永久访问权限
            context.contentResolver.takePersistableUriPermission(
                selectedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            // 保存URI并显示导入对话框
            viewModel.setSelectedUri(selectedUri)
            showImportDialog = true
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            // 添加词库按钮
            FloatingActionButton(
                onClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                content = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.library_import)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 标题
            Text(
                text = "词库管理",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            ImportInstructions()

            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.libraries.isEmpty() -> {
                    EmptyState(
                        message = "还没有词库\n点击右下角按钮，导入一个开始学习吧！",
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.libraries) { library ->
                            LibraryCard(
                                library = library,
                                onActivate = {
                                    viewModel.activateLibrary(library)
                                },
                                onDelete = {
                                    libraryToDelete = library
                                    showDeleteDialog = true
                                }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // 为FAB留出空间
                        }
                    }
                }
            }
        }
        
        // 导入词库对话框
        if (showImportDialog) {
            ImportLibraryDialog(
                onDismiss = { showImportDialog = false },
                onImport = { name ->
                    val selectedUri = viewModel.getSelectedUri()
                    if (selectedUri != null) {
                        viewModel.importLibrary(uri = selectedUri, name = name)
                        showImportDialog = false
                    }
                }
            )
        }
        
        // 删除确认对话框
        if (showDeleteDialog && libraryToDelete != null) {
            ConfirmationDialog(
                title = stringResource(R.string.delete_library),
                message = stringResource(R.string.library_delete_confirm),
                onConfirm = {
                    libraryToDelete?.let { viewModel.deleteLibrary(it) }
                    showDeleteDialog = false
                },
                onDismiss = {
                    showDeleteDialog = false
                }
            )
        }
        
        // 导入结果对话框
        uiState.importResult?.let { result ->
            when (result) {
                is ImportResult.Success -> {
                    ImportResultDialog(
                        success = true,
                        message = stringResource(
                            R.string.library_import_success,
                            result.importedCount,
                            result.skippedCount
                        ),
                        onDismiss = { viewModel.clearImportResult() }
                    )
                }
                is ImportResult.Error -> {
                    ImportResultDialog(
                        success = false,
                        message = result.message,
                        onDismiss = { viewModel.clearImportResult() }
                    )
                }
            }
        }
    }
}