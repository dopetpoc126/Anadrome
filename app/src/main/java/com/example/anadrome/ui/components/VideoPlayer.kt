package com.example.anadrome.ui.components // IMPORTANT: Ensure this matches your package

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
// No direct equivalent for VideoSize import needed for this specific PlayerView usage
// import androidx.media3.common.VideoSize // This is not strictly needed for the PlayerView setup directly

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect

// --- IMPORTANT: Add the PlayerView import from androidx.media3.ui ---
import androidx.media3.ui.PlayerView // <--- ADD THIS LINE


@Composable
fun VideoPlayer(videoUri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true // Start playing automatically
            repeatMode = ExoPlayer.REPEAT_MODE_ALL // Loop the video
        }
    }

    DisposableEffect(
        AndroidView(factory = { ctx ->
            // --- IMPORTANT: Change StyledPlayerView to PlayerView ---
            PlayerView(ctx).apply { // <--- THIS LINE MUST BE PlayerView, NOT StyledPlayerView
                player = exoPlayer
                useController = false // Hide controls for a seamless preview
            }
        }, modifier = Modifier.fillMaxSize())
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}