package com.yourcompany.worklisten.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 播放进度实体，保存用户的播放状态
 */
@Entity(tableName = "playback_progress")
data class PlaybackProgress(
    @PrimaryKey
    val id: Int = 1, // 只有一条记录
    val activeLibraryId: Long = -1, // 当前激活的词库ID
    val selectedGroups: String = "", // 存储选定的组ID，用逗号分隔
    val currentIndex: Int = 0, // 当前播放的单词索引
    val lastPlayedTime: Long = 0, // 最后播放时间
    val playbackSpeed: Float = 1.0f, // 播放速度
    val playbackInterval: Float = 0.1f, // 播放间隔（秒）
    val isRandom: Boolean = false, // 是否随机播放
    val isLoop: Boolean = true, // 是否循环播放
    val playbackMode: String = "WORD_TO_CN", // 播放模式：HIDE_ALL, CN_ONLY, WORD_ONLY, WORD_TO_CN
    val wordRepeatCount: Int = 1 // 单词重复次数
)