@file:Suppress("FunctionNaming")

package com.ian.stockpick.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.stockpick.screen.EditorUiState
import com.ian.stockpick.screen.EditorViewModel
import com.ian.stockpick.ui.components.FilterSummaryCard
import com.ian.stockpick.ui.components.ResultStockCard
import com.ian.stockpick.ui.components.StatusCards
import com.ian.stockpick.ui.theme.AccentBlue
import com.ian.stockpick.ui.theme.BgPrimary
import com.ian.stockpick.ui.theme.TextPrimary
import com.ian.stockpick.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    EditorScreenEffects(state, viewModel, snackbarHostState, context)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { EditorTopBar(onRefresh = { viewModel.refreshCache() }) },
    ) { innerPadding ->
        EditorScreenContent(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onRun = { viewModel.runScreen() },
            onCancel = { viewModel.cancel() },
        )
    }
}

@Composable
private fun EditorScreenEffects(
    state: EditorUiState,
    viewModel: EditorViewModel,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context,
) {
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.skippedCount, state.isRunning) {
        if (!state.isRunning && state.skippedCount > 0 && state.dataAsOf != null) {
            Toast.makeText(context, "略过 ${state.skippedCount} 只", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(onRefresh: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "StockPick",
                color = AccentBlue,
                fontWeight = FontWeight.Bold,
            )
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "刷新缓存",
                    tint = TextPrimary,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary),
    )
}

@Composable
private fun EditorScreenContent(
    state: EditorUiState,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.offHoursWarning) {
            item {
                Text(
                    text = "当前非交易时段，数据可能非实时",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.progressMessage.isNotBlank()) {
            item {
                RowWithCancel(message = state.progressMessage, onCancel = onCancel)
            }
        }
        item {
            FilterSummaryCard(isRunning = state.isRunning, onRun = onRun)
        }
        item {
            StatusCards(
                lastRunTime = state.lastRunTime,
                hitCount = state.hitCount,
                dataAsOf = state.dataAsOf,
            )
        }
        item {
            Text(
                text = "结果列表",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        if (state.results.isEmpty() && !state.isRunning && state.dataAsOf != null) {
            item {
                Text(
                    text = "当前无符合条件的股票",
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(state.results, key = { it.stock.code }) { result ->
            ResultStockCard(result = result)
        }
    }
}

@Composable
private fun RowWithCancel(
    message: String,
    onCancel: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = message, color = TextSecondary, fontSize = 13.sp)
        TextButton(onClick = onCancel) {
            Text(text = "取消", color = AccentBlue, fontSize = 13.sp)
        }
    }
}
