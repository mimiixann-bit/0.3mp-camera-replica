package com.yourco.grainshot.share

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareModule {
    fun sharePhoto(context: Context, uri: Uri) {
        context.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, null))
    }

    fun shareVideo(context: Context, uri: Uri) {
        context.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, null))
    }
}
