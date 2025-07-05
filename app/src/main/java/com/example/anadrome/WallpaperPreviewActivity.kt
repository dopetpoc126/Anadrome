package com.example.anadrome

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.anadrome.databinding.ActivityWallpaperPreviewBinding

class WallpaperPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperPreviewBinding
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullscreen()
        binding = ActivityWallpaperPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.decorView.keepScreenOn = true
    }

    private fun initializePlayer() {
        val videoUri = intent.getStringExtra("video_uri") ?: return

        exoPlayer = ExoPlayer.Builder(this)
            .build()
            .also { player ->
                binding.playerView.player = player
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
                player.setMediaItem(mediaItem)
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.volume = 0f
                player.playWhenReady = true
                player.prepare()
            }
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    public override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    public override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}