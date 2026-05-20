# A 股条件筛选 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 StockPick Editor 页：用户手动触发，从全 A 股（含创业板/科创板）中按 R1–R4 规则实时筛选并展示结果。

**Architecture:** App 直连东方财富免费接口，Room 缓存股票列表与日 K 线；`RunScreenUseCase` 编排「列表 → 粗筛实时快照 → 精筛 K 线 + 规则引擎」两阶段流程；Compose 深色 UI 展示进度与结果。

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Retrofit/OkHttp, Room, Coroutines/Flow, ViewModel, JUnit4

**Spec:** `docs/superpowers/specs/2026-05-20-a-share-screener-design.md`

---

## File Structure

```
app/src/main/java/com/ian/stockpick/
├── MainActivity.kt                          # 入口，挂载 EditorScreen
├── StockPickApp.kt                          # Application，提供 Repository 单例
├── domain/
│   ├── model/
│   │   ├── Stock.kt                         # 代码、名称、市场、secId
│   │   ├── DailyBar.kt                      # 日 K 单根
│   │   ├── QuoteSnapshot.kt                 # 实时快照
│   │   ├── ScreenCandidate.kt               # 指标 + 规则中间态
│   │   └── ScreenResult.kt                  # UI 展示用结果
│   ├── indicator/
│   │   └── IndicatorCalculator.kt           # MA、均量
│   └── rule/
│       ├── ScreenRules.kt                   # 阈值常量
│       └── ScreenRuleEngine.kt              # R1–R4 AND
├── data/
│   ├── remote/
│   │   ├── EastMoneyApi.kt                  # Retrofit 接口
│   │   ├── EastMoneyDto.kt                  # 响应 DTO + 映射
│   │   └── EastMoneyQuoteDataSource.kt      # 实现 QuoteDataSource
│   ├── local/
│   │   ├── StockPickDatabase.kt
│   │   ├── StockEntity.kt / DailyBarEntity.kt
│   │   ├── StockDao.kt / DailyBarDao.kt
│   │   └── QuoteCache.kt                    # 缓存读写 + 过期判断
│   ├── repository/
│   │   └── StockRepository.kt               # 列表/K线/快照 + 缓存
│   └── QuoteDataSource.kt                   # 接口（便于测试 Mock）
├── screen/
│   ├── RunScreenUseCase.kt                  # 筛选编排
│   ├── ScreenProgress.kt                    # 进度 sealed class
│   └── EditorViewModel.kt                   # UI 状态
└── ui/
    ├── theme/                               # 已有，扩展深色色板
    ├── screen/
    │   └── EditorScreen.kt                  # 主屏
    └── components/
        ├── FilterSummaryCard.kt
        ├── StatusCards.kt
        └── ResultStockCard.kt

app/src/test/java/com/ian/stockpick/
├── domain/indicator/IndicatorCalculatorTest.kt
├── domain/rule/ScreenRuleEngineTest.kt
├── screen/RunScreenUseCaseTest.kt
└── testutil/FixtureBars.kt                  # 固定 K 线 fixture
```

---

### Task 1: 项目依赖与网络权限

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 在 `gradle/libs.versions.toml` 追加版本与库**

```toml
[versions]
retrofit = "2.11.0"
okhttp = "4.12.0"
room = "2.6.1"
ksp = "2.0.21-1.0.28"
lifecycleViewmodel = "2.8.7"
coroutines = "1.9.0"
moshi = "1.15.1"

[libraries]
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-moshi = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodel" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
moshi = { group = "com.squareup.moshi", name = "moshi-kotlin", version.ref = "moshi" }
moshi-codegen = { group = "com.squareup.moshi", name = "moshi-kotlin-codegen", version.ref = "moshi" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: 在 `app/build.gradle` 启用 KSP 并添加依赖**

在文件顶部 `plugins` 块追加：

```gradle
alias(libs.plugins.ksp)
```

在 `dependencies` 块追加：

```gradle
implementation libs.retrofit
implementation libs.retrofit.moshi
implementation libs.okhttp
implementation libs.okhttp.logging
implementation libs.room.runtime
implementation libs.room.ktx
ksp libs.room.compiler
implementation libs.lifecycle.viewmodel.compose
implementation libs.kotlinx.coroutines.android
testImplementation libs.kotlinx.coroutines.test
implementation libs.moshi
ksp libs.moshi.codegen
```

在根 `build.gradle` 的 `plugins` 块追加（`apply false`）：

```gradle
alias(libs.plugins.ksp) apply false
```

- [ ] **Step 3: 添加 INTERNET 权限**

在 `AndroidManifest.xml` 的 `<manifest>` 下、`<application>` 前添加：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- [ ] **Step 4: 同步并编译**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle build.gradle app/src/main/AndroidManifest.xml
git commit -m "chore: add networking, room, and coroutines dependencies"
```

