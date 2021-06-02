package com.kaleta.custommuzeisource

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.work.*
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import java.io.IOException
import java.util.concurrent.TimeUnit


class KaletaWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "KW"

        @SuppressLint("ServiceCast")
        internal fun enqueueLoad(context: Context, ignoreRequirements: Boolean = false) {
            val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                context
            )

            val constraints = Constraints.Builder()
            constraints.setRequiredNetworkType(NetworkType.CONNECTED)

            if(!ignoreRequirements) {
                if(sharedPreferences.getBoolean("requiresCharging", false)) {
                    constraints.setRequiresCharging(true)
                }
                if(sharedPreferences.getBoolean("batteryNotLow", false)) {
                    constraints.setRequiresBatteryNotLow(true)
                }
                constraints.setRequiresStorageNotLow(true)
            }

            WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<KaletaWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .setConstraints(constraints.build())
                .addTag(TAG)
                .build()
            )
        }
    }

    override fun doWork(): Result {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )

        if(sharedPreferences.getBoolean("onlyWifi", false) && !checkWifiOnAndConnected()) {
            return Result.failure()
        }

        val apiUrl = sharedPreferences.getString("apiUrl", null);
        if(apiUrl === null) {
            Log.w(TAG, "Please define apiUrl in settings.")
            return Result.failure()
        }

        val photos = try {
            KaletaService.getImages(apiUrl, sharedPreferences.getString("requestRawJsonBody", null))
        } catch (e: IOException) {
            Log.w(TAG, "Error reading response", e)
            return Result.retry()
        }

        if (photos.isEmpty()) {
            Log.w(TAG, "No images returned from API.")
            return Result.failure()
        }

        var list = ArrayList<Artwork>()
        for(photo in photos) {
            list.add(Artwork.Builder()
                .token(photo.id)
                .title(photo.name)
                .byline(photo.artist)
                .persistentUri(photo.downloadUrl.toUri())
                .webUri(photo.url.toUri())
                .build()
            )
        }

        ProviderContract.getProviderClient(
            applicationContext,
            "com.kaleta.custommuzeisource"
        ).setArtwork(list)

        return Result.success()
    }

    private fun checkWifiOnAndConnected(): Boolean
    {
        val wifiMgr = getSystemService(applicationContext, WifiManager::class.java)
        return if (wifiMgr!!.isWifiEnabled) { // Wi-Fi adapter is ON
            val wifiInfo = wifiMgr.connectionInfo

            // Is connected to an access point ?
            wifiInfo.networkId != -1
        } else {
            false // Wi-Fi adapter is OFF
        }
    }
}
