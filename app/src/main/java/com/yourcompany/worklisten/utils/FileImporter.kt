package com.yourcompany.worklisten.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.data.local.model.WordChapter
import com.yourcompany.worklisten.data.local.model.WordGroup
import com.yourcompany.worklisten.data.local.model.WordLibrary
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.mozilla.universalchardet.UniversalDetector
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import android.util.Log

/**
 * 文件导入工具类
 */
class FileImporter(private val context: Context) {
	
	private val TAG = "FileImporter"
	
	// 内部数据类，用于临时存储解析出的单词信息，wordType 允许为空
	private data class ParsedWord(
		val word: String,           // 单词本体（如日语假名、英语单词）
		val originalWord: String,   // 汉字（日语专用，其他语言可空）
		val wordType: String?,      // 词性（如 [n.]、[名]，可空）
		val meaning: String,        // 中文含义（不可空）
		val isJapanese: Boolean     // 是否为日语单词
	)
	
	companion object {
		private const val WORDS_PER_GROUP = 50
		private const val GROUPS_PER_CHAPTER = 10
	}
	
	/**
	 * 获取AssetManager
	 */
	fun getAssetManager() = context.assets
	
	/**
	 * 从Assets目录导入文件
	 */
	fun importFromAssets(fileName: String, libraryName: String): ImportResult {
		return try {
			val inputStream = context.assets.open(fileName)
			when {
				fileName.endsWith(".csv", true) -> importCsv(inputStream, libraryName, ',')
				fileName.endsWith(".txt", true) -> importTxt(inputStream, libraryName)
				fileName.endsWith(".xls", true) || fileName.endsWith(".xlsx", true) -> importExcel(inputStream, libraryName)
				else -> ImportResult.Error("不支持的文件类型")
			}
		} catch (e: Exception) {
			Log.e(TAG, "从Assets导入失败: ${e.message}", e)
			ImportResult.Error("从Assets导入失败: ${e.message}")
		}
	}
	
	/**
	 * 导入TXT文件
	 */
	internal fun importTxt(
		inputStream: InputStream,
		libraryName: String
	): ImportResult {
		try {
			val reader = detectCharsetAndGetReader(inputStream)
			val parsedWords = mutableListOf<ParsedWord>()
			
			reader.useLines { lines ->
				lines.forEach { line ->
					val trimmedLine = line.trim()
					if (trimmedLine.isBlank()) return@forEach
					
					// TXT 文件可能使用多种分隔符，这里仍然主要考虑逗号。
					// 对于 TXT，我们直接将整行作为需要 WordParser.parseColumns 处理的 "parts"
					// 如果一行中包含逗号，它会被 split，然后由 WordParser 智能处理
					val parts = trimmedLine.split(",").map { it.trim() }
					
					// 调用 WordParser 的新方法来解析列
					val parsedData = WordParser.parseColumns(parts)
					
					if (parsedData.word.isNotBlank() && parsedData.meaning.isNotBlank()) {
						parsedWords.add(ParsedWord(
							word = parsedData.word,
							originalWord = parsedData.originalWord,
							wordType = parsedData.wordType,
							meaning = parsedData.meaning,
							isJapanese = parsedData.isJapanese
						))
					} else {
						Log.w(TAG, "跳过无效或不完整数据行: $trimmedLine")
					}
				}
			}
			return processParsedWords(libraryName, parsedWords)
		} catch (e: Exception) {
			Log.e(TAG, "导入TXT失败: ${e.message}", e)
			return ImportResult.Error("导入TXT失败: ${e.message}")
		}
	}
	
	/**
	 * 导入CSV文件
	 */
	fun importCsvFile(
		uri: Uri,
		libraryName: String,
		delimiter: Char = ','
	): ImportResult {
		return try {
			val inputStream = context.contentResolver.openInputStream(uri)
				?: return ImportResult.Error("无法打开文件")
			importCsv(inputStream, libraryName, delimiter)
		} catch (e: Exception) {
			Log.e(TAG, "导入CSV文件失败: ${e.message}", e)
			ImportResult.Error("导入失败: ${e.message}")
		}
	}
	
	/**
	 * 导入CSV文件
	 */
	private fun importCsv(
		inputStream: InputStream,
		libraryName: String,
		delimiter: Char
	): ImportResult {
		try {
			val reader = detectCharsetAndGetReader(inputStream)
			val parsedWords = mutableListOf<ParsedWord>()
			val csvReader = CSVReaderBuilder(reader)
				.withCSVParser(CSVParserBuilder().withSeparator(delimiter).build())
				.withSkipLines(0)
				.build()
			
			val rows = csvReader.readAll()
			if (rows.isEmpty()) {
				return ImportResult.Error("文件为空")
			}
			csvReader.close()
			
			rows.forEach { row ->
				val parts = row.map { it.trim() } // 确保每个部分都被trim
				val parsedData = WordParser.parseColumns(parts)
				
				if (parsedData.word.isNotBlank() && parsedData.meaning.isNotBlank()) {
					parsedWords.add(ParsedWord(
						word = parsedData.word,
						originalWord = parsedData.originalWord,
						wordType = parsedData.wordType,
						meaning = parsedData.meaning,
						isJapanese = parsedData.isJapanese
					))
				} else {
					Log.w(TAG, "跳过无效或不完整CSV行: ${row.joinToString(delimiter.toString())}")
				}
			}
			
			return processParsedWords(libraryName, parsedWords)
			
		} catch (e: Exception) {
			Log.e(TAG, "导入CSV失败: ${e.message}", e)
			return ImportResult.Error("导入失败: ${e.message}")
		}
	}
	
