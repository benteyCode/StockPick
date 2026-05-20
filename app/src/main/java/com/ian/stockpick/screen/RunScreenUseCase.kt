package com.ian.stockpick.screen

import com.ian.stockpick.data.repository.StockReadRepository
import com.ian.stockpick.domain.indicator.IndicatorCalculator
import com.ian.stockpick.domain.model.QuoteSnapshot
import com.ian.stockpick.domain.model.ScreenResult
import com.ian.stockpick.domain.model.Stock
import com.ian.stockpick.domain.rule.ScreenInput
import com.ian.stockpick.domain.rule.ScreenRuleEngine
import com.ian.stockpick.domain.rule.ScreenRules
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

class RunScreenUseCase(
    private val repository: StockReadRepository,
    private val ruleEngine: ScreenRuleEngine = ScreenRuleEngine(),
    private val calc: IndicatorCalculator = IndicatorCalculator(),
) {
    suspend fun run(
        forceRefresh: Boolean = false,
        onProgress: (ScreenProgress) -> Unit = {},
    ): Result<RunScreenOutcome> = try {
        onProgress(ScreenProgress.LoadingList)
        val stocks = repository.getStockList(forceRefresh)
        onProgress(ScreenProgress.CoarseFilter(stocks.size))

        val snapshots = repository.getSnapshots(stocks)
            .filter { !it.suspended && it.volume > 0 }
        val snapshotMap = snapshots.associateBy { it.code }

        val candidates = stocks.filter { snapshotMap.containsKey(it.code) }
        val results = mutableListOf<ScreenResult>()
        var skipped = 0

        for ((index, stock) in candidates.withIndex()) {
            onProgress(ScreenProgress.FineFilter(index + 1, candidates.size))
            when (
                val outcome = evaluateCandidate(stock, snapshotMap.getValue(stock.code), forceRefresh)
            ) {
                is CandidateOutcome.Hit -> results += outcome.result
                CandidateOutcome.Skip -> skipped++
                CandidateOutcome.NoMatch -> Unit
            }
        }

        onProgress(ScreenProgress.Done(skipped))
        Result.success(
            RunScreenOutcome(
                results = results.sortedBy { abs(it.distanceToMa10) },
                skipped = skipped,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onProgress(ScreenProgress.Failed(e.message ?: "筛选失败"))
        Result.failure(e)
    }

    private suspend fun evaluateCandidate(
        stock: Stock,
        snap: QuoteSnapshot,
        forceRefresh: Boolean,
    ): CandidateOutcome {
        val bars = repository.getDailyBars(stock, forceRefresh) ?: return CandidateOutcome.Skip
        if (bars.size < ScreenRules.KLINE_MIN_BARS) return CandidateOutcome.Skip

        val input = ScreenInput(
            historicalBars = bars,
            todayOpen = snap.open,
            currentPrice = snap.price,
            currentVolume = snap.volume,
        )
        if (!ruleEngine.matches(input)) return CandidateOutcome.NoMatch

        val ma10 = calc.ma(bars, ScreenRules.MA_SHORT) ?: return CandidateOutcome.NoMatch
        val avg5 = calc.avgVolume(bars, ScreenRules.AVG_VOLUME_SHORT) ?: return CandidateOutcome.NoMatch
        return CandidateOutcome.Hit(
            ScreenResult(
                stock = stock,
                price = snap.price,
                changePercent = snap.changePercent,
                distanceToMa10 = calc.distanceRatio(snap.price, ma10),
                volumeRatio = if (avg5 > 0) snap.volume / avg5 else 0.0,
                statusLabel = ruleEngine.buildStatusLabel(input),
            ),
        )
    }

    private sealed interface CandidateOutcome {
        data class Hit(val result: ScreenResult) : CandidateOutcome
        data object Skip : CandidateOutcome
        data object NoMatch : CandidateOutcome
    }
}

data class RunScreenOutcome(
    val results: List<ScreenResult>,
    val skipped: Int,
)
