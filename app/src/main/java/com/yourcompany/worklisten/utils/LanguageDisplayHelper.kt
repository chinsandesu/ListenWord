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
					when {
						word.isJapanese && word.originalWord?.isNotBlank() == true -> {
							// 日语有汉字：3层显示
							// 第一行：汉字和假名（同一行显示，确保居中对齐）
							// 日语有汉字：上下分层显示，确保居中对齐
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
									append("\n") // 假名和词性之间添加换行符
									// 显示词性和意思
									if (partOfSpeech.isNotBlank()) {
										withStyle(style = SpanStyle(color = posColor)) {
											append(partOfSpeech)
										}
										append("  ") // 词性和意思之间用两个空格分隔
									}
									append("${word.meaning}")
						}
						
						else -> { // 日语无汉字/其他语言
							// 第一行：单词本体 (word.word)
							withStyle(
								style = SpanStyle(
									color = wordColor,
									fontWeight = FontWeight.Bold
								)
							) {
								append(word.word)
									}
									// 减少空行，直接显示词性和意思
									if (partOfSpeech.isNotBlank()) {
										append("\n")
										withStyle(style = SpanStyle(color = posColor)) {
											append(partOfSpeech)
										}
										append("  ") // 词性和意思之间用两个空格分隔
									}
									append("${word.meaning}")
						}
					}
				}
				
				ReviewDisplayMode.HIDE_WORD -> {
					// 只显示词性、意思
					if (partOfSpeech.isNotBlank()) {
						withStyle(style = SpanStyle(color = posColor)) {
							append(partOfSpeech)
						}
						append("  ") // 词性和意思之间用两个空格分隔
						append("${word.meaning}")
					}
				}
				
				ReviewDisplayMode.HIDE_MEANING -> {
						// 只显示单词、词性
						when {
							word.isJapanese && word.originalWord?.isNotBlank() == true -> {
								// 日语有汉字：显示汉字和假名（同一行显示，确保居中对齐）
								// 日语有汉字：上下分层显示，确保居中对齐
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
								// 日语无汉字/其他语言：显示单词本体
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
						if (partOfSpeech.isNotBlank()) {
							append(" ") // 单词和词性之间一个空格
							withStyle(style = SpanStyle(color = posColor)) {
								append("[$partOfSpeech]")
							}
						}
					}
				}
			}
		}

		/**
		 * 计算字体大小，根据内容长度动态调整
		 */
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
		    val baseMainSize = if (isReviewMode) 34 else 38  // 刷词模式减小4sp
		    val baseSubSize = if (isReviewMode) 12 else 20    // 刷词模式减小4sp，随身听模式增大4sp
		    val baseMeaningSize = if (isReviewMode) 14 else 18 // 刷词模式减小4sp
		
		    return TextStyleInfo(
		        isCentered = true,
		        mainTextSize = calculateFontSize(baseMainSize, word.originalWord?.length ?: word.word.length),
		        subTextSize = calculateFontSize(baseSubSize, word.word.length),
		        meaningTextSize = calculateFontSize(baseMeaningSize, word.meaning.length)
		    )
		}
	}
