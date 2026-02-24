package com.example.ytdlpdownloader

import com.example.ytdlpdownloader.data.model.*
import com.example.ytdlpdownloader.data.repository.DownloadRepository
import com.example.ytdlpdownloader.domain.usecase.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UseCaseTests {

    private lateinit var repository: DownloadRepository
    private val testConfig = DownloadConfig(
        url = "https://youtube.com/watch?v=test",
        outputPath = "/storage/downloads"
    )
    private val testVideoInfo = VideoInfo(
        id = "test_id",
        title = "Test Video",
        uploader = "Uploader",
        uploaderUrl = null,
        duration = 60,
        viewCount = 100,
        likeCount = null,
        thumbnail = null,
        description = null,
        webpage_url = "https://youtube.com/watch?v=test",
        extractor = "youtube",
        uploadDate = null,
        chapters = null,
        formats = emptyList()
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
    }

    // ── StartDownloadUseCase ──────────────────────────────────────────────────

    @Test
    fun `StartDownloadUseCase creates item with QUEUED status`() = runTest {
        val useCase = StartDownloadUseCase(repository)
        val item = useCase(testVideoInfo, testConfig)

        assertEquals(DownloadStatus.QUEUED, item.status)
        assertEquals(testVideoInfo.title, item.title)
        assertEquals(testVideoInfo.webpage_url, item.url)
        coVerify { repository.addDownload(item) }
    }

    @Test
    fun `StartDownloadUseCase with scheduled flag creates SCHEDULED item`() = runTest {
        val useCase = StartDownloadUseCase(repository)
        val item = useCase(testVideoInfo, testConfig, isScheduled = true)

        assertEquals(DownloadStatus.SCHEDULED, item.status)
    }

    @Test
    fun `StartDownloadUseCase by URL creates item with placeholder title`() = runTest {
        val useCase = StartDownloadUseCase(repository)
        val item = useCase("https://youtube.com/watch?v=test", testConfig)

        assertEquals("Fetching...", item.title)
        assertEquals(DownloadStatus.QUEUED, item.status)
    }

    // ── CancelDownloadUseCase ─────────────────────────────────────────────────

    @Test
    fun `CancelDownloadUseCase cancels process and updates status`() = runTest {
        val useCase = CancelDownloadUseCase(repository)
        useCase("download_id_1", "process_id_1")

        verify { repository.cancelDownload("process_id_1") }
        coVerify { repository.updateStatus("download_id_1", DownloadStatus.CANCELLED) }
    }

    @Test
    fun `CancelDownloadUseCase works without processId`() = runTest {
        val useCase = CancelDownloadUseCase(repository)
        useCase("download_id_1", null)

        verify(exactly = 0) { repository.cancelDownload(any()) }
        coVerify { repository.updateStatus("download_id_1", DownloadStatus.CANCELLED) }
    }

    // ── ReDownloadUseCase ─────────────────────────────────────────────────────

    @Test
    fun `ReDownloadUseCase creates new item with fresh state`() = runTest {
        val useCase = ReDownloadUseCase(repository)
        val originalItem = DownloadItem(
            id = "original_id",
            url = "https://youtube.com/watch?v=test",
            title = "Test",
            thumbnail = null,
            duration = null,
            uploader = null,
            config = testConfig,
            status = DownloadStatus.COMPLETED,
            progress = 1f,
            filePath = "/some/file.mp4",
            completedAt = 12345L
        )

        val newItem = useCase(originalItem)

        assertNotEquals("original_id", newItem.id)
        assertEquals(DownloadStatus.QUEUED, newItem.status)
        assertEquals(0f, newItem.progress, 0.001f)
        assertNull(newItem.filePath)
        assertNull(newItem.completedAt)
        assertEquals(originalItem.url, newItem.url)
        assertEquals(originalItem.config, newItem.config)
        coVerify { repository.addDownload(newItem) }
    }

    // ── FetchVideoInfoUseCase ─────────────────────────────────────────────────

    @Test
    fun `FetchVideoInfoUseCase calls fetchVideoInfo for regular URL`() = runTest {
        coEvery { repository.fetchVideoInfo(any(), any()) } returns Result.success(testVideoInfo)

        val useCase = FetchVideoInfoUseCase(repository)
        val result = useCase("https://youtube.com/watch?v=test")

        assertTrue(result.isSuccess)
        coVerify { repository.fetchVideoInfo("https://youtube.com/watch?v=test", null) }
    }

    @Test
    fun `FetchVideoInfoUseCase calls fetchPlaylistInfo for playlist URL`() = runTest {
        coEvery { repository.fetchPlaylistInfo(any(), any()) } returns Result.success(testVideoInfo)

        val useCase = FetchVideoInfoUseCase(repository)
        val result = useCase("https://youtube.com/playlist?list=PLtest123", forcePlaylist = false)

        // Should detect playlist URL automatically
        coVerify { repository.fetchPlaylistInfo("https://youtube.com/playlist?list=PLtest123", null) }
    }
}
