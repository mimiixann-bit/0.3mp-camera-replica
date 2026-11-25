package com.yourco.grainshot.camera

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import com.yourco.grainshot.data.Video
import com.yourco.grainshot.data.VideoDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RecordingState { IDLE, RECORDING, SAVING }

data class VideoUiState(
    val recordingState: RecordingState = RecordingState.IDLE,
    val durationMs: Long = 0L,
)

class VideoManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val videoDao: VideoDao,
) {
    private val _state = MutableStateFlow(VideoUiState())
    val state: StateFlow<VideoUiState> = _state.asStateFlow()

    private var recording: Recording? = null
    private var startMs: Long = 0L

    val videoCaptureUseCase: VideoCapture<Recorder> by lazy {
        VideoCapture.withOutput(
            Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.SD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                .build()
        )
    }

    fun startRecording() {
        if (_state.value.recordingState != RecordingState.IDLE) return
        val cv = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "grainshot_vid_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Grainshot")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val output = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        ).setContentValues(cv).build()

        startMs = System.currentTimeMillis()
        _state.value = _state.value.copy(recordingState = RecordingState.RECORDING)

        recording = videoCaptureUseCase.output
            .prepareRecording(context, output)
            .apply {
                if (ContextCompat.checkSelfPermission(context,
                        android.Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED) withAudioEnabled()
            }
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Status ->
                        _state.value = _state.value.copy(
                            durationMs = System.currentTimeMillis() - startMs)
                    is VideoRecordEvent.Finalize ->
                        scope.launch { onFinalize(event) }
                    else -> Unit
                }
            }
    }

    fun stopRecording() {
        if (_state.value.recordingState != RecordingState.RECORDING) return
        _state.value = _state.value.copy(recordingState = RecordingState.SAVING)
        recording?.stop(); recording = null
    }

    private suspend fun onFinalize(event: VideoRecordEvent.Finalize) {
        val duration = System.currentTimeMillis() - startMs
        if (!event.hasError()) {
            val uri = event.outputResults.outputUri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val upd = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                context.contentResolver.update(uri, upd, null, null)
            }
            videoDao.insert(Video(mediaStoreUri = uri.toString(),
                recordedAt = System.currentTimeMillis(), durationMs = duration))
        }
        _state.value = VideoUiState()
    }
}
