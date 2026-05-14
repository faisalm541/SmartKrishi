package com.example.smartkrishi

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface AgroApiService {

    // Returns Response<> so we can inspect HTTP status codes (e.g. 409 conflict)
    @POST("agro/1.0/polygons")
    suspend fun createPolygon(
        @Query("appid") apiKey: String,
        @Body polygon: PolygonRequest
    ): Response<PolygonResponse>

    @GET("agro/1.0/soil")
    suspend fun getSoilData(
        @Query("polyid") polyId: String,
        @Query("appid")  apiKey: String
    ): Response<SoilResponse>

    // Fetch all saved polygons so we can reuse an existing one
    @GET("agro/1.0/polygons")
    suspend fun getPolygons(
        @Query("appid") apiKey: String
    ): Response<List<PolygonResponse>>
}

object AgroApi {
    // Fixed: was http:// — AgroMonitoring requires https://
    private const val BASE_URL = "https://api.agromonitoring.com/"

    val service: AgroApiService by lazy {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC  // BODY is too verbose in prod
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgroApiService::class.java)
    }
}
