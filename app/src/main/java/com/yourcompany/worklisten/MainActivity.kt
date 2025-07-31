package com.yourcompany.worklisten

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yourcompany.worklisten.data.local.AppDatabase
import com.yourcompany.worklisten.data.repository.BackgroundRepository
import com.yourcompany.worklisten.data.repository.SettingsRepository
import com.yourcompany.worklisten.data.repository.WordRepository
import com.yourcompany.worklisten.ui.theme.WorkListenTheme
import com.yourcompany.worklisten.utils.FileImporter
import com.yourcompany.worklisten.utils.TtsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Provider
import kotlinx.coroutines.launch
import android.content.Intent
import android.os.Build
import com.yourcompany.worklisten.service.PlaybackService

class MainActivity : ComponentActivity() {

    private lateinit var ttsHelper: TtsHelper
    private lateinit var wordRepository: WordRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var fileImporter: FileImporter
    private lateinit var backgroundRepository: BackgroundRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化工具类
        fileImporter = FileImporter(this)
        settingsRepository = SettingsRepository(this)
        ttsHelper = TtsHelper(this, settingsRepository)
        ttsHelper.warmUp()
        backgroundRepository = BackgroundRepository(this)

        // 启动后台播放服务
        val serviceIntent = Intent(this, PlaybackService::class.java)
	    startForegroundService(serviceIntent)
	    
	    // 创建协程作用域
        val applicationScope = CoroutineScope(Dispatchers.IO)

        // 使用Provider解决循环依赖
        val repositoryProvider = Provider { wordRepository }

        // 初始化数据库
        val database = AppDatabase.getDatabase(this, applicationScope, repositoryProvider)

        // 初始化仓库
        wordRepository = WordRepository(database, fileImporter)

        setContent {
            WorkListenTheme {
                WorkListenApp(
                repository = wordRepository,
                settingsRepository = settingsRepository,
                fileImporter = fileImporter,
                ttsHelper = ttsHelper,
                backgroundRepository = backgroundRepository
            )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 应用退到后台时批量写入待处理进度
        if (::wordRepository.isInitialized) {
            CoroutineScope(Dispatchers.IO).launch {
                wordRepository.flushPendingUpdates()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 确保在应用销毁时关闭TTS
        if (::ttsHelper.isInitialized) {
            ttsHelper.shutdown()
        }
    }
}
