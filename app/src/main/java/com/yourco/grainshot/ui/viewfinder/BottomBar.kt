package com.yourco.grainshot.ui.viewfinder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourco.grainshot.camera.CaptureMode
import com.yourco.grainshot.camera.RecordingState
import com.yourco.grainshot.ui.theme.*

@Composable
fun BottomBar(
    captureMode: CaptureMode,
    recordingState: RecordingState,
    onShutter: () -> Unit,
    onModeToggle: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(64.dp).background(ColorSurfaceHigh),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Mode toggle — camera / video
        IconButton(onClick = onModeToggle, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector        = if (captureMode == CaptureMode.PHOTO)
                    Icons.Outlined.Videocam else Icons.Outlined.CameraAlt,
                contentDescription = "Toggle mode",
                tint               = ColorOnSurfaceDim,
                modifier           = Modifier.size(22.dp),
            )
        }

        // Shutter / record button
        val isRecording = recordingState == RecordingState.RECORDING
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (isRecording) ColorAccentDim else ColorAccent)
                .clickable(onClick = onShutter),
            contentAlignment = Alignment.Center,
        ) {
            if (isRecording) {
                // Stop: filled square
                Box(Modifier.size(20.dp).background(Color.White))
            } else {
                // Shutter: bordered square
                Box(Modifier.size(32.dp).border(2.dp, ColorBackground).background(ColorAccent))
            }
        }

        // Gallery
        IconButton(onClick = onGallery, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector        = Icons.Outlined.PhotoLibrary,
                contentDescription = "Gallery",
                tint               = ColorOnSurfaceDim,
                modifier           = Modifier.size(22.dp),
            )
        }
    }
}
