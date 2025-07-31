package com.yourcompany.worklisten.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourcompany.worklisten.data.local.model.WordGroup
import kotlinx.coroutines.flow.Flow

/**
 * 单词组数据访问对象
 */
@Dao
interface WordGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: WordGroup): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<WordGroup>): List<Long>
    
    @Update
    suspend fun updateGroup(group: WordGroup)
    
    @Query("SELECT * FROM word_groups WHERE libraryId = :libraryId ORDER BY groupId ASC")
    fun getGroupsForLibrary(libraryId: Long): Flow<List<WordGroup>>

    @Query("SELECT * FROM word_groups WHERE libraryId = :libraryId ORDER BY groupId ASC")
    suspend fun getGroupsForLibraryOnce(libraryId: Long): List<WordGroup>
    
    @Query("SELECT * FROM word_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): WordGroup?
    
    @Query("SELECT * FROM word_groups WHERE libraryId = :libraryId AND groupId = :groupNum")
    suspend fun getGroupByLibraryAndGroupId(libraryId: Long, groupNum: Int): WordGroup?
    
    @Query("UPDATE word_groups SET playCount = playCount + 1, lastPlayedTime = :timestamp WHERE id = :groupId")
    suspend fun incrementPlayCount(groupId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE word_groups SET totalStudyCount = totalStudyCount + 1, lastStudyTime = :timestamp WHERE id = :groupId")
    suspend fun incrementStudyCount(groupId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE word_groups SET listenedToday = :listened WHERE id = :groupId")
    suspend fun updateGroupListened(groupId: Long, listened: Boolean)
    
    @Query("UPDATE word_groups SET reviewedToday = :reviewed WHERE id = :groupId")
    suspend fun updateGroupReviewed(groupId: Long, reviewed: Boolean)

    @Query("UPDATE word_groups SET isSelected = :selected WHERE libraryId = :libraryId")
    suspend fun updateAllGroupsSelected(libraryId: Long, selected: Boolean)

    @Query("UPDATE word_groups SET isSelected = :selected WHERE id IN (:groupIds)")
    suspend fun updateGroupsSelected(groupIds: List<Long>, selected: Boolean)

    @Query("SELECT * FROM word_groups WHERE libraryId = :libraryId AND isSelected = 1")
    suspend fun getSelectedGroupsForLibrary(libraryId: Long): List<WordGroup>
    
    @Query("UPDATE word_groups SET lastStudyTime = :timestamp WHERE id = :groupId")
    suspend fun updateLastStudyTime(groupId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE word_groups SET listenedToday = 0, reviewedToday = 0")
    suspend fun resetDailyFlags()
    
    @Query("DELETE FROM word_groups WHERE libraryId = :libraryId")
    suspend fun deleteAllGroupsFromLibrary(libraryId: Long)
    
    @Query("SELECT MAX(groupId) FROM word_groups WHERE libraryId = :libraryId")
    suspend fun getMaxGroupIdForLibrary(libraryId: Long): Int?

    @Query("UPDATE word_groups SET listenedToday = 1 WHERE id IN (:ids)")
    suspend fun updateGroupsListened(ids: List<Long>)

    @Query("UPDATE word_groups SET reviewedToday = 1 WHERE id IN (:ids)")
    suspend fun updateGroupsReviewed(ids: List<Long>)
}