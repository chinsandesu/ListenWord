// LanguageDisplayHelper.kt
package com.yourcompany.worklisten.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.ui.viewmodel.ReviewDisplayMode
import kotlin.math.max
import kotlin.math.min

/**
 * 语言显示帮助类，用于处理不同语言单词的显示逻辑
 */
object LanguageDisplayHelper {
	
	/**
	 * 根据单词的语言类型和显示模式获取要显示的文本
	 * @param word 单词对象
	 * @param displayMode 当前的显示模式
	 * @return 要显示的AnnotatedString
	 */
	fun getFormattedWordDisplay(word: Word, displayMode: ReviewDisplayMode): AnnotatedString {
		val partOfSpeech = FormatUtils.getChinesePartOfSpeech(word.wordType)
		val posColor = FormatUtils.getColorForPartOfSpeech(word.wordType)
		val wordColor = FormatUtils.getWordColor()

		return buildAnnotatedString {
			when (displayMode) {
				ReviewDisplayMode.SHOW_ALL -> {
					// 第一部分：单词
					when {
						word.isJapanese && word.originalWord?.isNotBlank() == true -> {
							// 日语有汉字：显示汉字和假名
							withStyle(
								style = SpanStyle(
									color = wordColor,
									fontWeight = FontWeight.Bold
								)
							) {
								append(word.originalWord)
							}
							append("\n") // 汉字和假名之间添加换行符
							withStyle(
								style = SpanStyle(
									color = wordColor,
									fontWeight = FontWeight.Bold
								)
							) {
								append(word.word)
							}
						} else ->{
							// 其他语言或无汉字的日语：显示单词本体
							withStyle(
								style = SpanStyle(
									color = wordColor,
									fontWeight = FontWeight.Bold
								)
							) {
								append(word.word)
							}
						}
					}

					// 第二部分：词性+意思
					append("\n") // 单词和词性之间添加换行符
					if (partOfSpeech.isNotBlank()) {
						withStyle(style = SpanStyle(color = posColor)) {
							append(partOfSpeech)
						}
						append("  ") // 词性和意思之间添加两个空格
					}
					if (word.meaning.isNotBlank()) {
						append(word.meaning)
					} else {
						append("无释义")
					}
				}

				ReviewDisplayMode.HIDE_WORD -> {
					// 第一部分：词性+意思
					if (partOfSpeech.isNotBlank()) {
						withStyle(style = SpanStyle(color = posColor)) {
							append(partOfSpeech)
						}
						append(" ") // 词性和意思之间添加空格
					}
					if (word.meaning.isNotBlank()) {
						append(word.meaning)
					} else {
						append("无释义")
					}
				}

				ReviewDisplayMode.HIDE_MEANING -> {
					// 第一部分：单词
					when {
						word.isJapanese && word.originalWord?.isNotBlank() == true -> {
							// 日语有汉字：显示汉字和假名
							withStyle(
								style = SpanStyle(
									color = wordColor,
									fontWeight = FontWeight.Bold
								)
							) {
								append(word.originalWord)
							}
							append("\n") // 汉字和假名之间添加换行符
							withStyle(
								style = SpanStyle(
									color = wordColor,
									fontWeight = FontWeight.Bold
								)
							) {
								append(word.word)
							}
						}
						else -> {
							// 其他语言：显示单词本体
							withStyle(
								style = SpanStyle(
									color = wordColor,
									fontWeight = FontWeight.Bold
								)
							) {
								append(word.word)
							}
						}
					}

					// 第二部分：词性（仅当有词性时显示）
					if (partOfSpeech.isNotBlank()) {
						append("\n") // 单词和词性之间添加换行符
						withStyle(style = SpanStyle(color = posColor)) {
							append(partOfSpeech)
						}
					}
				}
			}
		}
	}
		
		/**
		 * 计算字体大小，根据内容长度动态调整
		 */
		// 添加空行以保持代码结构一致
		
		// 修复：确保在HIDE_MEANING模式下不显示单词意思
		// 修改内容：
		// 1. 在HIDE_MEANING模式下，单词和词性之间添加换行符，使显示更清晰
		// 2. 移除了词性外的方括号，保持与其他模式一致的显示风格
		// 3. 确保在任何情况下都不会访问word.meaning
		// 4. 修复了HIDE_WORD模式下的逻辑，确保即使partOfSpeech为空也能显示意思

		fun calculateFontSize(baseSize: Int, contentLength: Int, maxReduction: Int = 4): Int {
			val effectiveLength = contentLength * 2 
			val reduction = min(effectiveLength / 20 * 2, maxReduction)
			return max(baseSize - reduction, baseSize - maxReduction)
		}
		
		data class TextStyleInfo(
			val isCentered: Boolean,
			val mainTextSize: Int,
			val subTextSize: Int,
			val meaningTextSize: Int
		)
		
		/**
		 * 根据单词的语言类型和显示模式获取要显示的文本样式
		 * @param word 单词对象
		 * @param isReviewMode 是否为刷词模式 (true: 刷词模式, false: 随身听模式)
		 */
		fun getTextStyleInfo(word: Word, isReviewMode: Boolean = true): TextStyleInfo {
		    // 基础字体大小设置
		    val baseMainSize = if (isReviewMode) 28 else 38  // 刷词模式减小10sp(原34-6)
		    val baseSubSize = if (isReviewMode) 8 else 20    // 刷词模式减小12sp(原12-4)
		    val baseMeaningSize = if (isReviewMode) 10 else 18 // 刷词模式减小8sp(原14-4)
		
		    return TextStyleInfo(
		        isCentered = true,
		        mainTextSize = calculateFontSize(baseMainSize, word.originalWord?.length ?: word.word.length),
		        subTextSize = calculateFontSize(baseSubSize, word.word.length),
		        meaningTextSize = calculateFontSize(baseMeaningSize, word.meaning.length)
		    )
		}
	}
