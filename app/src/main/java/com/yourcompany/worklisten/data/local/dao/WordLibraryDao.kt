package com.yourcompany.worklisten.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.yourcompany.worklisten.data.local.model.WordLibrary
import kotlinx.coroutines.flow.Flow

/**
 * 词库数据访问对象
 */
@Dao
interface WordLibraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibrary(library: WordLibrary): Long
    
    @Update
    suspend fun updateLibrary(library: WordLibrary)
    
    @Delete
    suspend fun deleteLibrary(library: WordLibrary)
    
    @Query("SELECT * FROM word_libraries ORDER BY createdAt DESC")
    fun getAllLibraries(): Flow<List<WordLibrary>>
    
    @Query("SELECT * FROM word_libraries WHERE id = :libraryId")
    suspend fun getLibraryById(libraryId: Long): WordLibrary?
    
    @Query("SELECT * FROM word_libraries WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveLibrary(): WordLibrary?
    
    @Query("UPDATE word_libraries SET isActive = 0")
    suspend fun clearActiveLibraries()
    
    @Query("UPDATE word_libraries SET isActive = 1 WHERE id = :libraryId")
    suspend fun setLibraryActive(libraryId: Long)
    
    @Transaction
    suspend fun activateLibrary(libraryId: Long) {
        clearActiveLibraries()
        setLibraryActive(libraryId)
    }
    
    @Query("SELECT COUNT(*) FROM word_libraries")
    suspend fun getLibraryCount(): Int
} 