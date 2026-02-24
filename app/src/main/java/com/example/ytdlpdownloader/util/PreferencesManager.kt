package com.example.ytdlpdownloader.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.ytdlpdownloader.data.model.AppSettings
import com.example.ytdlpdownloader.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("app_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    private val keyDownloadPath = stringPreferencesKey(Constants.PREFS_DOWNLOAD_PATH)
    private val keyDefaultFormat = stringPreferencesKey(Constants.PREFS_DEFAULT_FORMAT)
    private val keyConcurrentFragments = intPreferencesKey(Constants.PREFS_CONCURRENT_FRAGMENTS)
    private val keySpeedLimit = longPreferencesKey(Constants.PREFS_SPEED_LIMIT)
    private val keyProxyUrl = stringPreferencesKey(Constants.PREFS_PROXY_URL)
    private val keyAria2c = booleanPreferencesKey(Constants.PREFS_ARIA2C_ENABLED)
    private val keySponsorBlock = booleanPreferencesKey(Constants.PREFS_SPONSORBLOCK_ENABLED)
    private val keySponsorBlockCats = stringPreferencesKey(Constants.PREFS_SPONSORBLOCK_CATEGORIES)
    private val keyEmbedThumb = booleanPreferencesKey(Constants.PREFS_EMBED_THUMBNAIL)
    private val keyEmbedMeta = booleanPreferencesKey(Constants.PREFS_EMBED_METADATA)
    private val keyEmbedSubs = booleanPreferencesKey(Constants.PREFS_EMBED_SUBTITLES)
    private val keySubLang = stringPreferencesKey(Constants.PREFS_SUBTITLE_LANG)
    private val keyThemeMode = stringPreferencesKey(Constants.PREFS_THEME_MODE)
    private val keyDynamicColor = booleanPreferencesKey(Constants.PREFS_DYNAMIC_COLOR)
    private val keyGeoBypass = booleanPreferencesKey(Constants.PREFS_GEO_BYPASS)
    private val keyCookiesFile = stringPreferencesKey(Constants.PREFS_COOKIES_FILE_PATH)

    val appSettings: Flow<AppSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppSettings(
                downloadPath = prefs[keyDownloadPath] ?: context.getDefaultDownloadDir().absolutePath,
                defaultFormat = prefs[keyDefaultFormat] ?: Constants.DEFAULT_FORMAT_VIDEO,
                concurrentFragments = prefs[keyConcurrentFragments] ?: Constants.DEFAULT_CONCURRENT_FRAGMENTS,
                speedLimitKbps = prefs[keySpeedLimit] ?: 0L,
                proxyUrl = prefs[keyProxyUrl] ?: "",
                useAria2c = prefs[keyAria2c] ?: false,
                sponsorBlockEnabled = prefs[keySponsorBlock] ?: false,
                sponsorBlockCategories = (prefs[keySponsorBlockCats] ?: "sponsor").split(","),
                embedThumbnail = prefs[keyEmbedThumb] ?: true,
                embedMetadata = prefs[keyEmbedMeta] ?: true,
                embedSubtitles = prefs[keyEmbedSubs] ?: false,
                subtitleLang = prefs[keySubLang] ?: "en",
                themeMode = ThemeMode.valueOf(prefs[keyThemeMode] ?: ThemeMode.SYSTEM.name),
                useDynamicColor = prefs[keyDynamicColor] ?: true,
                geoBypass = prefs[keyGeoBypass] ?: false,
                cookiesFilePath = prefs[keyCookiesFile] ?: ""
            )
        }

    suspend fun updateSetting(update: suspend (MutablePreferences) -> Unit) {
        dataStore.edit { update(it) }
    }

    suspend fun setDownloadPath(path: String) = updateSetting { it[keyDownloadPath] = path }
    suspend fun setDefaultFormat(format: String) = updateSetting { it[keyDefaultFormat] = format }
    suspend fun setConcurrentFragments(n: Int) = updateSetting { it[keyConcurrentFragments] = n }
    suspend fun setSpeedLimit(kbps: Long) = updateSetting { it[keySpeedLimit] = kbps }
    suspend fun setProxyUrl(url: String) = updateSetting { it[keyProxyUrl] = url }
    suspend fun setAria2c(enabled: Boolean) = updateSetting { it[keyAria2c] = enabled }
    suspend fun setSponsorBlock(enabled: Boolean) = updateSetting { it[keySponsorBlock] = enabled }
    suspend fun setSponsorBlockCategories(cats: List<String>) =
        updateSetting { it[keySponsorBlockCats] = cats.joinToString(",") }
    suspend fun setEmbedThumbnail(v: Boolean) = updateSetting { it[keyEmbedThumb] = v }
    suspend fun setEmbedMetadata(v: Boolean) = updateSetting { it[keyEmbedMeta] = v }
    suspend fun setEmbedSubtitles(v: Boolean) = updateSetting { it[keyEmbedSubs] = v }
    suspend fun setSubtitleLang(lang: String) = updateSetting { it[keySubLang] = lang }
    suspend fun setThemeMode(mode: ThemeMode) = updateSetting { it[keyThemeMode] = mode.name }
    suspend fun setDynamicColor(v: Boolean) = updateSetting { it[keyDynamicColor] = v }
    suspend fun setGeoBypass(v: Boolean) = updateSetting { it[keyGeoBypass] = v }
    suspend fun setCookiesFile(path: String) = updateSetting { it[keyCookiesFile] = path }
}
