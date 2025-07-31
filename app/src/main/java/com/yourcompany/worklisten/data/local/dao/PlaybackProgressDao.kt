package com.yourcompany.worklisten.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourcompany.worklisten.data.local.model.PlaybackProgress
import kotlinx.coroutines.flow.Flow

/**
 * 播放进度数据访问对象
 */
@Dao
interface PlaybackProgressDao {

    @Query("SELECT * FROM playback_progress LIMIT 1")
    fun getProgress(): Flow<PlaybackProgress?>
    
    @Query("SELECT * FROM playback_progress LIMIT 1")
    suspend fun getProgressOnce(): PlaybackProgress?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: PlaybackProgress)
    
    @Update
    suspend fun updateProgress(progress: PlaybackProgress)
    
    @Query("UPDATE playback_progress SET selectedGroups = :selectedGroups")
    suspend fun updateSelectedGroups(selectedGroups: String)
    
    @Query("UPDATE playback_progress SET activeLibraryId = :libraryId")
    suspend fun updateActiveLibrary(libraryId: Long)
    
    @Query("UPDATE playback_progress SET currentIndex = :index WHERE id = 1")
    suspend fun updateCurrentIndex(index: Int)
    
    @Query("UPDATE playback_progress SET playbackSpeed = :speed WHERE id = 1")
    suspend fun updatePlaybackSpeed(speed: Float)
    
    @Query("UPDATE playback_progress SET playbackInterval = :interval WHERE id = 1")
    suspend fun updatePlaybackInterval(interval: Float)
    
    @Query("UPDATE playback_progress SET isRandom = :isRandom WHERE id = 1")
    suspend fun updateRandomMode(isRandom: Boolean)
    
    @Query("UPDATE playback_progress SET isLoop = :isLoop WHERE id = 1")
    suspend fun updateLoopMode(isLoop: Boolean)
    
    @Query("UPDATE playback_progress SET playbackMode = :mode WHERE id = 1")
    suspend fun updatePlaybackMode(mode: String)
    
    @Query("UPDATE playback_progress SET lastPlayedTime = :timestamp WHERE id = 1")
    suspend fun updateLastPlayedTime(timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE playback_progress SET wordRepeatCount = :count WHERE id = 1")
    suspend fun updateWordRepeatCount(count: Int)
}