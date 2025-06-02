package com.example.livewallpaper
import androidx.compose.ui.draw.clip

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.* // Import for remember, mutableStateOf
import androidx.compose.ui.viewinterop.AndroidView // Import for AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner // Import for LocalLifecycleOwner

// ExoPlayer imports
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

// Make sure this import path matches your theme file location
import com.example.livewallpaper.ui.theme.LiveWallpaperTheme // Adjust if your theme package is different

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveWallpaperTheme { // Apply your app's theme for Material You styling
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WallpaperSetupScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSetupScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State to control the visibility of the video preview
    var showVideoPreview by remember { mutableStateOf(false) }

    // IMPORTANT: These *must* match your live wallpaper service's package and class name.
    val liveWallpaperPackageName = context.packageName // Gets the current app's package name
    val liveWallpaperServiceClassName = "com.example.livewallpaper.VideoWallpaperService" // Full path to your service

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Primus") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Button to apply the live wallpaper
            Button(
                onClick = {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    intent.putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(liveWallpaperPackageName, liveWallpaperServiceClassName)
                    )
                    // Check if the intent can be resolved to prevent crashes on some devices
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        // Fallback: If direct intent fails, try opening settings
                        val settingsIntent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                        if (settingsIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(settingsIntent)
                            Toast.makeText(context, "Couldn't open live wallpaper picker directly. Trying display settings.", Toast.LENGTH_LONG).show()
                        } else {
                            // As a last resort, inform the user with a Toast
                            Toast.makeText(context, "Could not open wallpaper settings. Please set it manually.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
            ) {
                Text("Apply Primus Wallpaper", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp)) // Spacer between buttons

            // Button to toggle the video preview
            OutlinedButton(
                onClick = {
                    showVideoPreview = !showVideoPreview // Toggle visibility
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
            ) {
                Text(
                    if (showVideoPreview) "Hide Preview" else "Preview Wallpaper",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // NEW: Video Player Composable, shown conditionally
            if (showVideoPreview) {
                Spacer(modifier = Modifier.height(24.dp))
                VideoPlayerComposable(
                    videoResId = R.raw.my_wallpaper_video, // Reference to your video in res/raw
                    lifecycleOwner = lifecycleOwner,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(16f / 9f) // Maintain aspect ratio (e.g., 16:9)
                        .clip(MaterialTheme.shapes.medium) // Apply rounded corners
                )
            }
        }
    }
}

/**
 * Composable function to play a video using ExoPlayer within the app.
 *
 * @param videoResId The resource ID of the video (e.g., R.raw.my_wallpaper_video).
 * @param lifecycleOwner The LifecycleOwner to observe lifecycle events for player management.
 * @param modifier Modifier for layout and styling of the video player.
 */
@Composable
fun VideoPlayerComposable(
    videoResId: Int,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/$videoResId")
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            volume = 0f // Mute by default for wallpaper preview
            repeatMode = ExoPlayer.REPEAT_MODE_ALL // Loop the video
        }
    }

    // Manage ExoPlayer lifecycle with the Composable's lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Start playing when the Composable is visible (e.g., app in foreground)
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                }
                Lifecycle.Event.ON_STOP -> {
                    // Pause when the Composable is no longer visible (e.g., app in background)
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    // Release player resources when the Composable is destroyed
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release() // Ensure player is released on dispose
        }
    }

    // Host the ExoPlayer's PlayerView in Compose
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Hide controls for a seamless preview
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun DefaultPreview() {
    LiveWallpaperTheme { // Use your actual theme name here
        WallpaperSetupScreen()
    }
}
