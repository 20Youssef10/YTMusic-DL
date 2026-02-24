package com.example.ytdlpdownloader

import app.cash.turbine.test
import com.example.ytdlpdownloader.data.model.*
import com.example.ytdlpdownloader.domain.usecase.FetchVideoInfoUseCase
import com.example.ytdlpdownloader.domain.usecase.StartDownloadUseCase
import com.example.ytdlpdownloader.ui.home.FetchState
import com.example.ytdlpdownloader.ui.home.HomeViewModel
import com.example.ytdlpdownloader.util.PreferencesManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var fetchVideoInfo: FetchVideoInfoUseCase
    private lateinit var startDownload: StartDownloadUseCase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var viewModel: HomeViewModel

    private val mockVideoInfo = VideoInfo(
        id = "test_id",
        title = "Test Video",
        uploader = "Test Uploader",
        uploaderUrl = null,
        duration = 120,
        viewCount = 1000,
        likeCount = null,
        thumbnail = "https://example.com/thumb.jpg",
        description = "A test video",
        webpage_url = "https://youtube.com/watch?v=test",
        extractor = "youtube",
        uploadDate = "20240101",
        chapters = null,
        formats = listOf(
            VideoFormat(
                formatId = "137",
                ext = "mp4",
                resolution = "1920x1080",
                width = 1920,
                height = 1080,
                fps = 30.0,
                vcodec = "avc1",
                acodec = "none",
                abr = null,
                vbr = 5000.0,
                tbr = 5000.0,
                filesize = 150_000_000,
                filesizeApprox = null,
                dynamicRange = "SDR",
                formatNote = "1080p",
                isVideoOnly = true
            )
        )
    )

    @Before
    fun setup() {
        fetchVideoInfo = mockk()
        startDownload = mockk(relaxed = true)
        preferencesManager = mockk {
            every { appSettings } returns flowOf(AppSettings())
        }

        viewModel = HomeViewModel(fetchVideoInfo, startDownload, preferencesManager)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `initial state is idle with empty URL`() = runTest(dispatcher) {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.urlInput)
            assertTrue(state.fetchState is FetchState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onUrlChanged updates urlInput`() = runTest(dispatcher) {
        viewModel.onUrlChanged("https://youtube.com/watch?v=abc")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("https://youtube.com/watch?v=abc", state.urlInput)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fetchInfo with invalid URL sets error state`() = runTest(dispatcher) {
        viewModel.onUrlChanged("not a url")
        viewModel.fetchInfo("not a url")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.fetchState is FetchState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fetchInfo success sets Success state with VideoInfo`() = runTest(dispatcher) {
        coEvery { fetchVideoInfo(any(), any(), any()) } returns Result.success(mockVideoInfo)

        viewModel.onUrlChanged("https://youtube.com/watch?v=test")
        viewModel.fetchInfo("https://youtube.com/watch?v=test")

        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            val fetchState = state.fetchState
            assertTrue(fetchState is FetchState.Success)
            assertEquals("Test Video", (fetchState as FetchState.Success).info.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fetchInfo failure sets Error state`() = runTest(dispatcher) {
        coEvery { fetchVideoInfo(any(), any(), any()) } returns
            Result.failure(Exception("Network error"))

        viewModel.onUrlChanged("https://youtube.com/watch?v=test")
        viewModel.fetchInfo("https://youtube.com/watch?v=test")

        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            val fetchState = state.fetchState
            assertTrue(fetchState is FetchState.Error)
            assertTrue((fetchState as FetchState.Error).message.contains("Network error"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearInput resets URL and fetch state`() = runTest(dispatcher) {
        viewModel.onUrlChanged("https://example.com")
        viewModel.clearInput()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.urlInput)
            assertTrue(state.fetchState is FetchState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setAudioOnly updates isAudioOnly flag`() = runTest(dispatcher) {
        viewModel.setAudioOnly(true)

        viewModel.uiState.test {
            assertTrue(awaitItem().isAudioOnly)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
