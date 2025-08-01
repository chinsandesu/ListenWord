package com.yourcompany.worklisten.data.repository

import com.yourcompany.worklisten.data.local.AppDatabase
import com.yourcompany.worklisten.data.local.model.PlaybackProgress
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.data.local.model.WordGroup
import com.yourcompany.worklisten.data.local.model.WordLibrary
import com.yourcompany.worklisten.utils.FileImporter
import kotlinx.coroutines.flow.Flow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 单词数据仓库
 * 处理应用中所有与单词数据相关的操作
 */
class WordRepository(private val database: AppDatabase, private val fileImporter: FileImporter) {
    /**
     * 保存选中组（全局，仅一条记录）
     */
    suspend fun saveSelectedGroups(groupIds: List<Int>) {
        val selectedGroups = groupIds.joinToString(",")
        playbackProgressDao.updateSelectedGroups(selectedGroups)
    }

    /**
     * 获取选中组（全局，仅一条记录）
     */
    suspend fun getSelectedGroups(): List<Int> {
        val selectedGroups = playbackProgressDao.getProgressOnce()?.selectedGroups ?: ""
        return if (selectedGroups.isBlank()) {
            emptyList()
        } else {
            selectedGroups.split(",").mapNotNull { it.toIntOrNull() }
        }
    }
    
    private val wordDao = database.wordDao()
    private val wordLibraryDao = database.wordLibraryDao()
    private val wordGroupDao = database.wordGroupDao()
    private val wordChapterDao = database.wordChapterDao()
    private val playbackProgressDao = database.playbackProgressDao()

    private val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    // 缓存集合
    private val pendingListenedWordIds = ConcurrentHashMap.newKeySet<Long>()
    private val pendingReviewedWordIds = ConcurrentHashMap.newKeySet<Long>()
    private val pendingListenedGroupIds = ConcurrentHashMap.newKeySet<Long>()
    private val pendingReviewedGroupIds = ConcurrentHashMap.newKeySet<Long>()

