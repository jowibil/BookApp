package com.bookappp.app.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookappp.app.data.repository.SettingsRepository
import com.bookappp.app.domain.model.ReaderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds reader appearance settings for both the sheet UI and the navigator bridge.
 * Writes go through SettingsRepository (DataStore) and are clamped there.
 */
@HiltViewModel
class ReaderSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<ReaderSettings> = settingsRepository.settings
        .catch { emit(ReaderSettings.Default) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ReaderSettings.Default,
        )

    fun update(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
        }
    }
}
