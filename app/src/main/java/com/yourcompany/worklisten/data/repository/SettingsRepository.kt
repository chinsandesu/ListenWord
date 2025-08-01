package com.yourcompany.worklisten.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
	private val prefs: SharedPreferences by lazy {
		context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
	}

    private object PreferencesKeys {
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val REPEAT_COUNT = intPreferencesKey("repeat_count")
    }

    val backgroundImageUri: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BACKGROUND_IMAGE_URI]
        }

    suspend fun saveBackgroundImageUri(uri: String?) {
        context.dataStore.edit { settings ->
            if (uri != null) {
                settings[PreferencesKeys.BACKGROUND_IMAGE_URI] = uri
            } else {
                settings.remove(PreferencesKeys.BACKGROUND_IMAGE_URI)
            }
        }
    }
	// 背景图片相关
	private val _backgroundFileName = MutableStateFlow<String?>(null)
	val backgroundFileName: Flow<String?> = _backgroundFileName.asStateFlow()
	
	init {
		_backgroundFileName.value = prefs.getString("background_file", null)
	}
	
	fun saveBackgroundFileName(fileName: String) {
		prefs.edit().putString("background_file", fileName).apply()
		_backgroundFileName.value = fileName
	}
	
	fun getBackgroundFileName(): String? {
		return prefs.getString("background_file", null)
	}
	
	suspend fun clearBackgroundSetting() {
		prefs.edit().remove("background_file").apply()
		_backgroundFileName.value = null
		saveBackgroundImageUri(null)
	}

    // 单词重复朗读次数相关
    val DEFAULT_REPEAT_COUNT = 1 // 默认朗读次数为1

    val repeatCount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.REPEAT_COUNT] ?: DEFAULT_REPEAT_COUNT
        }

    suspend fun saveRepeatCount(count: Int) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.REPEAT_COUNT] = count
        }
    }
}
