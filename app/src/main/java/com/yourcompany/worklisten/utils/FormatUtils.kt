// FormatUtils.kt
package com.yourcompany.worklisten.utils

import androidx.compose.ui.graphics.Color

class FormatUtils {
	companion object PartOfSpeechHelper {
		// ===== 颜色定义 =====
		// 统一词性颜色为天蓝色
		private val POS_COLOR = Color(0xFF87CEEB) // 天蓝色
		val WORD_COLOR = Color(0xFF2E8B57) // 墨绿色
		
		/**
		 * 将英语或日语词性标记转换为中文显示形式
		 */
		fun getChinesePartOfSpeech(original: String?): String {
			if (original.isNullOrBlank()) {
				return ""
			}
			
			val pos = original.trim().lowercase()
			return when {
				// === 法语完整词性名称映射 ===
				pos == "nom" || pos == "substantif" -> "名词"
				pos == "verbe" -> "动词"
				pos == "adjectif" -> "形容词"
				pos == "adverbe" -> "副词"
				pos == "pronom" -> "代词"
				pos == "préposition" -> "介词"
				pos == "conjonction" -> "连词"
				pos == "numéral" || pos == "nombre" -> "数词"
				pos == "article" -> "冠词"
				pos == "interjection" -> "感叹词"
				
				// === 英/法常见缩写及性别标记 ===
				// 处理法语n.f./n.m.格式
				// 基础性别标记
				pos == "n.f." || pos == "f.f." -> "阴.名词"
				pos == "n.m." || pos == "m.m." -> "阳.名词"
				pos == "n." -> "名词"
				pos == "e.adj." -> "形容词"
				// 处理更灵活的词性格式
				pos.startsWith("n.") -> {
					when {
						pos.contains(".f.") -> "阴.名词"
						pos.contains(".m.") -> "阳.名词"
						else -> "名词"
					}
				}
				pos.startsWith("v.") -> {
					when {
						pos.contains(".t.") || pos.contains("transitif") -> "及物动词"
						pos.contains(".i.") || pos.contains("intransitif") -> "不及物动词"
						pos.contains(".pr.") -> "代动词"
						pos.contains(".impers.") -> "无人称动词"
						pos.contains(".aux.") -> "助动词"
						pos.contains(".mod.") -> "情态动词"
						else -> "动词"
					}
				}
				pos.startsWith("adj.") -> {
					when {
						pos.contains(".f.") -> "阴.形容词"
						pos.contains(".m.") -> "阳.形容词"
						pos.contains(".inv.") -> "不变形容词"
						else -> "形容词"
					}
				}
				// 处理常见法语词性缩写
				pos == "adv." || pos == "adverbe" -> "副词"
				pos == "adj." || pos == "adjectif" -> "形容词"
				pos == "art." || pos == "article" -> "冠词"
				pos == "conj." || pos == "conjonction" -> "连词"
				pos == "interj." || pos == "interjection" -> "感叹词"
				pos == "prép." || pos == "préposition" -> "介词"
				pos == "pron." || pos == "pronom" -> "代词"
				pos == "num." || pos == "numéral" || pos == "nombre" -> "数词"
				pos == "n.f." -> "阴.名词"
				pos == "n.m." -> "阳.名词"
				pos == "v.f." -> "及物动词"
				pos == "v.i." -> "不及物动词"
				pos == "v.pr." || pos == "verbe pronominal" -> "代动词"
				pos == "v.impers." || pos == "verbe impersonnel" -> "无人称动词"
				pos == "v.aux." || pos == "verbe auxiliaire" -> "助动词"
				pos == "v.mod." || pos == "verbe modal" -> "情态动词"
				// 增强法语名词阴阳性处理
				pos.startsWith("n.") -> {
					when {
						pos.contains("f") || pos.contains("féminin") || pos.contains("fem") || pos.contains("féminine") -> "阴.名词"
						pos.contains("m") || pos.contains("masc") || pos.contains("masculin") || pos.contains("masculine") -> "阳.名词"
						else -> "名词"
					}
				}
				// 处理法语复合词性标记
				pos.contains("nom") && pos.contains("masculin") -> "阳.名词"
				pos.contains("nom") && pos.contains("féminin") -> "阴.名词"
				pos.contains("adjectif") && pos.contains("masculin") -> "阳.形容词"
				pos.contains("adjectif") && pos.contains("féminin") -> "阴.形容词"
				pos.startsWith("v.") -> { // 动词细分
					when {
						pos.contains("t") || pos.contains("transitif") || pos.contains("transitive") -> "及物动词"
						pos.contains("i") || pos.contains("intransitif") || pos.contains("intransitive") -> "不及物动词"
						else -> "动词"
					}
				}
				pos.startsWith("adj") -> {
					when {
						pos.contains("f") || pos.contains("féminin") || pos.contains("fem") || pos.contains("féminine") -> "阴.形容词"
						pos.contains("m") || pos.contains("masc") || pos.contains("masculin") || pos.contains("masculine") -> "阳.形容词"
						else -> "形容词"
					}
				}
				pos.startsWith("adv") -> "副词"
				pos.startsWith("pron") -> "代词"
				pos.startsWith("prep") -> "介词"
				pos.startsWith("conj") -> "连词"
				pos.startsWith("num") -> "数词"
				pos.startsWith("art") -> "冠词"
				pos.startsWith("int") -> "感叹词"
				
				// === 日语方括号/圆括号标记 ===
				pos.contains("[名]") || (pos.contains("名") && !pos.contains("形")) -> "名词"
				pos.contains("[动]") || pos.contains("動") -> "动词"
				pos.contains("[形]") -> "形容词"
				pos.contains("イ型") || pos.contains("イ形") -> "イ形容词"
				pos.contains("ナ型") || pos.contains("ナ形") -> "ナ形容词"
				
				else -> original.trim() // 如果以上都不匹配，返回原始词性，确保不会崩溃，但可能没有颜色
			}
		}
		
		/**
		 * 返回统一的词性颜色
		 */
		fun getColorForPartOfSpeech(original: String?): Color {
			return POS_COLOR
		}

		/**
		 * 返回单词本体颜色
		 */
		fun getWordColor(): Color {
			return WORD_COLOR
		}
	}
}
