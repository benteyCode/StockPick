@file:Suppress("LongParameterList")

package com.ian.stockpick.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

private const val KLIME_FULL_URL = "https://push2his.eastmoney.com/api/qt/stock/kline/get"

internal interface EastMoneyApi {

    @GET("api/qt/clist/get")
    suspend fun clistGet(
        @Query("fs") fs: String = EastMoneyParams.CLIST_FS,
        @Query("pn") pn: Int,
        @Query("pz") pz: Int = 100,
        @Query("fields") fields: String = "f12,f14,f13",
        @Query("fid") fid: String = "f3",
        @Query("po") po: Int = 1,
    ): EastMoneyClistResponse

    @GET
    suspend fun klineGet(
        @Url url: String = KLIME_FULL_URL,
        @Query("secid") secid: String,
        @Query("klt") klt: Int = 101,
        @Query("fqt") fqt: Int = 1,
        @Query("lmt") lmt: Int,
        @Query("end") end: String = "20500101",
        @Query("fields1") fields1: String = "f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13",
        @Query("fields2") fields2: String = "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61",
    ): EastMoneyKlineResponse

    @GET("api/qt/ulist.np/get")
    suspend fun ulistGet(
        @Query("fltt") fltt: Int = 2,
        @Query("secids") secids: String,
        @Query("fields") fields: String = "f12,f2,f3,f5,f17,f152",
    ): EastMoneyUlistResponse
}

internal object EastMoneyParams {
    const val CLIST_FS =
        "m:0+t:6,m:0+t:80,m:0+t:81+s:2048,m:1+t:2,m:1+t:23"
}
