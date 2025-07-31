import android.util.Log

object WordParser {
	
	private const val TAG = "WordParser"
	
	private val POS_FINDER_REGEX = Regex("\\b(adv|adj|art|aux|conj|int|n|num|prep|pron|v|vi|vt)\\b\\.?")
	
	private val JAPANESE_CHAR_PATTERN = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF]+")
	private val JAPANESE_WORD_MAIN_PART_PATTERN = Regex("^([\\u3040-\\u309F\\u30A0-\\u30FFー]+)(?:【.+】|\\s+[\\u4E00-\\u9FFF]+)?")
	private val ENGLISH_WORD_PATTERN = Regex("^[a-zA-Z]+(?:[-'][a-zA-Z]+)*$")
	private val FRENCH_CHAR_PATTERN = Regex("^[a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\\-]+$")
	
	private val JAPANESE_POS_PATTERN = Regex("\\[(名|动|形|イ型|ナ型|副|助|助動|連体|接頭|接尾|感動|固有|引用|記号|助詞|副助詞|接続助詞|格助詞|係助詞|終助詞|間投助詞|形式名詞|动词型|形容词型|形容动词型|助词型|助动词型|连接词型|连体词型|感叹词型|数词型|前缀词型|后缀词型|副词|形容词|动词|名词|代词)\\.?\\]")
	private val ENGLISH_POS_PATTERN = Regex("^(n|v|adj|adv|pron|prep|conj|int|art|num|aux)\\.?$")
	
	data class ParsedWordData(
		val word: String,
		val originalWord: String,
		val wordType: String?,
		val meaning: String,
		val isJapanese: Boolean
	)
	
	fun parseColumns(parts: List<String>): ParsedWordData {
		var word = ""
		var originalWord = ""
		var wordType: String? = null
		var meaning = ""
		var isJapanese = false
		
		when (parts.size) {
			2 -> {
				word = parts[0].trim()
				meaning = parts[1].trim()
				isJapanese = isJapaneseWord(word)
			}
			3 -> {
				word = parts[0].trim()
				val secondPart = parts[1].trim()
				val thirdPart = parts[2].trim()
				
				isJapanese = isJapaneseWord(word)
				
				if (isJapanese) {
					originalWord = secondPart
					meaning = thirdPart
					val (parsedTypeFromMeaning, cleanMeaning) = parseMeaningAndType(meaning)
					wordType = parsedTypeFromMeaning
					meaning = cleanMeaning
				} else {
					wordType = if (secondPart.isNotBlank() && (ENGLISH_POS_PATTERN.matches(secondPart) || JAPANESE_POS_PATTERN.matches(secondPart))) secondPart else null
					meaning = thirdPart
					val (parsedTypeFromMeaning, cleanMeaning) = parseMeaningAndType(meaning)
					if (parsedTypeFromMeaning != null && wordType == null) {
						wordType = parsedTypeFromMeaning
					}
					meaning = cleanMeaning
				}
			}
			4 -> {
				word = parts[0].trim()
				originalWord = parts[1].trim()
				wordType = parts[2].trim().let { if (it.isNotBlank()) it else null }
				meaning = parts[3].trim()
				isJapanese = isJapaneseWord(word)
				
				meaning = cleanMeaningString(meaning)
			}
			else -> {
				Log.w(TAG, "跳过无效格式行，列数不匹配 (expected 2, 3, or 4): ${parts.joinToString(",")}")
				return ParsedWordData("", "", null, "", false)
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
	
	fun parseMeaningAndType(rawText: String): Pair<String?, String> {
		var text = rawText.trim()
		var pos: String? = null
		
		val japanesePosMatch = JAPANESE_POS_PATTERN.find(text)
		if (japanesePosMatch != null) {
			pos = japanesePosMatch.value
			text = text.replace(pos, "").trim()
			if (text.startsWith(",")) {
				text = text.substring(1).trim()
			}
			Log.d(TAG, "Parsed Japanese word type: $pos, meaning: $text from text: $rawText")
			return Pair(pos, cleanMeaningString(text))
		}
		
		if (text.startsWith("[") && text.contains("]")) {
			val endBracketIndex = text.indexOf("]")
			if (endBracketIndex > 0) {
				val extractedPos = text.substring(0, endBracketIndex + 1)
				if (JAPANESE_POS_PATTERN.matches(extractedPos) || ENGLISH_POS_PATTERN.matches(extractedPos.replace("[", "").replace("]", "").replace(".", ""))) {
					pos = extractedPos
					text = text.substring(endBracketIndex + 1).trim()
					Log.d(TAG, "Parsed bracketed word type: $pos, meaning: $text from text: $rawText")
					return Pair(pos, cleanMeaningString(text))
				}
			}
		}
		
		val dotIndex = text.indexOf(".")
		if (dotIndex > 0 && dotIndex < text.length - 1) {
			val extractedPos = text.substring(0, dotIndex + 1)
			if (ENGLISH_POS_PATTERN.matches(extractedPos)) {
				pos = extractedPos
				text = text.substring(dotIndex + 1).trim()
				Log.d(TAG, "Parsed dot word type: $pos, meaning: $text from text: $rawText")
				return Pair(pos, cleanMeaningString(text))
			}
		}
		
		Log.d(TAG, "Parsed no recognized word type, meaning: $text from text: $rawText")
		return Pair(null, cleanMeaningString(text))
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
	
	/**
	 * 判断单词是否为法语单词 (是法语允许的字符 AND 包含重音符号)。
	 */
	fun isFrenchWord(word: String): Boolean {
		val conformsToFrenchCharPattern = FRENCH_CHAR_PATTERN.matches(word)
		
		val frenchAccentedChars = "àâäéèêëïîôöùûüÿç"
		val frenchAccentedCharsUpperCase = frenchAccentedChars.uppercase()
		val containsAccent = word.any {
			it in frenchAccentedChars || it in frenchAccentedCharsUpperCase
		}
		
		return conformsToFrenchCharPattern && containsAccent
	}
}