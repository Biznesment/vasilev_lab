package com.vasilev.beeropener.ui

import androidx.lifecycle.ViewModel
import com.vasilev.beeropener.model.BeerSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Состояние главного экрана приложения.
 */
data class BeerUiState(
    val selectedSort: BeerSort = BeerSort.LAGER,
    val isOpened: Boolean = false,
    val pullProgress: Float = 0f,
    val openedCount: Int = 0,
    val soundEnabled: Boolean = true,
    val openEventId: Int = 0
)

/**
 * ViewModel для хранения состояния открывашки (сохраняется при повороте экрана).
 */
class BeerOpenerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BeerUiState())
    val uiState: StateFlow<BeerUiState> = _uiState.asStateFlow()

    /**
     * Прогресс свайпа вверх по крышке (delta > 0 при движении пальца вверх).
     * При достижении 1.0 бутылка открывается автоматически.
     */
    fun onPullCap(deltaProgress: Float) {
        val current = _uiState.value
        if (current.isOpened) return

        val updatedProgress = (current.pullProgress + deltaProgress).coerceIn(0f, 1f)
        if (updatedProgress >= 1f) {
            openBottle()
        } else {
            _uiState.update { it.copy(pullProgress = updatedProgress) }
        }
    }

    /**
     * Если пользователь отпустил крышку раньше открытия — возвращаем её на место.
     */
    fun onPullRelease() {
        _uiState.update { state ->
            if (state.isOpened) state else state.copy(pullProgress = 0f)
        }
    }

    /**
     * Открывает текущую бутылку, запускает анимацию пены и увеличивает счётчик.
     */
    fun openBottle() {
        _uiState.update { state ->
            if (state.isOpened) {
                state
            } else {
                state.copy(
                    isOpened = true,
                    pullProgress = 1f,
                    openedCount = state.openedCount + 1,
                    openEventId = state.openEventId + 1
                )
            }
        }
    }

    /**
     * Ставит новую закрытую бутылку, сохраняя общий счётчик открытых бутылок.
     */
    fun nextBottle() {
        _uiState.update { state ->
            state.copy(
                isOpened = false,
                pullProgress = 0f
            )
        }
    }

    /**
     * Смена сорта напитка (ставит свежую закрытую бутылку выбранного сорта).
     */
    fun selectSort(sort: BeerSort) {
        _uiState.update { state ->
            state.copy(
                selectedSort = sort,
                isOpened = false,
                pullProgress = 0f
            )
        }
    }

    /**
     * Включение / выключение звука открытия.
     */
    fun toggleSound() {
        _uiState.update { it.copy(soundEnabled = !it.soundEnabled) }
    }

    /**
     * Полный сброс счётчика открытых бутылок.
     */
    fun resetCounter() {
        _uiState.update { state ->
            state.copy(
                isOpened = false,
                pullProgress = 0f,
                openedCount = 0
            )
        }
    }
}
