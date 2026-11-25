package com.yourco.grainshot.ui.gallery

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.yourco.grainshot.data.GalleryRepository
import com.yourco.grainshot.data.Photo
import com.yourco.grainshot.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────

data class GalleryUiState(val photos: List<Photo> = emptyList(), val loading: Boolean = true)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GalleryRepository(app)

    val state: StateFlow<GalleryUiState> = repo.observePhotos()
        .map { GalleryUiState(it, false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState())

    fun delete(photo: Photo) {
        viewModelScope.launch {
            repo.delete(photo)
            getApplication<Application>().contentResolver
                .delete(Uri.parse(photo.mediaStoreUri), null, null)
        }
    }
}

// ── Gallery screen (S05) ─────────────────────────────────────────────────

@Composable
fun GalleryScreen(
    onPhotoClick: (Long) -> Unit,
    vm: GalleryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val state by vm.state.collectAsState()
    Box(Modifier.fillMaxSize().background(ColorBackground)) {
        when {
            state.loading -> Unit
            state.photos.isEmpty() ->
                Text("NO PHOTOS", color = ColorOnSurfaceDim, fontFamily = ShareTechMono,
                    fontSize = 11.sp, letterSpacing = 0.08.em,
                    modifier = Modifier.align(Alignment.Center))
            else -> LazyVerticalGrid(
                columns             = GridCells.Fixed(2),
                modifier            = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement   = Arrangement.spacedBy(2.dp),
                contentPadding        = PaddingValues(0.dp),
            ) {
                items(state.photos, key = { it.id }) { photo ->
                    Box(Modifier.aspectRatio(1f).background(ColorSurface)
                        .clickable { onPhotoClick(photo.id) }) {
                        AsyncImage(model = photo.mediaStoreUri, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

// ── Photo viewer (S06) ───────────────────────────────────────────────────

@Composable
fun PhotoViewerScreen(
    photo: Photo,
    onDelete: (Photo) -> Unit,
    onShare:  (Photo) -> Unit,
    onAddFrame: (Photo) -> Unit,
    onBack:   () -> Unit,
) {
    var showOptions by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(ColorBackground)) {
        AsyncImage(model = photo.mediaStoreUri, contentDescription = null,
            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp)
                .background(ColorSurfaceHigh).align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack)              { Icon(Icons.Outlined.ArrowBack,  "Back",    tint = ColorOnSurfaceDim) }
            IconButton(onClick = { onShare(photo) })  { Icon(Icons.Outlined.Share,      "Share",   tint = ColorOnSurfaceDim) }
            IconButton(onClick = { showOptions=true }){ Icon(Icons.Outlined.MoreHoriz,  "Options", tint = ColorOnSurfaceDim) }
            IconButton(onClick = { onDelete(photo) }) { Icon(Icons.Outlined.Delete,     "Delete",  tint = ColorOnSurfaceDim) }
        }
    }
    if (showOptions) {
        OptionsMenu(
            onDismiss  = { showOptions = false },
            onAddFrame = { onAddFrame(photo); showOptions = false },
            onShare    = { onShare(photo);    showOptions = false },
            onDelete   = { onDelete(photo);   showOptions = false },
        )
    }
}

// ── Options menu (S07) ───────────────────────────────────────────────────

@Composable
fun OptionsMenu(
    onDismiss:  () -> Unit,
    onAddFrame: () -> Unit,
    onShare:    () -> Unit,
    onDelete:   () -> Unit,
) {
    androidx.activity.compose.BackHandler { onDismiss() }
    Box(Modifier.fillMaxSize().background(ColorScrim).clickable { onDismiss() }) {
        Column(
            Modifier.fillMaxWidth().background(ColorSurface).align(Alignment.BottomCenter)
        ) {
            listOf("ADD FRAME" to onAddFrame, "SHARE" to onShare, "DELETE" to onDelete)
                .forEachIndexed { i, (label, action) ->
                    Text(label, color = ColorOnBackground, fontFamily = ShareTechMono,
                        fontSize = 14.sp, letterSpacing = 0.08.em,
                        modifier = Modifier.fillMaxWidth().clickable { action() }
                            .padding(horizontal = 16.dp, vertical = 16.dp))
                    if (i < 2) HorizontalDivider(color = ColorSurfaceHigh, thickness = 1.dp)
                }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}