	/**
	 * 导入Excel文件
	 */
	fun importExcelFile(
		uri: Uri,
		libraryName: String
	): ImportResult {
		return try {
			val inputStream = context.contentResolver.openInputStream(uri) ?: return ImportResult.Error("无法打开文件")
			importExcel(inputStream, libraryName)
		} catch (e: Exception) {
			Log.e(TAG, "导入Excel文件失败: ${e.message}", e)
			ImportResult.Error("导入Excel失败: ${e.message}")
		}
	}
	
	/**
	 * 导入Excel文件 (内部方法)
	 */
	private fun importExcel(
		inputStream: InputStream,
		libraryName: String
	): ImportResult {
		try {
			val parsedWords = mutableListOf<ParsedWord>()
			
			// 使用Apache POI解析Excel文件
			val workbook = WorkbookFactory.create(inputStream)
			val sheet = workbook.getSheetAt(0) // 只读取第一个工作表
			
			for (row in sheet) {
				// 将Excel行转换为List<String>
				val parts = mutableListOf<String>()
				// 假设最多有4列，根据实际情况调整
				for (i in 0 until 4) { // 遍历可能存在的列
					val cell = row.getCell(i)
					parts.add(cell?.stringCellValue?.trim() ?: "")
				}
				// 如果实际列数少于4，WordParser.parseColumns 会根据实际传入的part.size处理
				
				val parsedData = WordParser.parseColumns(parts)
				
				if (parsedData.word.isNotBlank() && parsedData.meaning.isNotBlank()) {
					parsedWords.add(ParsedWord(
						word = parsedData.word,
						originalWord = parsedData.originalWord,
						wordType = parsedData.wordType,
						meaning = parsedData.meaning,
						isJapanese = parsedData.isJapanese
					))
				} else {
					Log.w(TAG, "跳过无效或不完整Excel行 (行号: ${row.rowNum}): ${parts.joinToString(",")}")
				}
			}
			workbook.close()
			inputStream.close()
			
			return processParsedWords(libraryName, parsedWords)
			
		} catch (e: Exception) {
			Log.e(TAG, "导入Excel失败: ${e.message}", e)
			return ImportResult.Error("导入Excel失败: ${e.message}")
		}
	}
	
