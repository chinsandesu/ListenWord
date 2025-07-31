package com.yourcompany.worklisten.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 词库实体，代表一个独立的词库
 */
@Entity(tableName = "word_libraries")
data class WordLibrary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val wordCount: Int,
    val groupCount: Int,
    val chapterCount: Int = 0,
    val isActive: Boolean = false,
    val language: String = "en", // 词库语言，默认为英语
    val createdAt: Long = System.currentTimeMillis()
)