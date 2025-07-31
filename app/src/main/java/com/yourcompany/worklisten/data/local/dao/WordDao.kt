package com.yourcompany.worklisten.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourcompany.worklisten.data.local.model.Word
import kotlinx.coroutines.flow.Flow

/**
 * 单词数据访问对象
 */
@Dao
interface WordDao {
	
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertWord(word: Word): Long
	
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertWords(words: List<Word>): List<Long>
	
	@Update
	suspend fun updateWord(word: Word)
	
	@Update
	suspend fun updateWords(words: List<Word>)
	
	@Query("SELECT * FROM words WHERE libraryId = :libraryId ORDER BY id")
	fun getWordsFromLibrary(libraryId: Long): Flow<List<Word>>
	
	@Query("SELECT * FROM words WHERE libraryId = :libraryId AND groupId IN (:groupIds) ORDER BY id ASC")
	fun getWordsForGroups(libraryId: Long, groupIds: List<Int>): Flow<List<Word>>
	
	@Query("SELECT * FROM words WHERE libraryId = :libraryId AND groupId IN (:groupIds) ORDER BY id ASC")
	suspend fun getWordsForGroupsOnce(libraryId: Long, groupIds: List<Int>): List<Word>
	
	@Query("SELECT * FROM words WHERE id = :wordId")
	suspend fun getWordById(wordId: Long): Word?
	
	@Query("SELECT COUNT(*) FROM words WHERE libraryId = :libraryId AND word = :word")
	suspend fun getWordCountByWordAndLibrary(libraryId: Long, word: String): Int
	
	@Query("UPDATE words SET hasListened = :hasListened WHERE id = :wordId")
	suspend fun updateWordListened(wordId: Long, hasListened: Boolean)
	
	@Query("UPDATE words SET hasReviewed = :hasReviewed WHERE id = :wordId")
	suspend fun updateWordReviewed(wordId: Long, hasReviewed: Boolean)
	
	@Query("DELETE FROM words WHERE libraryId = :libraryId")
	suspend fun deleteAllWordsFromLibrary(libraryId: Long)
	
	@Query("SELECT COUNT(*) FROM words WHERE libraryId = :libraryId")
	suspend fun getWordCountForLibrary(libraryId: Long): Int
	
	@Query("SELECT COUNT(*) FROM words WHERE libraryId = :libraryId AND groupId = :groupId")
	suspend fun getWordCountForGroup(libraryId: Long, groupId: Int): Int
	
	@Query("SELECT * FROM words WHERE libraryId = :libraryId ORDER BY id")
	fun pagingSourceWordsForLibrary(libraryId: Long): androidx.paging.PagingSource<Int, Word>
	
	@Query("SELECT * FROM words WHERE libraryId = :libraryId AND groupId IN (:groupIds) ORDER BY id ASC")
	fun pagingSourceWordsForGroups(
		libraryId: Long,
		groupIds: List<Int>
	): androidx.paging.PagingSource<Int, Word>
	
	@Query("UPDATE words SET hasListened = 1 WHERE id IN (:ids)")
	suspend fun updateWordsListened(ids: List<Long>)
	
	@Query("UPDATE words SET hasReviewed = 1 WHERE id IN (:ids)")
	suspend fun updateWordsReviewed(ids: List<Long>)
	
	/**
	 * 跨词库搜索：在所有词库中搜索，按originalWord去重
	 */

	@Query(
		"""
    SELECT * FROM words
    WHERE (originalWord = :query OR word = :query OR meaning = :query)  -- 精确匹配优先
    OR (originalWord LIKE :query || '%' OR word LIKE :query || '%' OR meaning LIKE :query || '%')  -- 前缀匹配
    OR (originalWord LIKE '%' || :query || '%' OR word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%')  -- 包含匹配
    ORDER BY
        CASE WHEN originalWord = :query OR word = :query OR meaning = :query THEN 1
             WHEN originalWord LIKE :query || '%' OR word LIKE :query || '%' OR meaning LIKE :query || '%' THEN 2
             ELSE 3 END
"""
	)
	suspend fun searchAllLibraries(query: String): List<Word>
}