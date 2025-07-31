package com.yourcompany.worklisten.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourcompany.worklisten.data.local.model.WordChapter
import kotlinx.coroutines.flow.Flow

@Dao
interface WordChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: WordChapter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<WordChapter>): List<Long>

    @Query("SELECT * FROM word_chapters WHERE libraryId = :libraryId ORDER BY chapterNumber ASC")
    fun getChaptersForLibrary(libraryId: Long): Flow<List<WordChapter>>

    @Query("SELECT * FROM word_chapters WHERE libraryId = :libraryId ORDER BY chapterNumber ASC")
    suspend fun getChaptersForLibraryOnce(libraryId: Long): List<WordChapter>

    @Query("SELECT * FROM word_chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: Long): WordChapter?
    
    @Query("DELETE FROM word_chapters WHERE libraryId = :libraryId")
    suspend fun deleteChaptersForLibrary(libraryId: Long)
} 