package com.yourco.grainshot.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat

class CaptureManager(private val context: Context) {

    fun capture(imageCapture: ImageCapture, onResult: (Uri?) -> Unit) {
        val filename = "grainshot_${System.currentTimeMillis()}.jpg"
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Grainshot")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), cv
        ) ?: run { onResult(null); return }

        val opts = ImageCapture.OutputFileOptions
            .Builder(context.contentResolver, uri, cv).build()

        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val upd = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                        context.contentResolver.update(uri, upd, null, null)
                    }
                    onResult(uri)
                }
                override fun onError(exc: ImageCaptureException) { onResult(null) }
            })
    }
}
