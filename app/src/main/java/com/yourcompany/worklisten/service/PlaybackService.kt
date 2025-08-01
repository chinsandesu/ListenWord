package com.yourcompany.worklisten.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import javax.inject.Provider
import androidx.core.app.NotificationCompat
import com.yourcompany.worklisten.R
import com.yourcompany.worklisten.data.local.AppDatabase
import com.yourcompany.worklisten.data.repository.SettingsRepository
import com.yourcompany.worklisten.data.repository.WordRepository
import com.yourcompany.worklisten.utils.FileImporter
import com.yourcompany.worklisten.utils.TtsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var ttsHelper: TtsHelper
    private lateinit var wordRepository: WordRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var fileImporter: FileImporter

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "WorkListenPlaybackChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        // 初始化依赖
        settingsRepository = SettingsRepository(this)
        ttsHelper = TtsHelper(this, settingsRepository)
        fileImporter = FileImporter(this)
        // 使用延迟初始化的Provider解决循环依赖
        val repositoryProvider = object : Provider<WordRepository> {
            private var repository: WordRepository? = null

            fun setRepository(repo: WordRepository) {
                repository = repo
            }

            override fun get(): WordRepository {
                return repository ?: throw IllegalStateException("WordRepository not initialized yet")
            }
        }

        // 初始化数据库
        val database = AppDatabase.getDatabase(this, serviceScope, repositoryProvider)
        // 初始化仓库
        wordRepository = WordRepository(database, fileImporter)
        // 设置仓库到Provider
        repositoryProvider.setRepository(wordRepository)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 这里可以添加后台播放逻辑，例如启动TTS播放
        // serviceScope.launch {
        //     // 示例：如果需要，可以在这里启动播放逻辑
        // }

        return START_STICKY // 服务被杀死后会尝试重启
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // 不支持绑定
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // 取消所有协程
        ttsHelper.shutdown() // 关闭TTS引擎
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "WorkListen Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("WorkListen 正在后台运行")
            .setContentText("点击返回应用")
            .setSmallIcon(R.drawable.app_icon) // 使用你的应用图标
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