    init {
        // 定时 flush 每 5 分钟
        scope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(5 * 60 * 1000L)
                flushPendingUpdates()
            }
        }
    }

    suspend fun flushPendingUpdates() {
        if (pendingListenedWordIds.isNotEmpty()) {
            val ids = pendingListenedWordIds.toList()
            pendingListenedWordIds.clear()
            wordDao.updateWordsListened(ids)
        }
        if (pendingReviewedWordIds.isNotEmpty()) {
            val ids = pendingReviewedWordIds.toList()
            pendingReviewedWordIds.clear()
            wordDao.updateWordsReviewed(ids)
        }
        if (pendingListenedGroupIds.isNotEmpty()) {
            val ids = pendingListenedGroupIds.toList()
            pendingListenedGroupIds.clear()
            wordGroupDao.updateGroupsListened(ids)
        }
        if (pendingReviewedGroupIds.isNotEmpty()) {
            val ids = pendingReviewedGroupIds.toList()
            pendingReviewedGroupIds.clear()
            wordGroupDao.updateGroupsReviewed(ids)
        }
    }

    fun getPlaybackProgress(): Flow<PlaybackProgress?> = playbackProgressDao.getProgress()

    suspend fun initDefaultPlaybackProgress() {
        if (playbackProgressDao.getProgressOnce() == null) {
            playbackProgressDao.insertProgress(PlaybackProgress())
        }
    }

    suspend fun updateProgress(
        playbackMode: String? = null,
        playbackSpeed: Float? = null,
        playbackInterval: Float? = null,
        isRandom: Boolean? = null,
        isLoop: Boolean? = null,
        currentIndex: Int? = null
    ) {
        val currentProgress = playbackProgressDao.getProgressOnce() ?: PlaybackProgress()
        val newProgress = currentProgress.copy(
            playbackMode = playbackMode ?: currentProgress.playbackMode,
            playbackSpeed = playbackSpeed ?: currentProgress.playbackSpeed,
            playbackInterval = playbackInterval ?: currentProgress.playbackInterval,
            isRandom = isRandom ?: currentProgress.isRandom,
            isLoop = isLoop ?: currentProgress.isLoop,
            currentIndex = currentIndex ?: currentProgress.currentIndex
        )
        playbackProgressDao.updateProgress(newProgress)
    }
    
    /**
     * 获取所有词库
     */
    fun getAllLibraries(): Flow<List<WordLibrary>> {
        return wordLibraryDao.getAllLibraries()
    }
    
    /**
     * 获取激活的词库
     */
    suspend fun getActiveLibrary(): WordLibrary? {
        return wordLibraryDao.getActiveLibrary()
    }
    
    /**
     * 激活词库
     */
    suspend fun activateLibrary(libraryId: Long) {
        wordLibraryDao.activateLibrary(libraryId)
        playbackProgressDao.updateActiveLibrary(libraryId)

        // 检查是否有选中的组，如果没有则默认选择第一组
        val selectedGroups = getSelectedGroups()
        if (selectedGroups.isEmpty()) {
            // 获取该词库的所有组
            val groups = getGroupsForLibraryOnce(libraryId)
            if (groups.isNotEmpty()) {
                // 默认选择第一组
                val firstGroup = groups.minByOrNull { it.groupId } ?: groups[0]
                saveSelectedGroups(listOf(firstGroup.groupId))
                // 更新组的选择状态
                updateGroupsSelected(listOf(firstGroup.id), true)
            }
        }
    }
    
    /**
     * 删除词库及相关数据
     */
    suspend fun deleteLibrary(library: WordLibrary) {
        // 先删除词库中的单词和组
        wordDao.deleteAllWordsFromLibrary(library.id)
        wordGroupDao.deleteAllGroupsFromLibrary(library.id)
        
        // 最后删除词库
        wordLibraryDao.deleteLibrary(library)
        
        // 如果删除的是当前激活的词库，清除播放进度中的激活词库
        val progress = playbackProgressDao.getProgressOnce()
        if (progress != null && progress.activeLibraryId == library.id) {
            playbackProgressDao.updateActiveLibrary(-1)
        }
    }
    
    /**
     * 导入词库
     * 处理导入结果并保存到数据库
     */
    suspend fun importLibraryFromResult(importResult: FileImporter.ImportResult.Success): Long {
        // 导入词库: ${importResult.library.name}，共 ${importResult.words.size} 个单词
        // 1. 插入词库并获取ID
        val libraryId = wordLibraryDao.insertLibrary(importResult.library)
        
        // 2. 插入章节并获取ID
        val chaptersWithLibId = importResult.chapters.map { it.copy(libraryId = libraryId) }
        val chapterIds = wordChapterDao.insertChapters(chaptersWithLibId)

        // 3. 更新组的库ID和章节ID
        val groups = importResult.groups.map { group ->
            val chapterIndex = group.groupId / 10 //
            val chapterId = chapterIds.getOrElse(chapterIndex.toInt()) { -1L }
            if (chapterId == -1L) {
                // Log an error or handle case where chapter index is out of bounds
            }
            group.copy(libraryId = libraryId, chapterId = chapterId)
        }
        
        // 4. 更新单词的库ID
        val words = importResult.words.map { it.copy(libraryId = libraryId) }
        
        // 5. 插入单词和组
        wordDao.insertWords(words)
        wordGroupDao.insertGroups(groups)
        
        // 词库导入成功，ID: $libraryId
        return libraryId
    }

    /**
     * 导入内置词库
     */
    suspend fun importBuiltInLibraries() {
        val assetFiles = fileImporter.getAssetManager().list("") ?: emptyArray()
        
        for (fileName in assetFiles) {
            // 只处理支持的文件格式
            if (fileName.endsWith(".csv", ignoreCase = true) || 
                fileName.endsWith(".txt", ignoreCase = true)) {
                // 从文件名推断库名（去掉扩展名）
                val libraryName = fileName.substringBeforeLast(".")
                val result = fileImporter.importFromAssets(fileName, libraryName)
                if (result is FileImporter.ImportResult.Success) {
                    importLibraryFromResult(result)
                }
            }
        }
    }
    
    /**
     * 获取词库中的所有单词
     */
    fun getWordsFromLibrary(libraryId: Long): Flow<List<Word>> {
        return wordDao.getWordsFromLibrary(libraryId)
    }
    
    /**
     * 获取选定组中的单词
     */
    fun getWordsFromGroups(libraryId: Long, groupIds: List<Int>): Flow<List<Word>> {
        return wordDao.getWordsForGroups(libraryId, groupIds)
    }

    suspend fun getWordsFromGroupsOnce(libraryId: Long, groupIds: List<Int>): List<Word> {
        return wordDao.getWordsForGroupsOnce(libraryId, groupIds)
    }

    /**
     * 获取词库中的所有组
     */
    fun getGroupsForLibrary(libraryId: Long): Flow<List<WordGroup>> {
        return wordGroupDao.getGroupsForLibrary(libraryId)
    }

    suspend fun getGroupsForLibraryOnce(libraryId: Long): List<WordGroup> {
        return wordGroupDao.getGroupsForLibraryOnce(libraryId)
    }

    suspend fun getChaptersForLibraryOnce(libraryId: Long): List<com.yourcompany.worklisten.data.local.model.WordChapter> {
        return wordChapterDao.getChaptersForLibraryOnce(libraryId)
    }
    
    /**
     * 更新单词的已听过标记
     */
    suspend fun updateWordListened(wordId: Long, hasListened: Boolean) {
        if (hasListened) pendingListenedWordIds.add(wordId) else return
    }
 
    
    /**
     * 更新组的已听过标记
     */
    suspend fun updateGroupListened(groupId: Long, listened: Boolean) {
        if (listened) pendingListenedGroupIds.add(groupId) else return
    }
	
	/**
     * 更新组的已复习标记
     */
    suspend fun updateGroupReviewed(groupId: Long, reviewed: Boolean) {
        if (reviewed) pendingReviewedGroupIds.add(groupId) else return
    }
    
    
    /**
     * 增加组的播放计数
     */
    suspend fun incrementGroupPlayCount(groupId: Long) {
        wordGroupDao.incrementPlayCount(groupId)
    }
    
    /**
     * 增加组的学习次数并更新最后学习时间
     */
    suspend fun incrementGroupStudyCount(groupId: Long) {
        wordGroupDao.incrementStudyCount(groupId)
    }
    
    /**
     * 重置所有组的每日标记
     */
    suspend fun resetDailyFlags() {
        wordGroupDao.resetDailyFlags()
    }
    
    /**
     * 重置指定词库的所有组的每日标记
     */
    suspend fun resetDailyTagsForLibrary(libraryId: Long) {
        val groups = wordGroupDao.getGroupsForLibraryOnce(libraryId)
        val groupIds = groups.map { it.id }
        // 重置数据库中的标记
        wordGroupDao.resetDailyFlags()
        // 清除待处理的更新
        pendingListenedGroupIds.removeAll(groupIds.toSet())
        pendingReviewedGroupIds.removeAll(groupIds.toSet())
    }

    /**
     * 获取指定词库中所有被选中的组
     */
    suspend fun getSelectedGroupsForLibrary(libraryId: Long): List<WordGroup> {
        return wordGroupDao.getSelectedGroupsForLibrary(libraryId)
    }

    /**
     * 更新指定词库中所有组的选择状态
     */
    suspend fun updateAllGroupsSelected(libraryId: Long, isSelected: Boolean) {
        wordGroupDao.updateAllGroupsSelected(libraryId, isSelected)
    }

    /**
     * 更新指定组ID列表的选择状态
     */
    suspend fun updateGroupsSelected(groupIds: List<Long>, isSelected: Boolean) {
        wordGroupDao.updateGroupsSelected(groupIds, isSelected)
    }
    
    /**
     * 获取播放进度
     */
    suspend fun getPlaybackProgressOnce(): PlaybackProgress? {
        return playbackProgressDao.getProgressOnce()
    }
    
    /**
     * 更新指定组的选择状态
     */
    suspend fun updateSelectedGroups(selectedGroups: String) {
        playbackProgressDao.updateSelectedGroups(selectedGroups)
    }
    
    
    /**
     * 更新当前播放索引
     */
    suspend fun updateCurrentIndex(index: Int) {
        playbackProgressDao.updateCurrentIndex(index)
    }
    
    /**
     * 更新播放速度
     */
    suspend fun updatePlaybackSpeed(speed: Float) {
        playbackProgressDao.updatePlaybackSpeed(speed)
    }
    
    /**
     * 更新播放间隔
     */
    suspend fun updatePlaybackInterval(interval: Float) {
        playbackProgressDao.updatePlaybackInterval(interval)
    }
    
    /**
     * 更新随机播放模式
     */
    suspend fun updateRandomMode(isRandom: Boolean) {
        playbackProgressDao.updateRandomMode(isRandom)
    }
    
    /**
     * 更新循环播放模式
     */
    suspend fun updateLoopMode(isLoop: Boolean) {
        playbackProgressDao.updateLoopMode(isLoop)
    }
    
    /**
     * 更新播放模式
     */
    suspend fun updatePlaybackMode(mode: String) {
        playbackProgressDao.updatePlaybackMode(mode)
    }
    
    /**
     * 更新最后播放时间
     */
    suspend fun updateLastPlayedTime() {
        playbackProgressDao.updateLastPlayedTime()
    }
    
    /**
     * 更新单词重复次数
     */
    suspend fun updateWordRepeatCount(count: Int) {
		// 更新单词重复次数到数据库: $count
		playbackProgressDao.updateWordRepeatCount(count)
	}
    
    /**
     * 更新组的最后学习时间
     */
    suspend fun updateGroupLastStudyTime(groupId: Long) {
        wordGroupDao.updateLastStudyTime(groupId)
    }

    /**
     * 分页获取选定组中的单词
     */
    fun getPagedWordsFromGroups(libraryId: Long, groupIds: List<Int>): kotlinx.coroutines.flow.Flow<PagingData<Word>> {
        return Pager(
            config = PagingConfig(pageSize = 100, enablePlaceholders = false),
            pagingSourceFactory = { wordDao.pagingSourceWordsForGroups(libraryId, groupIds) }
        ).flow
    }

    /**
     * 分页获取词库中的所有单词
     */
    fun getPagedWordsFromLibrary(libraryId: Long): kotlinx.coroutines.flow.Flow<PagingData<Word>> {
        return Pager(
            config = PagingConfig(pageSize = 100, enablePlaceholders = false),
            pagingSourceFactory = { wordDao.pagingSourceWordsForLibrary(libraryId) }
        ).flow
    }
	
	/**
	 * 跨所有词库搜索单词
	 */
	suspend fun searchAllLibraries(query: String): List<Word> {
		return wordDao.searchAllLibraries(query) // 修复：移除多余的参数
	}
	
	
}