package com.ian.stockpick.data.remote

import com.ian.stockpick.data.QuoteDataSource
import com.ian.stockpick.domain.model.DailyBar
import com.ian.stockpick.domain.model.QuoteSnapshot
import com.ian.stockpick.domain.model.Stock
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class EastMoneyQuoteDataSource internal constructor(
    private val api: EastMoneyApi,
) : QuoteDataSource {

    override suspend fun fetchAllStocks(): List<Stock> {
        val out = ArrayList<Stock>(8192)
        var pn = 1
        while (true) {
            val resp = api.clistGet(pn = pn)
            val diff = resp.data?.diff.orEmpty()
            if (diff.isEmpty()) break
            for (row in diff) {
                row.toStockOrNull()?.let(out::add)
            }
            pn++
        }
        return out
    }

    override suspend fun fetchSnapshots(secIds: List<String>): List<QuoteSnapshot> {
        if (secIds.isEmpty()) return emptyList()
        val out = ArrayList<QuoteSnapshot>(secIds.size)
        for (chunk in secIds.chunked(ULIST_CHUNK)) {
            val secids = chunk.joinToString(",")
            val resp = api.ulistGet(secids = secids)
            val diff = resp.data?.diff.orEmpty()
            for (row in diff) {
                row.toQuoteSnapshotOrNull()?.let(out::add)
            }
        }
        return out
    }

    override suspend fun fetchDailyBars(secId: String, limit: Int): List<DailyBar> {
        val raw = api.klineGet(secid = secId, lmt = limit).data?.klines.orEmpty()
        val bars = raw.mapNotNull(::parseKlineCsvLine)
        return bars.sortedBy { it.date }
    }

    companion object {
        private const val ULIST_CHUNK = 200

        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        fun create(): EastMoneyQuoteDataSource {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(RetryInterceptor(maxRetries = 3))
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .header("User-Agent", BROWSER_USER_AGENT)
                        .build()
                    chain.proceed(req)
                }
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl("https://push2.eastmoney.com/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            val api = retrofit.create(EastMoneyApi::class.java)
            return EastMoneyQuoteDataSource(api)
        }
    }
}

/**
 * Retries up to [maxRetries] times after a failed attempt (IOException or HTTP 5xx),
 * with exponential backoff 1s / 2s / 4s between tries.
 */
private class RetryInterceptor(
    private val maxRetries: Int,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var backoffMs = 1000L
        val request = chain.request()
        for (attempt in 0..maxRetries) {
            val response = tryProceed(chain, request, attempt)
            if (response != null) {
                return response
            }
            if (attempt == maxRetries) {
                break
            }
            sleepBackoff(backoffMs)
            backoffMs *= 2
        }
        throw IOException("HTTP retry exhausted")
    }

    private fun tryProceed(
        chain: Interceptor.Chain,
        request: Request,
        attempt: Int,
    ): Response? {
        return try {
            val response = chain.proceed(request)
            when {
                response.code < 500 -> response
                attempt == maxRetries -> response
                else -> {
                    response.close()
                    null
                }
            }
        } catch (e: IOException) {
            if (attempt == maxRetries) {
                throw e
            }
            null
        }
    }

    private fun sleepBackoff(backoffMs: Long) {
        try {
            TimeUnit.MILLISECONDS.sleep(backoffMs)
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted during HTTP retry", ie)
        }
    }
}