---

### Task 2: 领域模型

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/domain/model/Stock.kt`
- Create: `app/src/main/java/com/ian/stockpick/domain/model/DailyBar.kt`
- Create: `app/src/main/java/com/ian/stockpick/domain/model/QuoteSnapshot.kt`
- Create: `app/src/main/java/com/ian/stockpick/domain/model/ScreenResult.kt`
- Create: `app/src/main/java/com/ian/stockpick/domain/rule/ScreenRules.kt`

- [ ] **Step 1: 创建模型与规则常量**

`Stock.kt`:

```kotlin
package com.ian.stockpick.domain.model

enum class MarketBoard { SH_MAIN, SZ_MAIN, CHINEXT, STAR, OTHER }

data class Stock(
    val code: String,
    val name: String,
    val secId: String,          // "1.600519" 或 "0.000001"
    val board: MarketBoard,
) {
    val isSt: Boolean get() = name.contains("ST", ignoreCase = true)
}
```

`DailyBar.kt`:

```kotlin
package com.ian.stockpick.domain.model

data class DailyBar(
    val date: String,           // yyyy-MM-dd
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
)
```

`QuoteSnapshot.kt`:

```kotlin
package com.ian.stockpick.domain.model

data class QuoteSnapshot(
    val code: String,
    val price: Double,
    val open: Double,
    val changePercent: Double,
    val volume: Long,
    val suspended: Boolean = false,
)
```

`ScreenResult.kt`:

```kotlin
package com.ian.stockpick.domain.model

data class ScreenResult(
    val stock: Stock,
    val price: Double,
    val changePercent: Double,
    val distanceToMa10: Double,
    val volumeRatio: Double,
    val statusLabel: String,
)
```

`ScreenRules.kt`:

```kotlin
package com.ian.stockpick.domain.rule

