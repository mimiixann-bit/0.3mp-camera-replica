package com.yourco.grainshot.ui.import_

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.yourco.grainshot.camera.BitmapFilter
import com.yourco.grainshot.data.GalleryRepository
import com.yourco.grainshot.data.PhotoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportModule(private val context: Context) {
    private val repo = GalleryRepository(context)

    suspend fun processImport(uri: Uri, zoomLevel: Float = 1f): Long? =
        withContext(Dispatchers.IO) {
            try {
                val raw = context.contentResolver.openInputStream(uri)
                    ?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
                val scaled   = scaleToBound(raw, 2048)
                val filtered = BitmapFilter.apply(scaled, zoomLevel, false, System.currentTimeMillis())
                val outUri   = saveToMediaStore(filtered) ?: return@withContext null
                repo.save(outUri, PhotoSource.IMPORT, zoom = zoomLevel)
            } catch (e: Exception) { null }
        }

    private fun scaleToBound(src: Bitmap, max: Int): Bitmap {
        val scale = max.toFloat() / maxOf(src.width, src.height)
        return if (scale < 1f) Bitmap.createScaledBitmap(src,
            (src.width*scale).toInt(), (src.height*scale).toInt(), false) else src
    }

    private fun saveToMediaStore(bmp: Bitmap): Uri? {
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "grainshot_import_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Grainshot")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), cv) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.update(uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }
        return uri
    }
}
