package com.example.anadrome

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.anadrome.databinding.ActivityWallpaperPreviewBinding

@UnstableApi
class WallpaperPreviewActivity : AppCompatActivity() {

    private val TAG = "WallpaperPreview"
    private lateinit var binding: ActivityWallpaperPreviewBinding
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityWallpaperPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriString = intent.getStringExtra("video_uri")
        if (uriString == null) {
            Log.e(TAG, "Video URI not provided.")
            Toast.makeText(this, "Could not load video.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializePlayer(Uri.parse(uriString))
    }

    private fun initializePlayer(uri: Uri) {
        try {
            exoPlayer = ExoPlayer.Builder(this).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT

                // Add a listener to get video properties when they are ready
                addListener(playerListener)

                prepare()
                playWhenReady = true
            }
            binding.playerView.player = exoPlayer
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player", e)
            Toast.makeText(this, "Error playing video.", Toast.LENGTH_SHORT).show()
        }
    }

    // This new listener will react when the video size is known
    private val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.height == 0) {
                // Avoid division by zero
                return
            }

            // Calculate the aspect ratio of the video
            val aspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()

            // Apply this aspect ratio to the PlayerView using ConstraintSet
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.root)
            constraintSet.setDimensionRatio(binding.playerView.id, aspectRatio.toString())
            constraintSet.applyTo(binding.root)
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to remove the listener before releasing the player
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
    }
}