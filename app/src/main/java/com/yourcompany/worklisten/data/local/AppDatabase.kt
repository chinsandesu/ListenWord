package com.yourcompany.worklisten.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yourcompany.worklisten.data.local.dao.PlaybackProgressDao
import com.yourcompany.worklisten.data.local.dao.WordDao
import com.yourcompany.worklisten.data.local.dao.WordGroupDao
import com.yourcompany.worklisten.data.local.dao.WordLibraryDao
import com.yourcompany.worklisten.data.local.dao.WordChapterDao
import com.yourcompany.worklisten.data.local.model.PlaybackProgress
import com.yourcompany.worklisten.data.local.model.Word
import com.yourcompany.worklisten.data.local.model.WordChapter
import com.yourcompany.worklisten.data.local.model.WordGroup
import com.yourcompany.worklisten.data.local.model.WordLibrary
import com.yourcompany.worklisten.data.repository.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Provider

@Database(
    entities = [WordLibrary::class, Word::class, WordGroup::class, WordChapter::class, PlaybackProgress::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun wordLibraryDao(): WordLibraryDao
    abstract fun wordGroupDao(): WordGroupDao
    abstract fun wordChapterDao(): WordChapterDao
    abstract fun playbackProgressDao(): PlaybackProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope,
            wordRepositoryProvider: Provider<WordRepository>
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app.db"
                )
                .addCallback(DatabaseCallback(context, scope, wordRepositoryProvider))
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
    
    private class DatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope,
        private val wordRepositoryProvider: Provider<WordRepository>
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch {
                wordRepositoryProvider.get().importBuiltInLibraries()
            }
        }
    }
}