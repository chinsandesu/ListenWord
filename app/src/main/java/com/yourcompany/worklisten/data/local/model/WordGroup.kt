package com.yourcompany.worklisten.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词组实体，表示50个单词一组的分组信息
 */
@Entity(
    tableName = "word_groups",
    foreignKeys = [
        ForeignKey(
            entity = WordLibrary::class,
            parentColumns = ["id"],
            childColumns = ["libraryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WordChapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["libraryId", "groupId"], unique = true),
        Index(value = ["chapterId"])
    ]
)
data class WordGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val libraryId: Long,
    val chapterId: Long, // 所属章节ID
    val groupId: Int,
    val wordCount: Int, // 组内单词数量
    val playCount: Int = 0, // 播放次数
    val listenedToday: Boolean = false, // 今日是否已经听过
    val reviewedToday: Boolean = false, // 今日是否已经复习过
    val lastPlayedTime: Long = 0, // 最后播放时间戳
    val lastStudyTime: Long = 0, // 最后学习时间戳
    val totalStudyCount: Int = 0 ,// 总学习次数
    val isSelected: Boolean = false // 是否被选择
)