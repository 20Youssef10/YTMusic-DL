package com.example.ytdlpdownloader.util

object Constants {
    // Notification channels
    const val NOTIFICATION_CHANNEL_DOWNLOAD = "channel_download"
    const val NOTIFICATION_CHANNEL_COMPLETE = "channel_complete"
    const val NOTIFICATION_CHANNEL_ERROR = "channel_error"

    // Notification IDs
    const val NOTIFICATION_ID_FOREGROUND = 1001
    const val NOTIFICATION_ID_COMPLETE_BASE = 2000
    const val NOTIFICATION_ID_ERROR_BASE = 3000

    // WorkManager tags
    const val WORK_TAG_DOWNLOAD = "download_work"
    const val WORK_NAME_DOWNLOAD_PREFIX = "download_"

    // Download service actions
    const val ACTION_PAUSE_DOWNLOAD = "action_pause"
    const val ACTION_RESUME_DOWNLOAD = "action_resume"
    const val ACTION_CANCEL_DOWNLOAD = "action_cancel"
    const val ACTION_STOP_SERVICE = "action_stop_service"
    const val EXTRA_DOWNLOAD_ID = "extra_download_id"

    // Default download options
    const val DEFAULT_FORMAT_VIDEO = "bestvideo+bestaudio/best"
    const val DEFAULT_FORMAT_AUDIO_MP3 = "bestaudio/best"
    const val DEFAULT_CONCURRENT_FRAGMENTS = 4
    const val DEFAULT_RETRY_COUNT = 3
    const val DEFAULT_SPEED_LIMIT_KBPS = 0L // 0 = unlimited

    // File extensions
    val VIDEO_EXTENSIONS = listOf("mp4", "mkv", "webm", "avi", "mov", "flv", "ts")
    val AUDIO_EXTENSIONS = listOf("mp3", "m4a", "opus", "flac", "aac", "alac", "wav")

    // GitHub yt-dlp releases URL
    const val YTDLP_RELEASES_URL = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"

    // SponsorBlock categories
    val SPONSORBLOCK_CATEGORIES = listOf("sponsor", "intro", "outro", "selfpromo", "interaction", "preview")

    // DataStore keys
    const val PREFS_DOWNLOAD_PATH = "download_path"
    const val PREFS_DEFAULT_FORMAT = "default_format"
    const val PREFS_CONCURRENT_FRAGMENTS = "concurrent_fragments"
    const val PREFS_SPEED_LIMIT = "speed_limit"
    const val PREFS_PROXY_URL = "proxy_url"
    const val PREFS_ARIA2C_ENABLED = "aria2c_enabled"
    const val PREFS_SPONSORBLOCK_ENABLED = "sponsorblock_enabled"
    const val PREFS_SPONSORBLOCK_CATEGORIES = "sponsorblock_categories"
    const val PREFS_EMBED_THUMBNAIL = "embed_thumbnail"
    const val PREFS_EMBED_METADATA = "embed_metadata"
    const val PREFS_EMBED_SUBTITLES = "embed_subtitles"
    const val PREFS_SUBTITLE_LANG = "subtitle_lang"
    const val PREFS_THEME_MODE = "theme_mode"
    const val PREFS_DYNAMIC_COLOR = "dynamic_color"
    const val PREFS_GEO_BYPASS = "geo_bypass"
    const val PREFS_COOKIES_FILE_PATH = "cookies_file_path"

    // Max concurrent downloads
    const val MAX_CONCURRENT_DOWNLOADS = 3
}
