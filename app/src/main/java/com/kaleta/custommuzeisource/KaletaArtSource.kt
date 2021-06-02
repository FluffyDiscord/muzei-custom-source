package com.kaleta.custommuzeisource

import android.app.Service
import android.content.Intent
import android.os.Binder

/**
 * This class is kept only to serve as a tombstone to Muzei to know to replace it
 * with [KaletaArtProvider].
 */
class KaletaArtSource : Service() {
    override fun onBind(intent: Intent?): Binder? = null
}
