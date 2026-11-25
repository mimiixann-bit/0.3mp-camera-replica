package com.yourco.grainshot

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.yourco.grainshot.camera.CameraViewModel
import com.yourco.grainshot.data.AppDatabase
import com.yourco.grainshot.share.ShareModule
import com.yourco.grainshot.ui.frame.FrameEditorScreen
import com.yourco.grainshot.ui.gallery.GalleryScreen
import com.yourco.grainshot.ui.gallery.GalleryViewModel
import com.yourco.grainshot.ui.gallery.PhotoViewerScreen
import com.yourco.grainshot.ui.import_.ImportModule
import com.yourco.grainshot.ui.theme.*
import com.yourco.grainshot.ui.viewfinder.ViewfinderScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GrainshotTheme { GrainshotApp() } }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GrainshotApp() {
    val permissions = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    val permState = rememberMultiplePermissionsState(permissions)
    if (!permState.allPermissionsGranted) {
        PermissionScreen { permState.launchMultiplePermissionRequest() }
    } else {
        GrainshotNavHost()
    }
}

@Composable
fun GrainshotNavHost() {
    val navController = rememberNavController()
    val cameraVm: CameraViewModel = viewModel()
    val galleryVm: GalleryViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { scope.launch { ImportModule(context).processImport(it) } }
    }

    NavHost(navController, startDestination = "viewfinder") {

        composable("viewfinder") {
            ViewfinderScreen(
                onGalleryClick = { navController.navigate("gallery") },
                vm             = cameraVm,
            )
        }

        composable("gallery") {
            GalleryScreen(
                onPhotoClick = { id -> navController.navigate("photo/$id") },
                vm           = galleryVm,
            )
        }

        composable("photo/{id}") { back ->
            val id    = back.arguments?.getString("id")?.toLongOrNull() ?: return@composable
            val state by galleryVm.state.collectAsState()
            val photo = state.photos.firstOrNull { it.id == id } ?: return@composable
            PhotoViewerScreen(
                photo      = photo,
                onDelete   = { galleryVm.delete(it); navController.popBackStack() },
                onShare    = { ShareModule.sharePhoto(context, Uri.parse(it.mediaStoreUri)) },
                onAddFrame = { navController.navigate("frame/$id") },
                onBack     = { navController.popBackStack() },
            )
        }

        composable("frame/{id}") { back ->
            val id    = back.arguments?.getString("id")?.toLongOrNull() ?: return@composable
            val state by galleryVm.state.collectAsState()
            val photo = state.photos.firstOrNull { it.id == id } ?: return@composable
            var frames by remember { mutableStateOf(emptyList<com.yourco.grainshot.data.Frame>()) }
            LaunchedEffect(Unit) {
                frames = AppDatabase.get(context).frameDao().getAll()
            }
            FrameEditorScreen(
                photo    = photo,
                frames   = frames,
                onApplied = { navController.popBackStack() },
                onCancel  = { navController.popBackStack() },
            )
        }
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize().background(ColorBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CAMERA ACCESS NEEDED", color = ColorOnBackground, fontFamily = ShareTechMono,
                fontSize = 14.sp, letterSpacing = 0.08.em)
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.width(200.dp).height(44.dp).background(ColorAccent)
                    .clickable(onClick = onRequest),
                contentAlignment = Alignment.Center,
            ) {
                Text("ALLOW", color = Color.White, fontFamily = ShareTechMono,
                    fontSize = 12.sp, letterSpacing = 0.08.em)
            }
        }
    }
}
