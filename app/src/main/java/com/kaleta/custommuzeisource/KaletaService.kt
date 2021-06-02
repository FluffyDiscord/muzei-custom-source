package com.kaleta.custommuzeisource

import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.*
import java.io.IOException


internal interface KaletaService {

    companion object {

        private fun createService(apiUrl: String) : KaletaService {
            val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        var request = chain.request()
                        val url = request.url.newBuilder().build()
                        request = request.newBuilder().url(url).build()
                        chain.proceed(request)
                    }
                    .build()

            var baseUrl: String = apiUrl
            if (!baseUrl.endsWith("/")) baseUrl += "/"

            val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()

            return retrofit.create()
        }

        @Throws(IOException::class)
        internal fun getImages(apiUrl: String, rawJsonBody: String? = null): List<ApiImage> {
            var jsonBody = JSONObject("{}")
            if(rawJsonBody !== null) {
                jsonBody = try {
                    JSONObject(rawJsonBody)
                } catch (e: JSONException) {
                    JSONObject("{}")
                }
            }
            return createService(apiUrl).imageRequest(jsonBody.toString()).execute().body() ?: throw IOException("Response was null")
        }
    }

    @Headers("Content-Type: application/json")
    @POST(".")
    fun imageRequest(@Body jsonBody: String): Call<List<ApiImage>>

    data class ApiImage(
        val id: String,
        val name: String,
        val artist: String,
        val url: String,
        val downloadUrl: String,
    )
}