object ScreenRules {
    const val MA10_PROXIMITY_MIN = -0.01   // -1%
    const val MA10_PROXIMITY_MAX = 0.01    // +1%
    const val MA_CONVERGENCE_MAX = 0.02    // 2%
    const val VOLUME_SURGE_MULTIPLIER = 1.5
    const val VOLUME_SURGE_MIN_DAYS = 2
    const val VOLUME_SURGE_LOOKBACK = 20
    const val PULLBACK_LOOKBACK_MIN = 5
    const val PULLBACK_LOOKBACK_MAX = 10
    const val MA_SHORT = 10
    const val MA_LONG = 20
    const val AVG_VOLUME_SHORT = 5
    const val AVG_VOLUME_LONG = 20
    const val KLINE_MIN_BARS = 30
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/domain/
git commit -m "feat: add domain models and screen rule constants"
```

---

### Task 3: IndicatorCalculator（TDD）

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/domain/indicator/IndicatorCalculator.kt`
- Create: `app/src/test/java/com/ian/stockpick/domain/indicator/IndicatorCalculatorTest.kt`

- [ ] **Step 1: 写失败测试**

`IndicatorCalculatorTest.kt`:

```kotlin
package com.ian.stockpick.domain.indicator

import com.ian.stockpick.domain.model.DailyBar
import org.junit.Assert.assertEquals
import org.junit.Test

class IndicatorCalculatorTest {

    private val calc = IndicatorCalculator()

    @Test
    fun `ma10 returns average of last 10 closes`() {
        val bars = (1..10).map { i ->
            DailyBar("2026-01-$i", open = i.toDouble(), close = i.toDouble(),
                high = i.toDouble(), low = i.toDouble(), volume = 1000L)
        }
        assertEquals(5.5, calc.ma(bars, 10)!!, 0.001)
    }

    @Test
    fun `avgVolume excludes today and uses prior N days`() {
        val bars = (1..6).map { i ->
            DailyBar("2026-01-0$i", 1.0, 1.0, 1.0, 1.0, volume = i * 1000L)
        }
        // bars.last() = today; avg of prior 5 volumes: 1000+2000+3000+4000+5000 / 5 = 3000
        assertEquals(3000.0, calc.avgVolume(bars.dropLast(1), 5)!!, 0.001)
    }

    @Test
    fun `ma returns null when insufficient bars`() {
        val bars = listOf(DailyBar("2026-01-01", 1.0, 1.0, 1.0, 1.0, 1000L))
        assertEquals(null, calc.ma(bars, 10))
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ian.stockpick.domain.indicator.IndicatorCalculatorTest"`
Expected: FAIL — class not found

- [ ] **Step 3: 最小实现**

`IndicatorCalculator.kt`:

```kotlin
package com.ian.stockpick.domain.indicator

import com.ian.stockpick.domain.model.DailyBar

class IndicatorCalculator {

    fun ma(bars: List<DailyBar>, period: Int): Double? {
        if (bars.size < period) return null
        return bars.takeLast(period).map { it.close }.average()
    }

    fun avgVolume(bars: List<DailyBar>, period: Int): Double? {
        if (bars.size < period) return null
        return bars.takeLast(period).map { it.volume.toDouble() }.average()
    }

    fun distanceRatio(price: Double, ma: Double): Double = (price - ma) / ma

    fun maConvergenceRatio(maShort: Double, maLong: Double): Double =
        kotlin.math.abs(maShort - maLong) / maLong
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ian.stockpick.domain.indicator.IndicatorCalculatorTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/domain/indicator/ app/src/test/java/com/ian/stockpick/domain/indicator/
git commit -m "feat: add IndicatorCalculator with unit tests"
```

---

### Task 4: ScreenRuleEngine（TDD）

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/domain/rule/ScreenRuleEngine.kt`
- Create: `app/src/main/java/com/ian/stockpick/domain/rule/ScreenInput.kt`
- Create: `app/src/test/java/com/ian/stockpick/testutil/FixtureBars.kt`
- Create: `app/src/test/java/com/ian/stockpick/domain/rule/ScreenRuleEngineTest.kt`

- [ ] **Step 1: 定义引擎输入**

`ScreenInput.kt`:

```kotlin
package com.ian.stockpick.domain.rule

import com.ian.stockpick.domain.model.DailyBar

data class ScreenInput(
    val historicalBars: List<DailyBar>,   // 不含今日 partial 的已完成日 K
    val todayOpen: Double,
    val currentPrice: Double,
    val currentVolume: Long,
)
```

- [ ] **Step 2: 写失败测试（含 hit / miss fixture）**

`FixtureBars.kt` — 构造 30 根日 K，close 缓升，volume 基线 1_000_000，其中 2 天放量 2_000_000：

```kotlin
package com.ian.stockpick.testutil

import com.ian.stockpick.domain.model.DailyBar

object FixtureBars {
    fun uptrendWithVolumeSurge(): List<DailyBar> =
        (1..30).map { i ->
            val vol = when (i) {
                8, 15 -> 2_000_000L
                else -> 1_000_000L
            }
            DailyBar(
                date = "2026-04-${i.toString().padStart(2, '0')}",
                open = 10.0 + i * 0.05,
                close = 10.0 + i * 0.06,
                high = 10.0 + i * 0.07,
                low = 10.0 + i * 0.04,
                volume = vol,
            )
        }
}
```

`ScreenRuleEngineTest.kt`:

```kotlin
package com.ian.stockpick.domain.rule

import com.ian.stockpick.testutil.FixtureBars
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRuleEngineTest {

    private val engine = ScreenRuleEngine()

    @Test
    fun `passes all rules when pullback shrink negative near ma10`() {
        val bars = FixtureBars.uptrendWithVolumeSurge()
        val ma10 = bars.takeLast(10).map { it.close }.average()
        val input = ScreenInput(
            historicalBars = bars,
            todayOpen = ma10 * 1.005,
            currentPrice = ma10 * 0.998,
            currentVolume = 500_000L,
        )
        assertTrue(engine.matches(input))
    }

    @Test
    fun `fails when price too far below ma10`() {
        val bars = FixtureBars.uptrendWithVolumeSurge()
        val ma10 = bars.takeLast(10).map { it.close }.average()
        val input = ScreenInput(
            historicalBars = bars,
            todayOpen = ma10,
            currentPrice = ma10 * 0.95,
            currentVolume = 500_000L,
        )
        assertFalse(engine.matches(input))
    }

    @Test
    fun `fails when today is positive candle`() {
        val bars = FixtureBars.uptrendWithVolumeSurge()
        val ma10 = bars.takeLast(10).map { it.close }.average()
        val input = ScreenInput(
            historicalBars = bars,
            todayOpen = ma10 * 0.99,
            currentPrice = ma10 * 1.001,
            currentVolume = 500_000L,
        )
        assertFalse(engine.matches(input))
    }

    @Test
    fun `fails when insufficient volume surge days`() {
        val bars = FixtureBars.uptrendWithVolumeSurge().mapIndexed { i, b ->
            if (i == 7) b.copy(volume = 2_000_000L) else b.copy(volume = 1_000_000L)
        }
        val ma10 = bars.takeLast(10).map { it.close }.average()
        val input = ScreenInput(
            historicalBars = bars,
            todayOpen = ma10 * 1.005,
            currentPrice = ma10 * 0.998,
            currentVolume = 500_000L,
        )
        assertFalse(engine.matches(input))
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ian.stockpick.domain.rule.ScreenRuleEngineTest"`
Expected: FAIL

- [ ] **Step 4: 实现 ScreenRuleEngine**

`ScreenRuleEngine.kt`:

```kotlin
package com.ian.stockpick.domain.rule

import com.ian.stockpick.domain.indicator.IndicatorCalculator
import com.ian.stockpick.domain.model.DailyBar

class ScreenRuleEngine(
    private val calc: IndicatorCalculator = IndicatorCalculator(),
) {

    fun matches(input: ScreenInput): Boolean =
        rule1(input) && rule2(input) && rule3(input) && rule4(input)

    private fun rule1(input: ScreenInput): Boolean {
        val bars = input.historicalBars
        val ma10 = calc.ma(bars, ScreenRules.MA_SHORT) ?: return false
        val ma20 = calc.ma(bars, ScreenRules.MA_LONG) ?: return false
        if (ma10 <= ma20) return false
        if (input.currentPrice <= ma20) return false
        val dist = calc.distanceRatio(input.currentPrice, ma10)
        if (dist < ScreenRules.MA10_PROXIMITY_MIN || dist > ScreenRules.MA10_PROXIMITY_MAX) return false
        return hadPullbackSetup(bars)
    }

    private fun hadPullbackSetup(bars: List<DailyBar>): Boolean {
        val lookback = bars.takeLast(ScreenRules.PULLBACK_LOOKBACK_MAX)
        if (lookback.size < ScreenRules.PULLBACK_LOOKBACK_MIN) return false
        return lookback.any { bar ->
            val idx = bars.indexOf(bar)
            val prior = bars.take(idx + 1)
            val ma10AtBar = calc.ma(prior, ScreenRules.MA_SHORT) ?: return@any false
            bar.close > ma10AtBar
        }
    }

    private fun rule2(input: ScreenInput): Boolean {
        val ma10 = calc.ma(input.historicalBars, ScreenRules.MA_SHORT) ?: return false
        val ma20 = calc.ma(input.historicalBars, ScreenRules.MA_LONG) ?: return false
        return calc.maConvergenceRatio(ma10, ma20) <= ScreenRules.MA_CONVERGENCE_MAX
    }

    private fun rule3(input: ScreenInput): Boolean {
        val recent = input.historicalBars.takeLast(ScreenRules.VOLUME_SURGE_LOOKBACK)
        if (recent.size < ScreenRules.VOLUME_SURGE_LOOKBACK) return false
        val avg20 = calc.avgVolume(recent, ScreenRules.VOLUME_SURGE_LOOKBACK) ?: return false
        val surgeDays = recent.count { it.volume > avg20 * ScreenRules.VOLUME_SURGE_MULTIPLIER }
        return surgeDays >= ScreenRules.VOLUME_SURGE_MIN_DAYS
    }

    private fun rule4(input: ScreenInput): Boolean {
        if (input.currentPrice >= input.todayOpen) return false
        val avg5 = calc.avgVolume(input.historicalBars, ScreenRules.AVG_VOLUME_SHORT) ?: return false
        return input.currentVolume < avg5
    }

    fun buildStatusLabel(input: ScreenInput): String {
        val ma10 = calc.ma(input.historicalBars, ScreenRules.MA_SHORT) ?: return "缩量回调"
        val ma20 = calc.ma(input.historicalBars, ScreenRules.MA_LONG) ?: return "缩量回调"
        return if (calc.maConvergenceRatio(ma10, ma20) <= ScreenRules.MA_CONVERGENCE_MAX) {
            "MA10/20粘合"
        } else {
            "缩量回调"
        }
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ian.stockpick.domain.rule.ScreenRuleEngineTest"`
Expected: 4 tests passed

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/domain/rule/ app/src/test/java/com/ian/stockpick/domain/rule/ app/src/test/java/com/ian/stockpick/testutil/
git commit -m "feat: add ScreenRuleEngine with R1-R4 rule tests"
```

---

### Task 5: Room 本地缓存

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/data/local/StockEntity.kt`
- Create: `app/src/main/java/com/ian/stockpick/data/local/DailyBarEntity.kt`
- Create: `app/src/main/java/com/ian/stockpick/data/local/StockDao.kt`
- Create: `app/src/main/java/com/ian/stockpick/data/local/DailyBarDao.kt`
- Create: `app/src/main/java/com/ian/stockpick/data/local/StockPickDatabase.kt`
- Create: `app/src/main/java/com/ian/stockpick/data/local/QuoteCache.kt`

- [ ] **Step 1: 创建 Entity 与 Dao**

`StockEntity.kt`:

```kotlin
@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val code: String,
    val name: String,
    val secId: String,
    val board: String,
    val cachedAt: Long,
)
```

`DailyBarEntity.kt`:

```kotlin
@Entity(
    tableName = "daily_bars",
    primaryKeys = ["code", "date"],
)
data class DailyBarEntity(
    val code: String,
    val date: String,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val cachedAt: Long,
)
```

`StockDao.kt` — `upsertAll`, `getAll`, `getCachedAt()`  
`DailyBarDao.kt` — `upsertAll`, `getByCode(code)`, `deleteByCode(code)`

- [ ] **Step 2: Database + QuoteCache**

`StockPickDatabase.kt` — version 1, entities `[StockEntity, DailyBarEntity]`

`QuoteCache.kt`:

```kotlin
class QuoteCache(
    private val stockDao: StockDao,
    private val dailyBarDao: DailyBarDao,
) {
    fun isStockListFresh(now: Long = System.currentTimeMillis()): Boolean {
        val cachedAt = stockDao.getCachedAt() ?: return false
        return now - cachedAt < 24 * 60 * 60 * 1000
    }

    fun isKlineFresh(code: String, now: Long = System.currentTimeMillis()): Boolean {
        val bars = dailyBarDao.getByCode(code)
        if (bars.isEmpty()) return false
        val sameDay = java.time.Instant.ofEpochMilli(now)
            .atZone(java.time.ZoneId.of("Asia/Shanghai"))
            .toLocalDate()
            .toString()
        return bars.first().cachedAt >= startOfDayMillis(sameDay)
    }

    private fun startOfDayMillis(date: String): Long { /* Asia/Shanghai 00:00 */ }
}
```

- [ ] **Step 3: 编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/data/local/
git commit -m "feat: add Room entities, DAOs, and quote cache helpers"
```

---

### Task 6: 东方财富 API 与 QuoteDataSource

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/data/QuoteDataSource.kt`
- Create: `app/src/main/java/com/ian/stockpick/data/remote/EastMoneyApi.kt`
- Create: `app/src/main/java/com/ian/stockpick/data/remote/EastMoneyDto.kt`
- Create: `app/src/main/java/com/ian/stockpick/data/remote/EastMoneyQuoteDataSource.kt`

- [ ] **Step 1: 定义接口**

`QuoteDataSource.kt`:

```kotlin
interface QuoteDataSource {
    suspend fun fetchAllStocks(): List<Stock>
    suspend fun fetchSnapshots(secIds: List<String>): List<QuoteSnapshot>
    suspend fun fetchDailyBars(secId: String, limit: Int = 120): List<DailyBar>
}
```

- [ ] **Step 2: Retrofit API（东方财富 clist + kline）**

`EastMoneyApi.kt` — 两个 GET：

1. **全 A 股列表**（分页 pz=100，循环 pn 直到取完）  
   `https://push2.eastmoney.com/api/qt/clist/get`  
   参数：`fs=m:0+t:6,m:0+t:80,m:0+t:81+s:2048,m:1+t:2,m:1+t:23`  
   `fields=f12,f14,f13`（代码、名称、市场）  
   过滤：`name` 含 ST 跳过

2. **日 K 线**  
   `https://push2his.eastmoney.com/api/qt/stock/kline/get`  
   参数：`secid`, `klt=101`, `fqt=1`, `lmt=120`, `fields2=f51,f52,f53,f54,f55,f56`

3. **实时快照（批量）** — 复用 clist，`fields=f12,f2,f3,f5,f17,f152`，按 secId 分批（每批 200）

`EastMoneyDto.kt` — 解析 `data.diff` 与 kline `klines`（逗号分隔字符串）

- [ ] **Step 3: 实现 EastMoneyQuoteDataSource**

- OkHttp：`connectTimeout=15s`, `readTimeout=30s`
- 重试：失败最多 3 次，延迟 1s/2s/4s
- `f152==2` 或 volume=0 标记 `suspended=true`
- secId 映射：`f13==1` → `1.{code}`，`f13==0` → `0.{code}`

- [ ] **Step 4: 手动冒烟（可选 debug 单元或 logcat）**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/data/
git commit -m "feat: add East Money quote data source"
```

---

### Task 7: StockRepository

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/data/repository/StockRepository.kt`

- [ ] **Step 1: 实现缓存优先读写**

```kotlin
class StockRepository(
    private val remote: QuoteDataSource,
    private val cache: QuoteCache,
    private val stockDao: StockDao,
    private val dailyBarDao: DailyBarDao,
) {
    suspend fun getStockList(forceRefresh: Boolean = false): List<Stock> {
        if (!forceRefresh && cache.isStockListFresh()) {
            return stockDao.getAll().map { it.toDomain() }
        }
        val stocks = remote.fetchAllStocks().filter { !it.isSt }
        stockDao.upsertAll(stocks.map { it.toEntity() })
        return stocks
    }

    suspend fun getDailyBars(stock: Stock, forceRefresh: Boolean = false): List<DailyBar>? {
        if (!forceRefresh && cache.isKlineFresh(stock.code)) {
            return dailyBarDao.getByCode(stock.code).map { it.toDomain() }
        }
        return try {
            val bars = remote.fetchDailyBars(stock.secId)
            dailyBarDao.deleteByCode(stock.code)
            dailyBarDao.upsertAll(bars.map { stock.code to it }.toEntities())
            bars
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getSnapshots(stocks: List<Stock>): List<QuoteSnapshot> =
        remote.fetchSnapshots(stocks.map { it.secId })
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/data/repository/
git commit -m "feat: add StockRepository with cache-first reads"
```

---

### Task 8: RunScreenUseCase（TDD 集成）

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/screen/ScreenProgress.kt`
- Create: `app/src/main/java/com/ian/stockpick/screen/RunScreenUseCase.kt`
- Create: `app/src/test/java/com/ian/stockpick/screen/RunScreenUseCaseTest.kt`

- [ ] **Step 1: 进度模型**

```kotlin
sealed interface ScreenProgress {
    data object LoadingList : ScreenProgress
    data class CoarseFilter(val total: Int) : ScreenProgress
    data class FineFilter(val done: Int, val total: Int) : ScreenProgress
    data class Done(val skipped: Int) : ScreenProgress
    data class Failed(val message: String) : ScreenProgress
}
```

- [ ] **Step 2: 写失败集成测试（Fake Repository）**

Mock `StockRepository` 返回 3 只股票，其中 1 只满足规则；断言 `results.size == 1` 且按 `distanceToMa10` 排序。

- [ ] **Step 3: 实现 RunScreenUseCase**

核心逻辑：

```kotlin
class RunScreenUseCase(
    private val repository: StockRepository,
    private val ruleEngine: ScreenRuleEngine = ScreenRuleEngine(),
    private val calc: IndicatorCalculator = IndicatorCalculator(),
) {
    suspend fun run(
        forceRefresh: Boolean = false,
        onProgress: (ScreenProgress) -> Unit = {},
    ): Result<List<ScreenResult>> = runCatching {
        onProgress(ScreenProgress.LoadingList)
        val stocks = repository.getStockList(forceRefresh)
        onProgress(ScreenProgress.CoarseFilter(stocks.size))
        val snapshots = repository.getSnapshots(stocks)
            .filter { !it.suspended && it.volume > 0 }
        val snapshotMap = snapshots.associateBy { it.code }

        val candidates = stocks.filter { snapshotMap.containsKey(it.code) }
        val results = mutableListOf<ScreenResult>()
        var skipped = 0

        candidates.forEachIndexed { index, stock ->
            onProgress(ScreenProgress.FineFilter(index + 1, candidates.size))
            val bars = repository.getDailyBars(stock, forceRefresh) ?: run {
                skipped++
                return@forEachIndexed
            }
            if (bars.size < ScreenRules.KLINE_MIN_BARS) { skipped++; return@forEachIndexed }

            val snap = snapshotMap.getValue(stock.code)
            val input = ScreenInput(
                historicalBars = bars,
                todayOpen = snap.open,
                currentPrice = snap.price,
                currentVolume = snap.volume,
            )
            if (!ruleEngine.matches(input)) return@forEachIndexed

            val ma10 = calc.ma(bars, ScreenRules.MA_SHORT)!!
            val avg5 = calc.avgVolume(bars, ScreenRules.AVG_VOLUME_SHORT)!!
            results += ScreenResult(
                stock = stock,
                price = snap.price,
                changePercent = snap.changePercent,
                distanceToMa10 = calc.distanceRatio(snap.price, ma10),
                volumeRatio = snap.volume / avg5,
                statusLabel = ruleEngine.buildStatusLabel(input),
            )
        }

        onProgress(ScreenProgress.Done(skipped))
        results.sortedBy { kotlin.math.abs(it.distanceToMa10) }
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `./gradlew :app:testDebugUnitTest --tests "com.ian.stockpick.screen.RunScreenUseCaseTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/screen/ app/src/test/java/com/ian/stockpick/screen/
git commit -m "feat: add RunScreenUseCase with integration test"
```

---

### Task 9: 深色主题色板

**Files:**
- Modify: `app/src/main/java/com/ian/stockpick/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/ian/stockpick/ui/theme/Theme.kt`

- [ ] **Step 1: 按设计稿定义深色色板**

```kotlin
// Color.kt
val BgPrimary = Color(0xFF0D0F12)
val CardBg = Color(0xFF1A1D23)
val CardBorder = Color(0xFF2A2F38)
val AccentBlue = Color(0xFF4DA3FF)
val AccentGreen = Color(0xFF3DDC84)
val TextPrimary = Color(0xFFE8EAED)
val TextSecondary = Color(0xFF9AA0A6)
val RedDown = Color(0xFFFF5252)
val GreenUp = Color(0xFF3DDC84)
```

`Theme.kt` — `darkColorScheme` 使用上述颜色，`StockPickTheme(darkTheme = true)` 默认深色。

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/ui/theme/
git commit -m "feat: add dark theme colors matching design mockup"
```

---

### Task 10: EditorViewModel

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/screen/EditorViewModel.kt`
- Create: `app/src/main/java/com/ian/stockpick/screen/EditorUiState.kt`

- [ ] **Step 1: UI 状态**

```kotlin
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
)
```

- [ ] **Step 2: ViewModel**

```kotlin
class EditorViewModel(
    private val runScreen: RunScreenUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun runScreen(forceRefresh: Boolean = false) {
        if (_state.value.isRunning) return
        job = viewModelScope.launch {
            _state.update { it.copy(isRunning = true, errorMessage = null, offHoursWarning = !isMarketHours()) }
            runScreen.run(forceRefresh) { progress ->
                _state.update { it.copy(progressMessage = progress.toMessage()) }
            }.onSuccess { results ->
                val now = formatNow()
                _state.update {
                    it.copy(
                        isRunning = false,
                        results = results,
                        hitCount = results.size,
                        lastRunTime = now.substring(11, 16),
                        dataAsOf = now,
                        progressMessage = "",
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isRunning = false, errorMessage = e.message ?: "网络异常", progressMessage = "") }
            }
        }
    }

    fun cancel() { job?.cancel(); _state.update { it.copy(isRunning = false, progressMessage = "") } }
    fun refreshCache() { viewModelScope.launch { /* repository.getStockList(forceRefresh=true) 仅刷新缓存，不重跑 */ } }
    fun clearError() { _state.update { it.copy(errorMessage = null) } }
}
```

`isMarketHours()` — 周一至周五 09:30–11:30、13:00–15:00，`Asia/Shanghai`。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/screen/EditorViewModel.kt app/src/main/java/com/ian/stockpick/screen/EditorUiState.kt
git commit -m "feat: add EditorViewModel with run/cancel/refresh state"
```

---

### Task 11: EditorScreen UI

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/ui/components/FilterSummaryCard.kt`
- Create: `app/src/main/java/com/ian/stockpick/ui/components/StatusCards.kt`
- Create: `app/src/main/java/com/ian/stockpick/ui/components/ResultStockCard.kt`
- Create: `app/src/main/java/com/ian/stockpick/ui/screen/EditorScreen.kt`

- [ ] **Step 1: FilterSummaryCard** — 四条只读规则 + `RO MODE` badge + 蓝色「运行筛选」按钮（running 时 disabled + CircularProgressIndicator）

- [ ] **Step 2: StatusCards** — 双卡片「上次运行」「命中结果」+ 下方 `数据截至`

- [ ] **Step 3: ResultStockCard** — 代码名称、交易所标签、`changePercent` 红绿、距 MA10 / 量比 / 状态标签

- [ ] **Step 4: EditorScreen 组装**

- 顶栏：`StockPick` 居中 + 右侧 IconButton 刷新
- 非交易时段 Banner
- Snackbar 显示 `errorMessage`；有 `skippedCount` 时 Toast
- 空态：`hitCount == 0 && !isRunning` 时显示「当前无符合条件的股票」
- 筛选进行中显示 `progressMessage`，长按或 Secondary 按钮「取消」

- [ ] **Step 5: 编译**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/ui/
git commit -m "feat: add EditorScreen UI with dark theme components"
```

---

### Task 12: 应用入口与依赖注入

**Files:**
- Create: `app/src/main/java/com/ian/stockpick/StockPickApp.kt`
- Modify: `app/src/main/java/com/ian/stockpick/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: StockPickApp 手动 DI（无 Hilt，YAGNI）**

```kotlin
class StockPickApp : Application() {
    lateinit var repository: StockRepository
        private set
    lateinit var runScreenUseCase: RunScreenUseCase
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, StockPickDatabase::class.java, "stockpick.db").build()
        val remote = EastMoneyQuoteDataSource.create()
        val cache = QuoteCache(db.stockDao(), db.dailyBarDao())
        repository = StockRepository(remote, cache, db.stockDao(), db.dailyBarDao())
        runScreenUseCase = RunScreenUseCase(repository)
    }
}
```

- [ ] **Step 2: MainActivity**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as StockPickApp
        enableEdgeToEdge()
        setContent {
            StockPickTheme {
                val vm: EditorViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return EditorViewModel(app.runScreenUseCase) as T
                        }
                    }
                )
                EditorScreen(viewModel = vm)
            }
        }
    }
}
```

- [ ] **Step 3: Manifest 注册 Application**

```xml
<application android:name=".StockPickApp" ...>
```

- [ ] **Step 4: 全量测试 + 编译**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: all tests pass, BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ian/stockpick/StockPickApp.kt app/src/main/java/com/ian/stockpick/MainActivity.kt app/src/main/AndroidManifest.xml
git commit -m "feat: wire EditorScreen in MainActivity with manual DI"
```

