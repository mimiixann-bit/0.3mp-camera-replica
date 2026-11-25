package com.yourco.grainshot.camera

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.SystemClock
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.yourco.grainshot.billing.BillingManager
import com.yourco.grainshot.billing.BillingUiState
import com.yourco.grainshot.data.BillingRepository
import com.yourco.grainshot.data.GalleryRepository
import com.yourco.grainshot.data.PhotoSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CaptureMode { PHOTO, VIDEO }

data class CameraUiState(
    val zoomLevel: Float    = 1f,
    val flashOn: Boolean    = false,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val showCaptureFlash: Boolean = false,
)

class CameraViewModel(app: Application) : AndroidViewModel(app) {

    private val billingRepo = BillingRepository(app)
    private val galleryRepo = GalleryRepository(app)
    private val captureManager = CaptureManager(app)

    val billingState: StateFlow<BillingUiState> = billingRepo.observeState()
        .map { s ->
            BillingUiState(
                isSubscribed = s?.isSubscribed ?: false,
                shotCount    = s?.shotCount    ?: 0,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillingUiState())

    private val _camState  = MutableStateFlow(CameraUiState())
    val camState: StateFlow<CameraUiState> = _camState.asStateFlow()

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    lateinit var billingManager: BillingManager
    private var imageCapture: ImageCapture? = null

    init {
        viewModelScope.launch { billingRepo.ensureRow() }
        billingManager = BillingManager(
            context           = app,
            scope             = viewModelScope,
            onPurchaseGranted = { token, expiry ->
                billingRepo.markSubscribed(token, expiry)
                _showPaywall.value = false
            },
            onPurchaseRevoked = { billingRepo.markUnsubscribed() },
        )
        billingManager.connect()
    }

    fun bindCamera(owner: LifecycleOwner, previewView: PreviewView) {
        val ctx = getApplication<Application>()
        ProcessCameraProvider.getInstance(ctx).addListener({
            val provider = ProcessCameraProvider.getInstance(ctx).get()
            val preview  = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                viewModelScope.launch {
                    camState.collect { s ->
                        previewView.setRenderEffect(
                            FilterRenderer.buildRenderEffect(
                                width   = previewView.width.coerceAtLeast(1),
                                height  = previewView.height.coerceAtLeast(1),
                                zoom    = s.zoomLevel,
                                flashOn = s.flashOn,
                                timeMs  = SystemClock.elapsedRealtime(),
                            )
                        )
                    }
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA,
                preview, imageCapture)
        }, ContextCompat.getMainExecutor(ctx))
    }

    fun onZoomChange(scale: Float) {
        _camState.value = _camState.value.copy(
            zoomLevel = (_camState.value.zoomLevel * scale).coerceIn(1f, 8f))
    }

    fun onDoubleTap() {
        _camState.value = _camState.value.copy(flashOn = !_camState.value.flashOn)
    }

    fun toggleMode() {
        _camState.value = _camState.value.copy(
            captureMode = if (_camState.value.captureMode == CaptureMode.PHOTO)
                CaptureMode.VIDEO else CaptureMode.PHOTO)
    }

    fun onShutterTap(onCapture: () -> Unit) {
        viewModelScope.launch {
            if (billingRepo.isPaywallDue()) { _showPaywall.value = true; return@launch }
            val cap = imageCapture ?: return@launch
            captureManager.capture(cap) { uri ->
                if (uri != null) {
                    viewModelScope.launch {
                        billingRepo.incrementShot()
                        galleryRepo.save(uri, PhotoSource.CAMERA,
                            zoom    = _camState.value.zoomLevel,
                            flashOn = _camState.value.flashOn)
                        _camState.value = _camState.value.copy(showCaptureFlash = true)
                        kotlinx.coroutines.delay(80)
                        _camState.value = _camState.value.copy(showCaptureFlash = false)
                        if (billingRepo.isPaywallDue()) _showPaywall.value = true
                    }
                }
            }
        }
    }

    fun onSubscribeTap(activity: Activity) = billingManager.launchBillingFlow(activity)
    fun onPaywallDismiss() { _showPaywall.value = false }

    override fun onCleared() { super.onCleared(); billingManager.disconnect() }
}
