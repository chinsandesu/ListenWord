package com.yourcompany.worklisten.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词章节实体，用于组织单词组
 */
@Entity(
    tableName = "word_chapters",
    foreignKeys = [
        ForeignKey(
            entity = WordLibrary::class,
            parentColumns = ["id"],
            childColumns = ["libraryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["libraryId", "chapterNumber"], unique = true)
    ]
)
data class WordChapter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val libraryId: Long,
    val chapterNumber: Int, // 章节号
    val title: String // 章节标题，例如 "第一章" 或 "1-10组"
) 