	/**
	 * 核心处理逻辑: 从解析出的单词列表创建数据库实体
	 */
	private fun processParsedWords(
		libraryName: String,
		parsedWords: List<ParsedWord>
	): ImportResult {
		Log.d(TAG, "Starting to process ${parsedWords.size} parsed words for library: $libraryName")
		val words = mutableListOf<Word>()
		val groups = mutableListOf<WordGroup>()
		val chapters = mutableListOf<WordChapter>()
		
		var wordCount = 0
		var skippedCount = 0
		
		var currentGroupWordCount = 0
		var currentGroupId = 0
		var currentChapterGroupCount = 0
		
		// 创建第一个章节
		var currentChapter = WordChapter(
			id = 0, // 在保存到数据库时会由Room自动生成
			libraryId = 0, // 导入时临时设为0，实际保存时会更新
			chapterNumber = 1,
			title = "第 1 章 (1-${GROUPS_PER_CHAPTER}组)"
		)
		chapters.add(currentChapter)
		
		parsedWords.forEach { parsedWord ->
			// 再次进行最终有效性检查，尽管 WordParser.parseColumns 已做初步检查
			if (parsedWord.word.isBlank() || parsedWord.meaning.isBlank()) {
				skippedCount++
				Log.w(TAG, "跳过因单词或含义为空的行: ${parsedWord.word}, ${parsedWord.meaning}")
				return@forEach
			}
			
			// 自动推断语言 (由 WordParser 完成大部分工作，这里只是传递)
			val isJapanese = parsedWord.isJapanese
			val isFrench = WordParser.isFrenchWord(parsedWord.word) // 重新验证法语，防止 WordParser 没判断到
			
			val language = if (isJapanese) "ja" else if (isFrench) "fr" else "en"
			
			val word = Word(
				libraryId = 0, // 导入时临时设为0，实际保存时会更新
				groupId = currentGroupId,
				word = parsedWord.word,
				originalWord = parsedWord.originalWord,
				meaning = parsedWord.meaning,
				wordType = parsedWord.wordType, // 词性现在是 String? 类型
				isJapanese = isJapanese,
				language = language
			)
			Log.d(TAG, "Adding word: ${word.word}, meaning: ${word.meaning}, type: ${word.wordType}, isJapanese: ${word.isJapanese}, language: ${word.language}")
			words.add(word)
			wordCount++
			currentGroupWordCount++
			
			// 当一个组满时，创建新的组
			if (currentGroupWordCount >= WORDS_PER_GROUP) {
				groups.add(
					WordGroup(
						libraryId = 0, // 临时ID
						chapterId = 0, // 临时ID
						groupId = currentGroupId,
						wordCount = currentGroupWordCount
					)
				)
				
				currentGroupId++
				currentGroupWordCount = 0
				currentChapterGroupCount++
				
				// 当一个章节满时，创建新的章节
				if (currentChapterGroupCount >= GROUPS_PER_CHAPTER) {
					val chapterNumber = chapters.size + 1
					val startGroup = (chapterNumber - 1) * GROUPS_PER_CHAPTER
					val endGroup = chapterNumber * GROUPS_PER_CHAPTER -1
					
					currentChapter = WordChapter(
						id = 0, // 临时ID
						libraryId = 0, // 临时ID
						chapterNumber = chapterNumber,
						title = "第 $chapterNumber 章 (${startGroup +1}-${endGroup+1}组)" // 确保组号从1开始
					)
					chapters.add(currentChapter)
					currentChapterGroupCount = 0
				}
			}
		}
		
		// 添加最后一个未满的组
		if (currentGroupWordCount > 0) {
			groups.add(
				WordGroup(
					libraryId = 0,
					chapterId = 0,
					groupId = currentGroupId,
					wordCount = currentGroupWordCount
				)
			)
		}
		
		// 根据实际导入的单词来推断词库的最终语言
		val finalLanguage = if (words.any { it.isJapanese }) {
			"ja"
		} else if (words.any { it.language == "fr" }) { // 检查是否有任何法语单词
			"fr"
		} else {
			"en"
		}
		
		val library = WordLibrary(
			name = libraryName,
			wordCount = wordCount,
			groupCount = groups.size,
			chapterCount = chapters.size,
			isActive = false,
			language = finalLanguage // 设置词库语言
		)
		
		return ImportResult.Success(library, words, groups, chapters, wordCount, skippedCount)
	}
	
	
	/**
	 * 根据文件扩展名导入文件
	 */
	fun importFile(
		uri: Uri,
		libraryName: String,
		delimiter: Char = ','
	): ImportResult {
		val fileName = getFileNameFromUri(uri) ?: return ImportResult.Error("无法获取文件名")
		
		return when {
			fileName.endsWith(".csv", true) -> importCsvFile(uri, libraryName, delimiter)
			fileName.endsWith(".txt", true) -> importTxtFile(uri, libraryName)
			fileName.endsWith(".xls", true) || fileName.endsWith(".xlsx", true) -> importExcelFile(uri, libraryName)
			else -> ImportResult.Error("不支持的文件类型")
		}
	}
	
	/**
	 * 导入用户TXT文件
	 */
	private fun importTxtFile(uri: Uri, libraryName: String): ImportResult {
		return try {
			val inputStream = context.contentResolver.openInputStream(uri)
				?: return ImportResult.Error("无法打开文件")
			importTxt(inputStream, libraryName)
		} catch (e: Exception) {
			Log.e(TAG, "导入TXT文件失败: ${e.message}", e)
			return ImportResult.Error("导入TXT失败: ${e.message}")
		}
	}
	
	/**
	 * 从Uri中获取文件名
	 */
	private fun getFileNameFromUri(uri: Uri): String? {
		var cursor: Cursor? = null
		try {
			cursor = context.contentResolver.query(uri, null, null, null, null)
			if (cursor != null && cursor.moveToFirst()) {
				val nameIndex = cursor.getColumnIndex("_display_name")
				if (nameIndex != -1) {
					return cursor.getString(nameIndex)
				}
			}
		} finally {
			cursor?.close()
		}
		return null
	}
	
	/**
	 * 检测输入流的字符集并返回一个Reader
	 */
	private fun detectCharsetAndGetReader(inputStream: InputStream): InputStreamReader {
		// 为了避免inputStream关闭导致后续读取失败，先全部读入ByteArray
		val buffer = inputStream.readBytes()
		val detector = UniversalDetector(null)
		
		detector.handleData(buffer, 0, buffer.size)
		detector.dataEnd()
		
		val detectedCharset = detector.detectedCharset
		detector.reset()
		
		val charset = if (detectedCharset != null && Charset.isSupported(detectedCharset)) {
			Charset.forName(detectedCharset)
		} else {
			Charsets.UTF_8 // 默认回退到UTF-8
		}
		
		return buffer.inputStream().reader(charset) // 从ByteArray创建新的InputStream
	}
	
	/**
	 * 导入结果的密封类
	 */
	sealed class ImportResult {
		data class Success(
			val library: WordLibrary,
			val words: List<Word>,
			val groups: MutableList<WordGroup>,
			val chapters: MutableList<WordChapter>,
			val importedCount: Int,
			val skippedCount: Int
		) : ImportResult()
		
		data class Error(val message: String) : ImportResult()
	}
}