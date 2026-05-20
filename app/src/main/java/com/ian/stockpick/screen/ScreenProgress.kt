package com.ian.stockpick.screen

sealed interface ScreenProgress {
    data object LoadingList : ScreenProgress
    data class CoarseFilter(val total: Int) : ScreenProgress
    data class FineFilter(val done: Int, val total: Int) : ScreenProgress
    data class Done(val skipped: Int) : ScreenProgress
    data class Failed(val message: String) : ScreenProgress
}

fun ScreenProgress.toMessage(): String = when (this) {
    ScreenProgress.LoadingList -> "拉取列表…"
    is ScreenProgress.CoarseFilter -> "粗筛 $total 只…"
    is ScreenProgress.FineFilter -> "精筛 ($done/$total)…"
    is ScreenProgress.Done -> if (skipped > 0) "完成，略过 $skipped 只" else "完成"
    is ScreenProgress.Failed -> message
}
