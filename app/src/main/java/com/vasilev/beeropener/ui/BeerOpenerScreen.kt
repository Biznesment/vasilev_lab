package com.vasilev.beeropener.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vasilev.beeropener.model.BeerSort
import com.vasilev.beeropener.sound.PopSoundPlayer
import com.vasilev.beeropener.ui.components.BeerBottleView
import com.vasilev.beeropener.ui.theme.BeerOpenerTheme

@Composable
fun BeerOpenerRoute(
    viewModel: BeerOpenerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var lastHandledEventId by rememberSaveable { mutableIntStateOf(uiState.openEventId) }

    LaunchedEffect(uiState.openEventId) {
        if (uiState.openEventId != lastHandledEventId) {
            lastHandledEventId = uiState.openEventId
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (uiState.soundEnabled) {
                PopSoundPlayer.playOpenSound()
            }
        }
    }

    BeerOpenerScreen(
        uiState = uiState,
        onSelectSort = viewModel::selectSort,
        onPullCap = viewModel::onPullCap,
        onPullRelease = viewModel::onPullRelease,
        onOpenClick = viewModel::openBottle,
        onNextBottleClick = viewModel::nextBottle,
        onToggleSound = viewModel::toggleSound,
        onResetCounter = viewModel::resetCounter
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeerOpenerScreen(
    uiState: BeerUiState,
    onSelectSort: (BeerSort) -> Unit,
    onPullCap: (Float) -> Unit,
    onPullRelease: () -> Unit,
    onOpenClick: () -> Unit,
    onNextBottleClick: () -> Unit,
    onToggleSound: () -> Unit,
    onResetCounter: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Открывашка",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onToggleSound) {
                        Text(text = if (uiState.soundEnabled) "Звук: ВКЛ" else "Звук: ВЫКЛ")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Карточка счётчика открытых бутылок
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Открыто бутылок",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${uiState.openedCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(
                        onClick = onResetCounter,
                        enabled = uiState.openedCount > 0 || uiState.isOpened
                    ) {
                        Text("Сбросить")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Выбор сорта напитка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                BeerSort.entries.forEach { sort ->
                    FilterChip(
                        selected = uiState.selectedSort == sort,
                        onClick = { onSelectSort(sort) },
                        label = { Text(sort.title) }
                    )
                }
            }

            Text(
                text = uiState.selectedSort.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Интерактивная бутылка (свайп вверх по крышке)
            BeerBottleView(
                sort = uiState.selectedSort,
                isOpened = uiState.isOpened,
                pullProgress = uiState.pullProgress,
                onPullCap = onPullCap,
                onPullRelease = onPullRelease,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )

            // Индикатор усилия при свайпе и подсказка
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!uiState.isOpened && uiState.pullProgress > 0.02f) {
                    LinearProgressIndicator(
                        progress = { uiState.pullProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = if (uiState.isOpened) {
                        "Пшшш! Пиво открыто!"
                    } else {
                        "Потяните крышку вверх свайпом или нажмите кнопку"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = if (uiState.isOpened) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (uiState.isOpened) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!uiState.isOpened) {
                    Button(
                        onClick = onOpenClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Открыть пиво",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = onNextBottleClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Следующая бутылка",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BeerOpenerScreenPreview() {
    BeerOpenerTheme {
        BeerOpenerScreen(
            uiState = BeerUiState(openedCount = 3),
            onSelectSort = {},
            onPullCap = {},
            onPullRelease = {},
            onOpenClick = {},
            onNextBottleClick = {},
            onToggleSound = {},
            onResetCounter = {}
        )
    }
}
