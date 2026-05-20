package com.ian.stockpick.screen

import com.ian.stockpick.domain.model.ScreenResult

data class EditorUiState(
    val isRunning: Boolean = false,
    val progressMessage: String = "",
    val lastRunTime: String? = null,
    val dataAsOf: String? = null,
    val hitCount: Int = 0,
    val results: List<ScreenResult> = emptyList(),
    val skippedCount: Int = 0,
    val errorMessage: String? = null,
    val offHoursWarning: Boolean = false,
    val refreshMessage: String? = null,
)
