package com.ian.stockpick.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.stockpick.data.repository.StockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EditorViewModel(
    private val runScreen: RunScreenUseCase,
    private val repository: StockRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun runScreen(forceRefresh: Boolean = false) {
        if (_state.value.isRunning) return
        job = viewModelScope.launch {
            _state.update {
                it.copy(
                    isRunning = true,
                    errorMessage = null,
                    offHoursWarning = !isMarketHours(),
                    refreshMessage = null,
                )
            }
            runScreen.run(forceRefresh) { progress ->
                _state.update { it.copy(progressMessage = progress.toMessage()) }
            }.onSuccess { outcome ->
                val now = formatNow()
                _state.update {
                    it.copy(
                        isRunning = false,
                        results = outcome.results,
                        hitCount = outcome.results.size,
                        skippedCount = outcome.skipped,
                        lastRunTime = now.substringAfter(" "),
                        dataAsOf = now,
                        progressMessage = "",
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isRunning = false,
                        errorMessage = e.message ?: "网络异常，请检查连接",
                        progressMessage = "",
                    )
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.update { it.copy(isRunning = false, progressMessage = "") }
    }

    fun refreshCache() {
        viewModelScope.launch {
            _state.update { it.copy(refreshMessage = "正在刷新缓存…") }
            try {
                repository.refreshCache()
                _state.update { it.copy(refreshMessage = "缓存已更新") }
            } catch (e: Exception) {
                _state.update {
                    it.copy(errorMessage = e.message ?: "网络异常，请检查连接")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearRefreshMessage() {
        _state.update { it.copy(refreshMessage = null) }
    }

    private fun formatNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        fmt.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return fmt.format(Date())
    }

    companion object {
        fun isMarketHours(): Boolean {
            val tz = TimeZone.getTimeZone("Asia/Shanghai")
            val cal = Calendar.getInstance(tz)
            val day = cal.get(Calendar.DAY_OF_WEEK)
            if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return false
            val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val morning = minutes in (9 * 60 + 30)..(11 * 60 + 30)
            val afternoon = minutes in (13 * 60)..(15 * 60)
            return morning || afternoon
        }
    }
}
