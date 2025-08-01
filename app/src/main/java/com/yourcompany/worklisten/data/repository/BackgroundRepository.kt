package com.yourcompany.worklisten.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.os.FileObserver
import kotlinx.coroutines.channels.awaitClose

class BackgroundRepository(private val context: Context) {
	// 私有文件夹路径：/data/data/com.yourcompany.worklisten/files/backgrounds
	private val backgroundDir by lazy {
		File(context.filesDir, "backgrounds").apply {
			if (!exists()) mkdirs() // 确保文件夹存在
		}
	}
	
	/**
	 * 保存图片到私有目录
	 * @param uri 图片原始URI
	 * @return 保存后的文件名（用于后续读取）
	 */
	suspend fun saveBackground(uri: Uri): String? = withContext(Dispatchers.IO) {
		return@withContext try {
			// 获取原始文件的MIME类型
			val mimeType = context.contentResolver.getType(uri)
			// 根据MIME类型确定文件扩展名
			val extension = when (mimeType) {
				"image/jpeg" -> "jpg"
				"image/png" -> "png"
				"image/webp" -> "webp"
				"image/gif" -> "gif"
				else -> {
					// 尝试从URI获取扩展名
					val fileName = uri.lastPathSegment?.substringAfterLast('.', "jpg")
					fileName ?: "jpg"
				}
			}
			
			// 生成唯一文件名并保留正确扩展名
			val fileName = "bg_${System.currentTimeMillis()}.$extension"
			val outputFile = File(backgroundDir, fileName)
			
			// 从URI读取图片并保存到私有目录
			context.contentResolver.openInputStream(uri)?.use { inputStream ->
				FileOutputStream(outputFile).use { outputStream ->
					inputStream.copyTo(outputStream)
				}
			}
			fileName // 返回保存的文件名
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}
	
	/**
	 * 读取保存的背景图片
	 */
	suspend fun getBackground(fileName: String): Bitmap? = withContext(Dispatchers.IO) {
		return@withContext try {
			val file = File(backgroundDir, fileName)
			if (file.exists()) {
				// 先获取图片尺寸
				val options = BitmapFactory.Options().apply {
					inJustDecodeBounds = true
				}
				BitmapFactory.decodeFile(file.absolutePath, options)
				
				// 使用固定分辨率2560x1440，避免设备屏幕尺寸获取问题
				val maxWidth = 2560
				val maxHeight = 1440
				
				// 计算缩放比例
				val scaleFactor = calculateInSampleSize(options, maxWidth, maxHeight)
				
				// 解码并缩放图片
				val decodeOptions = BitmapFactory.Options().apply {
					inSampleSize = scaleFactor
					inPreferredConfig = Bitmap.Config.ARGB_8888 // 支持透明通道
					// 移除可能导致问题的配置
				}
				BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
			} else {
				null
			}
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}
	
	/**
	 * 删除背景图片
	 */
	suspend fun deleteBackground(fileName: String): Boolean = withContext(Dispatchers.IO) {
		val file = File(backgroundDir, fileName)
		return@withContext if (file.exists()) {
			file.delete()
		} else {
			true
		}
	}
	
	/**
	 * 清除所有背景图片
	 */
	suspend fun clearAllBackgrounds(): Boolean = withContext(Dispatchers.IO) {
		return@withContext try {
			backgroundDir.listFiles()?.forEach { it.delete() }
			true
		} catch (e: Exception) {
			e.printStackTrace()
			false
		}
	}
	
	/**
	 * 观察背景图片文件列表变化
	 */
	fun observeBackgroundFiles(): Flow<List<File>> = callbackFlow {
		// 初始发送一次文件列表
		trySend(backgroundDir.listFiles()?.toList() ?: emptyList())
		
		// 创建文件观察者监听目录变化
		val fileObserver = object : FileObserver(backgroundDir.absolutePath, CREATE or DELETE or MODIFY) {
			override fun onEvent(event: Int, path: String?) {
				if (path != null) {
					trySend(backgroundDir.listFiles()?.toList() ?: emptyList())
				}
			}
		}
		
		// 开始观察
		fileObserver.startWatching()
		
		// 等待通道关闭
		awaitClose {
			fileObserver.stopWatching()
		}
	}
	
	/**
	 * 计算图片缩放比例
	 * 恢复为原始实现，确保兼容性
	 */
	private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
		val height = options.outHeight
		val width = options.outWidth
		var inSampleSize = 1
	
		if (height > reqHeight || width > reqWidth) {
			val halfHeight = height / 2
			val halfWidth = width / 2
			
			// 计算最大的inSampleSize值，该值是2的幂，同时保持高度和宽度大于请求的高度和宽度
			while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
				inSampleSize *= 2
			}
		}
		return inSampleSize
	}
}