---

### Task 13: 真机/模拟器手动验收

**Files:** 无

- [ ] **Step 1: 安装运行**

Run: `./gradlew :app:installDebug`
Expected: 安装成功，App 启动显示深色 Editor 页

- [ ] **Step 2: 交易时段点击「运行筛选」**

Expected: 进度文案变化 → 最终显示命中列表或空态 + 数据截至时间

- [ ] **Step 3: 断网测试**

关闭网络后点击运行 → Snackbar「网络异常」，上次结果保留

- [ ] **Step 4: 刷新按钮**

点击顶栏刷新 → 缓存更新，不自动重跑规则

- [ ] **Step 5: Commit（如有微调）**

```bash
git commit -m "fix: address manual QA findings"  # 仅当有改动
```

---

## Spec Coverage Checklist

| Spec 要求 | 对应 Task |
|-----------|-----------|
| R1–R4 规则 | Task 4 |
| 全 A 含创业板/科创板，排除 ST/停牌 | Task 6, 7 |
| 手动触发 | Task 10, 11 |
| 盘中实时 | Task 6 snapshots + Task 4 rule4 |
| 两阶段筛选 | Task 8 |
| SQLite 缓存 | Task 5, 7 |
| 错误处理/重试/跳过 | Task 6, 7, 8, 10 |
| 深色 Editor UI | Task 9, 11 |
| 仅 Editor 页 | Task 11, 12 |
| 单元/集成测试 | Task 3, 4, 8 |

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-20-a-share-screener.md`.

**两种执行方式：**

1. **Subagent-Driven（推荐）** — 每个 Task 派发独立 subagent，Task 之间做 review，迭代快  
2. **Inline Execution** — 在本会话按 Task 顺序直接实现，每 2–3 个 Task 设检查点

你选哪种？
