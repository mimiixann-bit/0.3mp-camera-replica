package com.yourco.grainshot.ui.frame

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yourco.grainshot.data.Frame
import com.yourco.grainshot.data.Photo
import com.yourco.grainshot.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FrameEditorScreen(
    photo: Photo,
    frames: List<Frame>,
    onApplied: () -> Unit,
    onCancel: () -> Unit,
) {
    var selectedFrameId by remember { mutableStateOf<Long?>(null) }
    var applying by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(ColorBackground)) {
        // Preview
        Box(Modifier.fillMaxWidth().weight(1f).background(ColorSurface),
            contentAlignment = Alignment.Center) {
            AsyncImage(model = photo.mediaStoreUri, contentDescription = null,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            selectedFrameId?.let { fid ->
                val frame = frames.firstOrNull { it.id == fid }
                frame?.let { FrameOverlay(it) }
            }
        }
        // Picker
        Row(
            Modifier.fillMaxWidth().height(80.dp).background(ColorSurfaceHigh)
                .horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            frames.forEach { frame ->
                val selected = frame.id == selectedFrameId
                Box(
                    Modifier.size(56.dp).background(ColorSurface)
                        .border(2.dp, if (selected) ColorAccent else ColorSurfaceHigh)
                        .clickable { selectedFrameId = frame.id },
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = remember(frame.assetPath) {
                        runCatching { context.assets.open(frame.assetPath)
                            .use { BitmapFactory.decodeStream(it) } }.getOrNull()
                    }
                    bmp?.let { Image(it.asImageBitmap(), frame.name,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
            }
        }
        // Actions
        Row(Modifier.fillMaxWidth().height(56.dp).background(ColorSurfaceHigh),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Text("CANCEL", color = ColorOnSurfaceDim, fontFamily = ShareTechMono,
                fontSize = 12.sp, letterSpacing = 0.08.em,
                modifier = Modifier.clickable(onClick = onCancel))
            Text(if (applying) "..." else "APPLY",
                color = if (selectedFrameId != null) ColorAccent else ColorOnSurfaceDim,
                fontFamily = ShareTechMono, fontSize = 12.sp, letterSpacing = 0.08.em,
                modifier = Modifier.clickable(enabled = selectedFrameId != null && !applying) {
                    val fid = selectedFrameId ?: return@clickable
                    val frame = frames.firstOrNull { it.id == fid } ?: return@clickable
                    applying = true
                    // Apply is handled via LaunchedEffect below
                })
        }
        if (applying) {
            val fid = selectedFrameId
            val frame = frames.firstOrNull { it?.id == fid }
            if (frame != null) {
                LaunchedEffect(fid) {
                    withContext(Dispatchers.IO) {
                        applyFrameAndSave(context, photo, frame)
                    }
                    applying = false
                    onApplied()
                }
            }
        }
    }
}

@Composable
fun FrameOverlay(frame: Frame) {
    val ctx = LocalContext.current
    val bmp = remember(frame.assetPath) {
        runCatching { ctx.assets.open(frame.assetPath)
            .use { BitmapFactory.decodeStream(it) } }.getOrNull()
    }
    bmp?.let { Image(it.asImageBitmap(), null,
        contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize()) }
}

private fun applyFrameAndSave(context: Context, photo: Photo, frame: Frame) {
    val base = context.contentResolver.openInputStream(
        android.net.Uri.parse(photo.mediaStoreUri))?.use { BitmapFactory.decodeStream(it) }
        ?: return
    val frameBmp = context.assets.open(frame.assetPath).use { BitmapFactory.decodeStream(it) }
        ?: return
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    Canvas(out).drawBitmap(
        Bitmap.createScaledBitmap(frameBmp, out.width, out.height, false), 0f, 0f,
        Paint(Paint.FILTER_BITMAP_FLAG))
    val cv = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "grainshot_framed_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Grainshot")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), cv) ?: return
    context.contentResolver.openOutputStream(uri)?.use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val upd = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        context.contentResolver.update(uri, upd, null, null)
    }
}
