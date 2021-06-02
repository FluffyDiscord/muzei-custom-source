package com.kaleta.custommuzeisource

import com.google.android.apps.muzei.api.provider.MuzeiArtProvider

class KaletaArtProvider : MuzeiArtProvider() {

    override fun onLoadRequested(initial: Boolean) {
        val context = context ?: return

        KaletaWorker.enqueueLoad(context, initial)
    }

//    override fun openFile(artwork: Artwork): InputStream {
//        return super.openFile(artwork).also {
//            artwork.token?.run {
//                try {
//                    UnsplashService.trackDownload(this)
//                } catch (e: IOException) {
//                    Log.w(TAG, "Error reporting download to Unsplash", e)
//                }
//            }
//        }
//    }
}
