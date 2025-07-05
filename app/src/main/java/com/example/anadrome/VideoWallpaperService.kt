package com.example.anadrome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@UnstableApi
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoWallpaperEngine()
    }

    inner class VideoWallpaperEngine : Engine() {
        private val TAG = "VideoWallpaperEngine"
        private var exoPlayer: ExoPlayer? = null
        private var powerManager: PowerManager? = null
        private var isPowerSaveMode = false
        private var currentVideoUri: String? = null

        private var respectBatterySaver = true
        private var animateContinuously = false
        private var animateOnReturn = false // New setting variable

        private val powerSaveModeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    updatePowerSaveMode()
                    if (isVisible) {
                        handlePlayerState()
                    }
                }
            }
        }

        private val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        Log.d(TAG, "Phone UNLOCKED.")
                        loadSettings()
                        exoPlayer?.repeatMode = if (animateContinuously) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF

                        checkAndUpdateVideo()
                        if (isVisible && shouldPlay()) {
                            Log.d(TAG, "Playing video after phone unlock")
                            exoPlayer?.let { player ->
                                player.seekTo(0)
                                player.play()
                            }
                        }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d(TAG, "Screen turned OFF.")
                        exoPlayer?.pause()
                        // Add this line to reset the video to the first frame
                        exoPlayer?.seekTo(0)
                    }
                }
            }
        }

        private val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && isPowerSaveMode && respectBatterySaver) {
                    exoPlayer?.pause()
                    Log.d(TAG, "Power save mode - showing first frame only")
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

            val powerFilter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            val unlockFilter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(powerSaveModeReceiver, powerFilter)
            registerReceiver(unlockReceiver, unlockFilter)

            updatePowerSaveMode()
            loadSettings()
        }

        private fun loadSettings() {
            val prefs = applicationContext.getSharedPreferences("video_wallpaper_prefs", Context.MODE_PRIVATE)
            respectBatterySaver = prefs.getBoolean("respect_battery_saver", true)
            animateContinuously = prefs.getBoolean("animate_continuously", false)
            animateOnReturn = prefs.getBoolean("animate_on_return", false) // Load new setting
            Log.d(TAG, "Settings loaded: respectBatterySaver=$respectBatterySaver, animateContinuously=$animateContinuously, animateOnReturn=$animateOnReturn")
        }

        private fun shouldPlay(): Boolean {
            if (respectBatterySaver && isPowerSaveMode) {
                return false
            }
            return true
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "Surface created.")
            initializePlayer(holder.surface)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            Log.d(TAG, "Surface destroyed.")
            releasePlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                Log.d(TAG, "Wallpaper is now VISIBLE.")
                loadSettings()

                exoPlayer?.repeatMode = if (animateContinuously) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF

                // Behavior changed to reset to the last frame instead of the first.
                exoPlayer?.let { player ->
                    val duration = player.duration
                    if (duration != C.TIME_UNSET) {
                        player.seekTo(duration)
                    }
                }

                // Play animation once on return if the setting is enabled
                if (animateOnReturn && shouldPlay()) {
                    exoPlayer?.let { player ->
                        Log.d(TAG, "Animate on return: Playing video.")
                        player.play()
                    }
                }

                checkAndUpdateVideo()
                handlePlayerState()
            } else {
                Log.d(TAG, "Wallpaper is now HIDDEN. Pausing video.")
                if (!animateContinuously) {
                    exoPlayer?.pause()
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.d(TAG, "Engine destroyed.")
            unregisterReceiver(powerSaveModeReceiver)
            unregisterReceiver(unlockReceiver)
            releasePlayer()
        }

        private fun updatePowerSaveMode() {
            isPowerSaveMode = powerManager?.isPowerSaveMode ?: false
            Log.d(TAG, "Power save mode is: ${if (isPowerSaveMode) "ON" else "OFF"}")
        }

        private fun checkAndUpdateVideo() {
            val prefs = applicationContext.getSharedPreferences("video_wallpaper_prefs", Context.MODE_PRIVATE)
            val newVideoUri = prefs.getString("video_uri", null)

            if (newVideoUri != currentVideoUri) {
                Log.d(TAG, "Video URI changed from $currentVideoUri to $newVideoUri")
                currentVideoUri = newVideoUri
                surfaceHolder?.surface?.let { surface ->
                    initializePlayer(surface)
                }
            }
        }

        private fun handlePlayerState() {
            if (!shouldPlay()) {
                Log.d(TAG, "Handling player state: Not playing due to settings.")
                exoPlayer?.let { player ->
                    player.pause()
                    player.seekTo(0)
                }
            } else {
                Log.d(TAG, "Handling player state: Ready to play.")
            }
        }

        private fun initializePlayer(surface: Surface) {
            releasePlayer()

            val prefs = applicationContext.getSharedPreferences("video_wallpaper_prefs", Context.MODE_PRIVATE)
            val uriString = prefs.getString("video_uri", null)

            if (uriString == null) {
                Log.w(TAG, "Video URI not found. Cannot play video.")
                currentVideoUri = null
                return
            }

            currentVideoUri = uriString
            val videoUri = Uri.parse(uriString)
            Log.d(TAG, "Initializing player with: $videoUri")

            try {
                exoPlayer = ExoPlayer.Builder(applicationContext).build().apply {
                    setVideoSurface(surface)
                    setMediaItem(MediaItem.fromUri(videoUri))
                    repeatMode = if (animateContinuously) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                    volume = 0f
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    playWhenReady = false
                    addListener(playerListener)
                    prepare()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ExoPlayer", e)
                currentVideoUri = null
            }
        }

        private fun releasePlayer() {
            exoPlayer?.removeListener(playerListener)
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}