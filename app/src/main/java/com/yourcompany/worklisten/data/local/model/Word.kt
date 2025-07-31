package com.yourcompany.worklisten.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.intellij.lang.annotations.Language

/**
 * 单词实体，表示单个单词的所有信息
 */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = WordLibrary::class,
            parentColumns = ["id"],
            childColumns = ["libraryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["libraryId"])
    ]
)
data class Word(
	@PrimaryKey(autoGenerate = true)
    val id: Long = 0,
	val libraryId: Long,
	val groupId: Int, // 50个单词一组
	val word: String, // 单词
	val originalWord: String?, // 汉字
	val meaning: String, // 中文含义
	val wordType: String?, // 词性
	val isJapanese: Boolean, // 是否为日语单词
	val language: String,//语言类型
	val hasListened: Boolean = false, // 是否已经听过
	val hasReviewed: Boolean = false // 是否已经复习过
)