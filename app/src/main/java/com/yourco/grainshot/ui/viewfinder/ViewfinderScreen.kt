package com.yourco.grainshot.ui.viewfinder

import android.app.Activity
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourco.grainshot.camera.CameraViewModel
import com.yourco.grainshot.camera.CaptureMode
import com.yourco.grainshot.camera.RecordingState
import com.yourco.grainshot.camera.VideoManager
import com.yourco.grainshot.ui.paywall.PaywallScreen
import com.yourco.grainshot.ui.theme.*

@Composable
fun ViewfinderScreen(
    onGalleryClick: () -> Unit,
    vm: CameraViewModel = viewModel(),
    videoManager: VideoManager? = null,
) {
    val camState    by vm.camState.collectAsState()
    val billing     by vm.billingState.collectAsState()
    val showPaywall by vm.showPaywall.collectAsState()
    val videoState  by (videoManager?.state?.collectAsState()
        ?: remember { mutableStateOf(null) })
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as Activity

    Box(Modifier.fillMaxSize().background(ColorBackground)) {

        // ── Viewfinder ───────────────────────────────────────────────────
        AndroidView(
            factory  = { ctx -> PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }},
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ -> vm.onZoomChange(zoom) }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { vm.onDoubleTap() })
                },
            update   = { view -> vm.bindCamera(lifecycleOwner, view) }
        )

        // ── Capture flash overlay ────────────────────────────────────────
        if (camState.showCaptureFlash) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))
        }

        // ── Flash dot ────────────────────────────────────────────────────
        if (camState.flashOn) {
            Box(Modifier.padding(12.dp).size(8.dp)
                .background(ColorAccent).align(Alignment.TopStart))
        }

        // ── Recording indicator ──────────────────────────────────────────
        val recState = videoState?.recordingState
        if (recState == RecordingState.RECORDING) {
            RecordingIndicator(
                durationMs = videoState?.durationMs ?: 0L,
                modifier   = Modifier.align(Alignment.TopStart),
            )
        }

        // ── Shot counter ─────────────────────────────────────────────────
        if (!billing.isSubscribed && camState.captureMode == CaptureMode.PHOTO) {
            Text(
                text          = "%02d / 10".format(billing.shotCount),
                color         = ColorOnBackground,
                fontFamily    = ShareTechMono,
                fontSize      = 11.sp,
                letterSpacing = 0.05.em,
                modifier      = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )
        }

        // ── Bottom bar ───────────────────────────────────────────────────
        BottomBar(
            captureMode  = camState.captureMode,
            recordingState = recState ?: RecordingState.IDLE,
            onShutter    = {
                if (camState.captureMode == CaptureMode.PHOTO) {
                    vm.onShutterTap {}
                } else {
                    if (recState == RecordingState.RECORDING) videoManager?.stopRecording()
                    else videoManager?.startRecording()
                }
            },
            onModeToggle = { vm.toggleMode() },
            onGallery    = onGalleryClick,
            modifier     = Modifier.align(Alignment.BottomCenter),
        )

        // ── Paywall ──────────────────────────────────────────────────────
        if (showPaywall) {
            PaywallScreen(
                price       = billing.price,
                pending     = billing.pending,
                error       = billing.error,
                onSubscribe = { vm.onSubscribeTap(activity) },
                onDismiss   = { vm.onPaywallDismiss() },
            )
        }
    }
}

@Composable
fun RecordingIndicator(durationMs: Long, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "blink")
    val alpha by transition.animateFloat(1f, 0f,
        infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha")
    Row(modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(ColorAccent.copy(alpha = alpha)))
        val s = (durationMs / 1000).toInt()
        Text("%02d:%02d".format(s / 60, s % 60), color = ColorOnBackground,
            fontFamily = ShareTechMono, fontSize = 11.sp, letterSpacing = 0.05.em)
    }
}
