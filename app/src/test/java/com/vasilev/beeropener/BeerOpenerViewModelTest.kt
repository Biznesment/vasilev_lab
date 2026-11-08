package com.vasilev.beeropener

import com.vasilev.beeropener.model.BeerSort
import com.vasilev.beeropener.ui.BeerOpenerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeerOpenerViewModelTest {

    @Test
    fun initialState_isClosedWithZeroCount() {
        val viewModel = BeerOpenerViewModel()
        val state = viewModel.uiState.value

        assertEquals(BeerSort.LAGER, state.selectedSort)
        assertFalse(state.isOpened)
        assertEquals(0, state.openedCount)
        assertEquals(0f, state.pullProgress, 0.001f)
    }

    @Test
    fun openBottle_incrementsCounterOncePerBottle() {
        val viewModel = BeerOpenerViewModel()

        viewModel.openBottle()
        assertTrue(viewModel.uiState.value.isOpened)
        assertEquals(1, viewModel.uiState.value.openedCount)

        // Повторный вызов на уже открытой бутылке не должен увеличивать счётчик
        viewModel.openBottle()
        assertEquals(1, viewModel.uiState.value.openedCount)
    }

    @Test
    fun partialSwipeAndRelease_resetsProgressWithoutOpening() {
        val viewModel = BeerOpenerViewModel()

        viewModel.onPullCap(0.45f)
        assertFalse(viewModel.uiState.value.isOpened)
        assertEquals(0.45f, viewModel.uiState.value.pullProgress, 0.001f)

        viewModel.onPullRelease()
        assertFalse(viewModel.uiState.value.isOpened)
        assertEquals(0f, viewModel.uiState.value.pullProgress, 0.001f)
    }

    @Test
    fun fullSwipe_opensBottleAutomatically() {
        val viewModel = BeerOpenerViewModel()

        viewModel.onPullCap(0.6f)
        viewModel.onPullCap(0.5f)

        assertTrue(viewModel.uiState.value.isOpened)
        assertEquals(1, viewModel.uiState.value.openedCount)
    }

    @Test
    fun nextBottle_keepsCountAndResetClearsCount() {
        val viewModel = BeerOpenerViewModel()

        viewModel.openBottle()
        viewModel.nextBottle()
        assertFalse(viewModel.uiState.value.isOpened)
        assertEquals(1, viewModel.uiState.value.openedCount)

        viewModel.openBottle()
        assertEquals(2, viewModel.uiState.value.openedCount)

        viewModel.resetCounter()
        assertFalse(viewModel.uiState.value.isOpened)
        assertEquals(0, viewModel.uiState.value.openedCount)
    }

    @Test
    fun selectSort_changesSortAndPutsClosedBottle() {
        val viewModel = BeerOpenerViewModel()
        viewModel.openBottle()

        viewModel.selectSort(BeerSort.STOUT)

        assertEquals(BeerSort.STOUT, viewModel.uiState.value.selectedSort)
        assertFalse(viewModel.uiState.value.isOpened)
        assertEquals(1, viewModel.uiState.value.openedCount)
    }
}
