package com.example.livewallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.util.Log
import androidx.media3.common.C

// --- IMPORTANT: Corrected ExoPlayer imports for Media3 ---
import androidx.media3.exoplayer.ExoPlayer // Changed from com.google.android.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem // Changed from com.google.android.exoplayer.MediaItem
import androidx.media3.common.Player // Changed from com.google.android.exoplayer.Player
import androidx.media3.common.VideoSize // Changed from com.google.android.exoplayer.video.VideoSize
// No direct equivalent for com.google.android.exoplayer.C, use Player.VIDEO_SCALING_MODE_... directly

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoWallpaperEngine()
    }

    inner class VideoWallpaperEngine : Engine(), Player.Listener {

        private var exoPlayer: ExoPlayer? = null
        private var isVisible: Boolean = false
        private var isBatterySaverOn: Boolean = false
        private var isPhoneUnlocked: Boolean = false

        // For the visual trick: static first frame image and a flag
        private var firstFrameBitmap: Bitmap? = null
        private var hasExoPlayerRenderedFirstFrame: Boolean = false // True once ExoPlayer starts drawing

        // Handler (retained, though direct play is prioritized)
        private val handler = Handler(Looper.getMainLooper())

        private val TAG = "VideoWallpaper"

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        Log.d(TAG, "Broadcast: ACTION_USER_PRESENT (Phone Unlocked)")
                        isPhoneUnlocked = true
                        updateVideoPlayback()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d(TAG, "Broadcast: ACTION_SCREEN_OFF (Screen Off/Locked)")
                        isPhoneUnlocked = false
                        exoPlayer?.let { player ->
                            player.seekTo(0) // Ensure lock screen shows first frame
                            player.playWhenReady = false // Stop playback, but keep player prepared at 0
                            Log.d(TAG, "Screen OFF logic: Seeked to 0, playWhenReady = false.")
                        }
                        // Reset the flag so the static image is drawn again on next unlock
                        hasExoPlayerRenderedFirstFrame = false
                        Log.d(TAG, "hasExoPlayerRenderedFirstFrame reset to false on SCREEN_OFF.")
                        updateVideoPlayback() // Re-evaluate state
                    }
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                        Log.d(TAG, "Broadcast: ACTION_POWER_SAVE_MODE_CHANGED")
                        checkBatterySaverMode()
                        updateVideoPlayback()
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            Log.d(TAG, "VideoWallpaperEngine onCreate.")

            // Load the static first frame image ONCE when the engine is created
            try {
                // Ensure you have a 'first_frame.png' in your res/drawable folder
                firstFrameBitmap = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.first_frame)
                if (firstFrameBitmap != null) {
                    Log.d(TAG, "first_frame.png loaded successfully. Dimensions: ${firstFrameBitmap?.width}x${firstFrameBitmap?.height}")
                } else {
                    Log.e(TAG, "first_frame.png loaded as null. Check if the file exists and is a valid image in res/drawable.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR: Exception loading first_frame.png: ${e.message}", e)
                firstFrameBitmap = null
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
            registerReceiver(receiver, filter)

            checkBatterySaverMode()
            isPhoneUnlocked = false // Assume locked initially
        }

        private fun checkBatterySaverMode() {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            isBatterySaverOn = powerManager.isPowerSaveMode
            Log.d(TAG, "Battery Saver Mode: $isBatterySaverOn")
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "onSurfaceCreated.")
            initializePlayer(holder)
            updateVideoPlayback()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "onSurfaceChanged: ${width}x${height}")
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            Log.d(TAG, "onVisibilityChanged: $isVisible")
            updateVideoPlayback()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isVisible = false
            Log.d(TAG, "onSurfaceDestroyed.")
            releasePlayer()
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.d(TAG, "VideoWallpaperEngine onDestroy.")
            unregisterReceiver(receiver)
            releasePlayer()
        }
        @androidx.media3.common.util.UnstableApi
        private fun initializePlayer(holder: SurfaceHolder) {
            if (exoPlayer == null) {
                Log.d(TAG, "Initializing ExoPlayer.")
                exoPlayer = ExoPlayer.Builder(applicationContext).build().apply {
                    setVideoSurface(holder.surface)

                    // Ensure 'my_wallpaper_video' is in your res/raw folder
                    val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.my_wallpaper_video)
                    val mediaItem = MediaItem.fromUri(videoUri)
                    setMediaItem(mediaItem)

                    repeatMode = Player.REPEAT_MODE_OFF // Use Player.REPEAT_MODE_OFF from androidx.media3.common.Player

                    // --- IMPORTANT: Use Player.VIDEO_SCALING_MODE_... from androidx.media3.common.Player ---
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

                    volume = 0f // Mute audio

                    addListener(this@VideoWallpaperEngine)
                    prepare()
                    Log.d(TAG, "ExoPlayer prepared.")
                }
            }
        }

        private fun releasePlayer() {
            Log.d(TAG, "Releasing ExoPlayer and bitmap.")
            firstFrameBitmap?.recycle() // Release bitmap memory
            firstFrameBitmap = null
            exoPlayer?.release()
            exoPlayer = null
        }

        // --- Core Logic: Update Video Playback State ---
        private fun updateVideoPlayback() {
            val shouldPlay = isVisible && isPhoneUnlocked && !isBatterySaverOn

            Log.d(TAG, "updateVideoPlayback called. Conditions: isVisible=$isVisible, isPhoneUnlocked=$isPhoneUnlocked, isBatterySaverOn=$isBatterySaverOn -> shouldPlay=$shouldPlay")
            Log.d(TAG, "Current hasExoPlayerRenderedFirstFrame: $hasExoPlayerRenderedFirstFrame, firstFrameBitmap is null: ${firstFrameBitmap == null}")

            exoPlayer?.let { player ->
                if (shouldPlay) {
                    // --- Draw static image if ExoPlayer hasn't rendered its first frame yet ---
                    if (!hasExoPlayerRenderedFirstFrame && firstFrameBitmap != null) {
                        Log.d(TAG, "Attempting to draw static first_frame.png.")
                        val canvas: Canvas? = surfaceHolder.lockCanvas()
                        if (canvas != null) {
                            try {
                                canvas.drawColor(Color.BLACK) // Clear previous content
                                val srcRect = Rect(0, 0, firstFrameBitmap!!.width, firstFrameBitmap!!.height)
                                val destRect = Rect(0, 0, canvas.width, canvas.height)
                                canvas.drawBitmap(firstFrameBitmap!!, srcRect, destRect, null)
                                Log.d(TAG, "Successfully drew static first_frame.png on surface.")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error drawing static bitmap: ${e.message}", e)
                            } finally {
                                surfaceHolder.unlockCanvasAndPost(canvas)
                            }
                        } else {
                            Log.e(TAG, "surfaceHolder.lockCanvas() returned null when trying to draw static image.")
                        }
                    } else if (hasExoPlayerRenderedFirstFrame) {
                        Log.d(TAG, "Skipped drawing static image: ExoPlayer already rendered its first frame.")
                    } else if (firstFrameBitmap == null) {
                        Log.e(TAG, "Skipped drawing static image: firstFrameBitmap is null. Check image file.")
                    }


                    // Only seek to 0 if the video has already finished (STATE_ENDED)
                    // or if player is idle and not at the beginning (implies a need to reset).
                    if (player.playbackState == Player.STATE_ENDED) {
                        player.seekTo(0)
                        Log.d(TAG, "Seeked to 0 because video ended.")
                    } else if (player.playbackState == Player.STATE_IDLE && player.currentPosition != 0L) {
                        player.seekTo(0)
                        Log.d(TAG, "Seeked to 0 because player was idle and not at start.")
                    } else {
                        Log.d(TAG, "No seek required. Current position: ${player.currentPosition}, state: ${player.playbackState}")
                    }

                    player.playWhenReady = true
                    player.play()
                    Log.d(TAG, "Commanded player to play directly (playWhenReady=true, play()).")

                } else {
                    player.playWhenReady = false
                    if (player.isPlaying) {
                        player.pause()
                        Log.d(TAG, "Player paused.")
                    } else {
                        Log.d(TAG, "Player not playing, so no explicit pause needed.")
                    }
                    hasExoPlayerRenderedFirstFrame = false // Reset flag when wallpaper is not playing
                    Log.d(TAG, "hasExoPlayerRenderedFirstFrame reset to false on shouldPlay=false.")
                }
            } ?: run { Log.d(TAG, "ExoPlayer is null in updateVideoPlayback, skipping.") }
        }

        // --- Player.Listener Callbacks ---

        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                Player.STATE_IDLE -> "STATE_IDLE"
                Player.STATE_BUFFERING -> "STATE_BUFFERING"
                Player.STATE_READY -> "STATE_READY"
                Player.STATE_ENDED -> "STATE_ENDED"
                else -> "UNKNOWN_STATE"
            }
            Log.d(TAG, "ExoPlayer state changed: playWhenReady=${exoPlayer?.playWhenReady}, playbackState=$stateString, currentPosition=${exoPlayer?.currentPosition}")
        }

        override fun onRenderedFirstFrame() {
            Log.d(TAG, "ExoPlayer has rendered its first frame. Setting hasExoPlayerRenderedFirstFrame to true.")
            hasExoPlayerRenderedFirstFrame = true
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "Is Playing Changed: $isPlaying")
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            Log.d(TAG, "Video size changed: ${videoSize.width}x${videoSize.height} (Pixel aspect ratio: ${videoSize.pixelWidthHeightRatio})")
        }
    }
}

// --- IMPORTANT: REMOVED THE DUPLICATE DefaultPreview() FROM HERE ---
// It should ONLY be in MainActivity.kt