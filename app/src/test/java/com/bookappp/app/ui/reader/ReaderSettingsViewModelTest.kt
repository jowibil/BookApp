package com.bookappp.app.ui.reader

import com.bookappp.app.data.repository.SettingsRepository
import com.bookappp.app.domain.model.ReaderSettings
import com.bookappp.app.domain.model.ReaderTheme
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settingsFlow = MutableStateFlow(ReaderSettings.Default)
    private val repository = mockk<SettingsRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.settings } returns settingsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stored settings are exposed as state`() = runTest {
        val viewModel = ReaderSettingsViewModel(repository)

        settingsFlow.value = ReaderSettings(theme = ReaderTheme.NIGHT, fontSizeScale = 1.5f)
        advanceUntilIdle()

        assertEquals(ReaderTheme.NIGHT, viewModel.settings.value.theme)
        assertEquals(1.5f, viewModel.settings.value.fontSizeScale, 0.001f)
    }

    @Test
    fun `update forwards the transform to the repository`() = runTest {
        val transform = slot<(ReaderSettings) -> ReaderSettings>()
        coEvery { repository.update(capture(transform)) } just Runs
        val viewModel = ReaderSettingsViewModel(repository)

        viewModel.update { it.copy(brightnessOverride = 0.4f) }
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.update(any()) }
        assertEquals(
            0.4f,
            transform.captured(ReaderSettings.Default).brightnessOverride!!,
            0.001f,
        )
    }

    @Test
    fun `unreadable settings fall back to defaults`() = runTest {
        every { repository.settings } returns flow { throw IOException("corrupt") }

        val viewModel = ReaderSettingsViewModel(repository)
        advanceUntilIdle()

        assertEquals(ReaderSettings.Default, viewModel.settings.value)
    }
}
