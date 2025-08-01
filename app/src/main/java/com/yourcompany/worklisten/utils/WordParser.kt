package com.yourcompany.worklisten.utils

import android.content.res.AssetManager
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object WordParser {

	private const val TAG = "WordParser"

	// 匹配常见词性的基本模式
	private val POS_FINDER_REGEX = Regex("\\b(adv|adj|art|aux|conj|int|n|num|prep|pron|v|vi|vt)\\b\\.?")
	// 匹配更灵活的词性缩写格式，如 n.m.、v.t.、adj.inv. 等
	private val POS_FLEXIBLE_PATTERN = Regex("^[a-z]{1,6}(\\.[a-z]{1,8})+\\.$")

	private val JAPANESE_CHAR_PATTERN = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF]+")
	private val JAPANESE_WORD_MAIN_PART_PATTERN = Regex("^([\\u3040-\\u309F\\u30A0-\\u30FFー]+)(?:【.+】|\\s+[\\u4E00-\\u9FFF]+)?")
	private val ENGLISH_WORD_PATTERN = Regex("^[a-zA-Z]+(?:[-'][a-zA-Z]+)*$")
	private val FRENCH_CHAR_PATTERN = Regex("^[a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\\-]+$")

	private val JAPANESE_POS_PATTERN = Regex("\\[(名|动|形|イ型|ナ型|副|助|助動|連体|接頭|接尾|感動|固有|引用|記号|助詞|副助詞|接続助詞|格助詞|係助詞|終助詞|間投助詞|形式名詞|动词型|形容词型|形容动词型|助词型|助动词型|连接词型|连体词型|感叹词型|数词型|前缀词型|后缀词型|副词|形容词|动词|名词|代词)\\.?\\]")
	private val ENGLISH_POS_PATTERN = Regex("^(n|v|adj|adv|pron|prep|conj|int|art|num|aux)\\.?$")
	// 扩展法语词性正则表达式，支持更多变体格式
	private val FRENCH_POS_PATTERN = Regex(
	    "^(n\\.|n\\.m\\.|n\\.f\\.|m\\.|f\\.|n\\.m\\.inv\\.|n\\.f\\.inv\\.|adj\\.|adj\\.inv\\.|e\\.adj\\.|adj\\.dem\\.|adj\\.poss\\.|adj\\.int\\.|adv\\.|v\\.|v\\.t\\.|v\\.i\\.|v\\.pr\\.|v\\.impers\\.|v\\.mod\\.|prep\\.|conj\\.|art\\.|pron\\.|pron\\.rel\\.|pron\\.pers\\.|pron\\.poss\\.|pron\\.dem\\.|num\\.|int\\.|aux\\.|interj\\.|loc\\.|loc\\.v\\.|loc\\.adj\\.|loc\\.adv\\.)$"
	)
	private val FRENCH_POS_IN_MEANING_PATTERN = Regex(
	    "\\[(n\\.|n\\.m\\.|n\\.f\\.|m\\.|f\\.|n\\.m\\.inv\\.|n\\.f\\.inv\\.|adj\\.|adj\\.inv\\.|e\\.adj\\.|adj\\.dem\\.|adj\\.poss\\.|adj\\.int\\.|adv\\.|v\\.|v\\.t\\.|v\\.i\\.|v\\.pr\\.|v\\.impers\\.|v\\.mod\\.|prep\\.|conj\\.|art\\.|pron\\.|pron\\.rel\\.|pron\\.pers\\.|pron\\.poss\\.|pron\\.dem\\.|num\\.|int\\.|aux\\.|interj\\.|loc\\.|loc\\.v\\.|loc\\.adj\\.|loc\\.adv\\.)\\]"
	)
	
	data class ParsedWordData(
		val word: String,
		val originalWord: String,
		val wordType: String?,
		val meaning: String,
		val isJapanese: Boolean
	)
	
	/**
	 * 解析单词列数据
	 * @param parts 拆分后的列数据
	 * @param fileName 文件名，用于判断法语单词
	 * @return 解析后的单词数据
	 */
	fun parseColumns(parts: List<String>, fileName: String = ""): ParsedWordData {
		var word = ""
		var originalWord = ""
		var wordType: String? = null
		var meaning = ""
		var isJapanese = false
		
		// 安全检查：确保parts至少有2个元素
		if (parts.size < 2) {
			throw IllegalArgumentException("CSV行必须至少包含2列数据")
		}
		
		when (parts.size) {
			2 -> {
				word = parts[0].trim()
				originalWord = word  // 设置原始单词
				meaning = parts[1].trim()
				isJapanese = isJapaneseWord(word)
			/**
	 * 从assets目录读取CSV文件并解析所有单词
	 * @param assetManager AssetManager实例，用于访问assets目录
	 * @param fileName CSV文件名
	 * @return 解析后的单词列表
	 */
	fun parseFromAssets(assetManager: AssetManager, fileName: String): List<ParsedWordData> {
		val wordList = mutableListOf<ParsedWordData>()

		try {
			// 打开assets目录中的文件
			val inputStream = assetManager.open(fileName)
			val reader = BufferedReader(InputStreamReader(inputStream))

			var line: String?
			// 读取文件的每一行
			while (reader.readLine().also { line = it } != null) {
				// 跳过空行
				if (line.isNullOrBlank()) continue

				// 使用parseLine函数解析每一行
				val parsedWord = parseLine(line!!)
				if (parsedWord != null) {
					wordList.add(parsedWord)
				} else {
					Log.w(TAG, "无法解析行: $line")
				}
			}

			reader.close()
			inputStream.close()

			Log.d(TAG, "成功解析 $fileName 文件，共 ${wordList.size} 个单词")
		} catch (e: IOException) {
			Log.e(TAG, "读取 $fileName 文件失败: ${e.message}", e)
		}

		return wordList
	}

	}
			3 -> {
				word = parts[0].trim()
				val secondPart = parts[1].trim()
				val thirdPart = parts[2].trim()
				
				// 使用when表达式改进语言识别逻辑，优先根据文件名判断法语
				val language = when {
					isJapaneseWord(word) -> "ja"
					fileName.contains("法语.csv") -> "fr"
					isFrenchWord(word) -> "fr"
					ENGLISH_WORD_PATTERN.matches(word) -> "en"
					else -> "unknown"
				}
				val isJapanese = language == "ja"
				val isFrench = language == "fr"
				
				if (isJapanese) {
					// 检查第二列是否是词性
					if (JAPANESE_POS_PATTERN.matches(secondPart)) {
						wordType = secondPart
						meaning = thirdPart
					} else {
						originalWord = secondPart
						meaning = thirdPart
						val (parsedTypeFromMeaning, cleanMeaning) = parseMeaningAndType(meaning, originalWord)
						wordType = parsedTypeFromMeaning
						meaning = cleanMeaning
					}
				} else if (isFrench) {
					// 处理法语单词
					// 使用灵活的词性模式增强识别能力
					originalWord = word  // 设置原始单词
				wordType = if (secondPart.isNotBlank() && (FRENCH_POS_PATTERN.matches(secondPart) || 
							FRENCH_POS_IN_MEANING_PATTERN.matches(secondPart) || 
							POS_FLEXIBLE_PATTERN.matches(secondPart.lowercase()))) secondPart else null
				meaning = thirdPart
					val (parsedTypeFromMeaning, cleanMeaning) = parseMeaningAndType(meaning, word)
					if (parsedTypeFromMeaning != null && wordType == null) {
						wordType = parsedTypeFromMeaning
					}
					meaning = cleanMeaning
				} else {
					// 处理英语和其他语言
					originalWord = word  // 设置原始单词
				wordType = if (secondPart.isNotBlank() && (ENGLISH_POS_PATTERN.matches(secondPart) || JAPANESE_POS_PATTERN.matches(secondPart))) secondPart else null
				meaning = thirdPart
					val (parsedTypeFromMeaning, cleanMeaning) = parseMeaningAndType(meaning, word)
					if (parsedTypeFromMeaning != null && wordType == null) {
						wordType = parsedTypeFromMeaning
					}
					meaning = cleanMeaning
				}
			}
			4 -> {
				word = parts[0].trim()
				// 判断parts[1]是否为词性标记
				val secondPart = parts[1].trim()
				originalWord = if (isPartOfSpeech(secondPart)) word else secondPart
				wordType = parts[2].trim().let { if (it.isNotBlank()) it else null }
				meaning = parts[3].trim()
				isJapanese = isJapaneseWord(word)

				// 如果wordType为null，尝试从secondPart提取词性
				if (wordType == null && isPartOfSpeech(secondPart)) {
					wordType = secondPart
				}

				// 调用parseMeaningAndType函数解析含义和词性，传递已有的词性信息
				val (parsedTypeFromMeaning, cleanMeaning) = parseMeaningAndType(meaning, word, wordType)
				if (parsedTypeFromMeaning != null && wordType == null) {
					wordType = parsedTypeFromMeaning
				}
				meaning = cleanMeaning
			}
			// 处理列数大于4的情况
			else -> {
				// 对于列数大于4的情况，前3列作为word、originalWord和基础词性
				word = parts[0].trim()
				val secondPart = parts[1].trim()
				// 判断parts[1]是否为词性标记
				originalWord = if (isPartOfSpeech(secondPart)) word else secondPart

				// 初始词性
				val baseWordType = parts[2].trim().let { if (it.isNotBlank()) it else null }

				// 收集所有词性
				val posList = mutableListOf<String>()
				if (baseWordType != null) {
					posList.add(baseWordType)
				}

				// 从第4列开始，检查每一列是否是词性
				val meaningParts = mutableListOf<String>()
				for (i in 3 until parts.size) {
					val part = parts[i].trim()
					// 使用灵活的词性模式匹配
					if (POS_FLEXIBLE_PATTERN.matches(part.lowercase()) ||
						FRENCH_POS_PATTERN.matches(part) ||
						ENGLISH_POS_PATTERN.matches(part) ||
						JAPANESE_POS_PATTERN.matches(part)) {
						posList.add(part)
					} else {
						meaningParts.add(part)
					}
				}

				// 合并词性
				wordType = if (posList.isNotEmpty()) posList.joinToString(", ") else null

				// 合并剩余部分作为含义
				meaning = meaningParts.joinToString(",").trim()
				isJapanese = isJapaneseWord(word)

				meaning = cleanMeaningString(meaning)
				Log.d(TAG, "处理列数大于4的行 (${parts.size}列): 单词='${word}', 词性='${wordType}', 含义='${meaning}'")
			}
		}
		
		if (isJapanese && originalWord.isBlank()) {
			val extractedKana = extractKana(word)
			if (extractedKana != word) {
				originalWord = word.replace(extractedKana, "").trim()
				word = extractedKana
			}
		}
		
		meaning = cleanMeaningString(meaning)
		return ParsedWordData(word, originalWord, wordType, meaning, isJapanese)
	}
	
	/**
	 * 从assets目录读取CSV文件并解析所有单词
	 * @param assetManager AssetManager实例，用于访问assets目录
	 * @param fileName CSV文件名
	 * @return 解析后的单词列表
	 */
	fun parseFromAssets(assetManager: AssetManager, fileName: String): List<ParsedWordData> {
		val wordList = mutableListOf<ParsedWordData>()

		try {
			// 打开assets目录中的文件
			val inputStream = assetManager.open(fileName)
			val reader = BufferedReader(InputStreamReader(inputStream))

			var line: String?
			// 读取文件的每一行
			while (reader.readLine().also { line = it } != null) {
				// 跳过空行
				if (line.isNullOrBlank()) continue

				// 使用parseLine函数解析每一行
				val parsedWord = parseLine(line!!)
				if (parsedWord != null) {
					wordList.add(parsedWord)
				} else {
					Log.w(TAG, "无法解析行: $line")
				}
			}

			reader.close()
			inputStream.close()

			Log.d(TAG, "成功解析 $fileName 文件，共 ${wordList.size} 个单词")
		} catch (e: IOException) {
			Log.e(TAG, "读取 $fileName 文件失败: ${e.message}", e)
		}

		return wordList
	}
	
	/**
	 * 解析一行文本为单词条目
	 * @param line 输入文本行
	 * @return 解析后的单词条目，若解析失败则返回null
	 */
	fun parseLine(line: String): ParsedWordData? { 
	    val parts = line.split(",").map { it.trim() }
	    if (parts.size < 2) return null

	    var word = parts[0].trim()
	    val raw = word
	    
	    // 移除括号内容
	    word = word.replace("""\s*\(.*?\)\s*""".toRegex(), "").replace(" ", "")

	    // 提取词性
    var wordType: String? = null
    var meaningStartIndex = 1
    for (i in 1 until parts.size) {
        val part = parts[i]
        if (isPartOfSpeech(part)) {
            wordType = part
            meaningStartIndex = i + 1
            break
        }
    }

	    val meaning = parts.subList(meaningStartIndex, parts.size).joinToString(",")
	    val isJapanese = isJapaneseWord(word)
	    var originalWord = raw
	    
	    // 处理日语单词
	    if (isJapanese && originalWord.isBlank()) {
	        val extractedKana = extractKana(word)
	        if (extractedKana != word) {
	            originalWord = word.replace(extractedKana, "").trim()
	            word = extractedKana
	        }
	    }

	    // 调用parseMeaningAndType函数进一步解析含义和词性
	    val (parsedTypeFromMeaning, cleanMeaning) = parseMeaningAndType(meaning, word, wordType)
	    if (parsedTypeFromMeaning != null && wordType == null) {
	        wordType = parsedTypeFromMeaning
	    }

	    return ParsedWordData(word, originalWord, wordType, cleanMeaning, isJapanese)
	}

	/**
	 * 解析含义和词性
	 * @param rawText 原始文本
	 * @param originalWord 原始单词（用于日志显示）
	 * @param existingPos 已有的词性信息（可选）
	 * @return 词性和清理后的含义
	 */
	fun parseMeaningAndType(rawText: String, originalWord: String = "", existingPos: String? = null): Pair<String?, String> {
		var text = rawText.trim()
		var pos: String? = existingPos
		
		val japanesePosMatch = JAPANESE_POS_PATTERN.find(text)
		if (japanesePosMatch != null) {
			pos = japanesePosMatch.value
			text = text.replace(pos, "").trim()
			if (text.startsWith(",")) {
				text = text.substring(1).trim()
			}
			return Pair(pos, cleanMeaningString(text))
		}

		// 检查法语词性模式 [n.m.], [n.f.], etc.
		val frenchPosMatch = FRENCH_POS_IN_MEANING_PATTERN.find(text)
		if (frenchPosMatch != null) {
			pos = frenchPosMatch.value
			text = text.replace(pos, "").trim()
			if (text.startsWith(",")) {
				text = text.substring(1).trim()
			}
			return Pair(pos, cleanMeaningString(text))
		}

		if (text.startsWith("[") && text.contains("]")) {
			val endBracketIndex = text.indexOf("]")
			if (endBracketIndex > 0) {
				val extractedPos = text.substring(0, endBracketIndex + 1)
				val posWithoutBrackets = extractedPos.replace("[", "").replace("]", "").lowercase()
				if (JAPANESE_POS_PATTERN.matches(extractedPos) || 
					ENGLISH_POS_PATTERN.matches(posWithoutBrackets.replace(".", "")) || 
					FRENCH_POS_PATTERN.matches(posWithoutBrackets) || 
					POS_FLEXIBLE_PATTERN.matches(posWithoutBrackets)) {
					pos = extractedPos
					text = text.substring(endBracketIndex + 1).trim()
					return Pair(pos, cleanMeaningString(text))
				}
			}
		}

		// 改进点号模式匹配，尝试匹配完整的词性标记		// 先尝试匹配法语词性模式
		val frenchPosMatch2 = FRENCH_POS_PATTERN.find(text)
		if (frenchPosMatch2 != null) {
			pos = frenchPosMatch2.value
			text = text.replace(pos, "").trim()
			return Pair(pos, cleanMeaningString(text))
		}

		// 再尝试匹配英语词性模式
		val englishPosMatch = ENGLISH_POS_PATTERN.find(text)
		if (englishPosMatch != null) {
			pos = englishPosMatch.value
			text = text.replace(pos, "").trim()
			return Pair(pos, cleanMeaningString(text))
		}

		// 最后尝试灵活词性模式
		val flexiblePosMatch = POS_FLEXIBLE_PATTERN.find(text.lowercase())
		if (flexiblePosMatch != null) {
			pos = flexiblePosMatch.value
			text = text.replace(pos, "").trim()
			return Pair(pos, cleanMeaningString(text))
		}

		// 使用原始单词而非处理后的文本显示
		val displayWord = if (originalWord.isNotBlank()) originalWord else rawText

		// 移除日志输出，避免单元测试依赖Android框架
		if (existingPos != null) {
			// 已提供词性信息，直接使用
		} else if (pos != null) {
			// 识别到词性
		} else {
			// 未识别到词性
		}

		return Pair(pos, cleanMeaningString(text))
	}
	
	private fun cleanMeaningString(meaning: String, preserveNewlines: Boolean = false): String {
		val lines = if (preserveNewlines) meaning.lines() else listOf(meaning)
		
		return lines.joinToString(if (preserveNewlines) "\n" else "； ") { line ->
			line.replace(Regex("[:：。.]+$"), "")
				.split("；")
				.flatMap { it.split(";") }
				.map { it.trim() }
				.filter { it.isNotBlank() }
				.joinToString("； ")
		}.trim()
	}
	
	fun isJapaneseWord(word: String): Boolean {
		return JAPANESE_CHAR_PATTERN.containsMatchIn(word) && !ENGLISH_WORD_PATTERN.matches(word)
	}
	
	fun extractKana(japaneseWord: String): String {
		val matcher = JAPANESE_WORD_MAIN_PART_PATTERN.find(japaneseWord.trim())
		return matcher?.groups?.get(1)?.value ?: japaneseWord
	}
	
	// 已知法语单词集合 (可扩展)
	private val KNOWN_FRENCH_WORDS = setOf(
		"croissance", "augmenter", "soulier", "chaussure", "renfort", 
		"agrandir", "accroître", "émission", "multiplier", "semer",
		"casser", "accroissement", "augmentation", "multiplication",
		"dodo", "joujou", "file", "sud", "faire", "dresser", "part"
	)

	// 法语特有的重音字符
	private val frenchAccentedChars = setOf('à', 'â', 'ä', 'é', 'è', 'ê', 'ë', 'ï', 'î', 'ô', 'ö', 'ù', 'û', 'ü', 'ÿ', 'ç')
	private val frenchAccentedCharsUpperCase = setOf('À', 'Â', 'Ä', 'É', 'È', 'Ê', 'Ë', 'Ï', 'Î', 'Ô', 'Ö', 'Ù', 'Û', 'Ü', 'Ÿ', 'Ç')

	/**
	 * 判断字符串是否为词性标记
	 */
	fun isPartOfSpeech(text: String): Boolean {
		return JAPANESE_POS_PATTERN.matches(text) ||
			   FRENCH_POS_PATTERN.matches(text) ||
			   ENGLISH_POS_PATTERN.matches(text) ||
			   POS_FLEXIBLE_PATTERN.matches(text.lowercase())
	}

	/**
	 * 判断单词是否为法语单词
	 */
	fun isFrenchWord(word: String): Boolean {
		// 检查是否在已知法语单词集合中
			if (word.lowercase() in KNOWN_FRENCH_WORDS) {
			return true
		}

		// 检查是否符合法语字符模式
		val isValidFrenchChars = FRENCH_CHAR_PATTERN.matches(word)
		// 检查是否包含法语特有的重音符号
		val hasAccent = word.any { it in frenchAccentedChars || it in frenchAccentedCharsUpperCase }

		// 符合法语字符模式且(包含重音符号或单词长度大于等于4或在已知法语单词集合中)
		return isValidFrenchChars && (hasAccent || word.length >= 4 || word.lowercase() in KNOWN_FRENCH_WORDS)
	}
	
	/**
	 * 根据单词、词性和含义猜测语言
	 */
	fun guessLanguage(word: String, type: String?, meaning: String): String {
		// Step 1: 如果是日语，直接返回
		if (isJapaneseWord(word)) {
			return "ja"
		}
		
		// Step 2: 如果词性明确是法语格式（如 n.f., v.t.），那就是法语
		if (type != null && Regex("^(n\\.|n\\.f\\.|n\\.m\\.|v\\.|v\\.t\\.|v\\.i\\.|adj\\.|adv\\.|pron\\.|aux\\.)$").matches(type.lowercase())) {
			return "fr"
		}

		// Step 3: 如果单词拼写特征符合法语规则
		if (Regex(".*(er|ir|re|aire|eur|tion|sion|ment|ette|age|ain|oir|isme|ance|ence|ure)$").matches(word.lowercase())) {
			return "fr"
		}
		
		// Step 4: 检查含义中是否包含法语词性标记
		if (FRENCH_POS_IN_MEANING_PATTERN.containsMatchIn(meaning)) {
			return "fr"
		}
		
		// 默认认为是英语
		return "en"
	}

	}
