package com.example.anadrome
import androidx.media3.transformer.ExportResult
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.anadrome.databinding.ActivityCropVideoBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import androidx.media3.transformer.Composition

@UnstableApi
class CropVideoActivity : AppCompatActivity() {

    private val TAG = "CropVideoActivity"

    private lateinit var binding: ActivityCropVideoBinding
    private var exoPlayer: ExoPlayer? = null
    private var videoUri: Uri? = null
    private var transformer: Transformer? = null

    private var videoIntrinsicWidth: Int = 0
    private var videoIntrinsicHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityCropVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriString = intent.getStringExtra("videoUri")
        if (uriString == null) {
            Log.e(TAG, "Video URI is null. Finishing activity.")
            Toast.makeText(this, "No video selected.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        videoUri = Uri.parse(uriString)
        initializePlayer()

        binding.applyWallpaperButton.setOnClickListener {
            val normalizedCropRect = binding.cropOverlayView.getNormalizedCropRect(videoIntrinsicWidth, videoIntrinsicHeight)
            if (normalizedCropRect.width() <= 0f || normalizedCropRect.height() <= 0f) {
                Toast.makeText(this, "Invalid crop area.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            startVideoTransformation(videoUri!!, normalizedCropRect)
        }
    }

    private fun startVideoTransformation(uri: Uri, normalizedCropRect: RectF) {
        val outputVideoFile = File(cacheDir, "cropped_video_${UUID.randomUUID()}.mp4")
        val mediaItem = MediaItem.fromUri(uri)

        val encoderFactory = DefaultEncoderFactory.Builder(this)
            .setEnableFallback(true)
            .build()

        transformer = Transformer.Builder(this)
            .setVideoMimeType("video/avc")
            .setEncoderFactory(encoderFactory)
            .addListener(getTransformerListener(outputVideoFile))
            .build()

        val transformationMatrix = Matrix().apply {
            val scaleX = 1f / normalizedCropRect.width()
            val scaleY = 1f / normalizedCropRect.height()
            val cropCenterXNdc = 2f * normalizedCropRect.centerX() - 1f
            val cropCenterYNdc = 1f - 2f * normalizedCropRect.centerY()
            val translateX = -cropCenterXNdc * scaleX
            val translateY = -cropCenterYNdc * scaleY
            setScale(scaleX, scaleY)
            postTranslate(translateX, translateY)
        }

        val matrixTransformation = MatrixTransformation { transformationMatrix }

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(androidx.media3.transformer.Effects(emptyList(), listOf(matrixTransformation)))
            .build()

        try {
            setUiInProgress(true)
            transformer?.start(editedMediaItem, outputVideoFile.absolutePath)
            checkTransformationProgress()
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: Could not start transformation", e)
            setUiInProgress(false)
            Toast.makeText(this, "Failed to start processing.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkTransformationProgress() {
        val progressHolder = ProgressHolder()
        lifecycleScope.launch {
            while (true) {
                val progressState = transformer?.getProgress(progressHolder)
                if (progressState == null || progressState != Transformer.PROGRESS_STATE_AVAILABLE) {
                    break
                }
                binding.progressBar.progress = progressHolder.progress
            }
        }
    }

    private fun getTransformerListener(outputFile: File) = object : Transformer.Listener {
        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
            runOnUiThread {
                setUiInProgress(false)
                val outputUri = Uri.fromFile(outputFile)
                saveWallpaperSettings(outputUri)
                promptSetLiveWallpaper()
            }
        }

        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
            Log.e(TAG, "LISTENER: onError", exportException)
            runOnUiThread {
                setUiInProgress(false)
                Toast.makeText(applicationContext, "Failed to process video.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setUiInProgress(inProgress: Boolean) {
        binding.progressBar.isVisible = inProgress
        binding.progressText.isVisible = inProgress
        binding.applyWallpaperButton.isEnabled = !inProgress
        binding.playerView.isVisible = !inProgress
        binding.cropOverlayView.isVisible = !inProgress
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri!!))
            repeatMode = Player.REPEAT_MODE_ALL
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            volume = 0f
            addListener(playerListener)
            prepare()
            playWhenReady = true
        }
        binding.playerView.player = exoPlayer
    }

    private fun saveWallpaperSettings(uri: Uri) {
        getSharedPreferences("video_wallpaper_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("video_uri", uri.toString())
            apply()
        }
    }

    private fun promptSetLiveWallpaper() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@CropVideoActivity, VideoWallpaperService::class.java)
            )
        }
        startActivity(intent)
    }

    private val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            videoIntrinsicWidth = videoSize.width
            videoIntrinsicHeight = videoSize.height
            val viewWidth = binding.playerView.width.toFloat()
            val viewHeight = binding.playerView.height.toFloat()
            val videoAspectRatio = videoIntrinsicWidth.toFloat() / videoIntrinsicHeight
            val viewAspectRatio = viewWidth / viewHeight

            val (left, top, right, bottom) = if (viewAspectRatio > videoAspectRatio) {
                val displayedWidth = viewHeight * videoAspectRatio
                val offsetX = (viewWidth - displayedWidth) / 2f
                arrayOf(offsetX, 0f, offsetX + displayedWidth, viewHeight)
            } else {
                val displayedHeight = viewWidth / videoAspectRatio
                val offsetY = (viewHeight - displayedHeight) / 2f
                arrayOf(0f, offsetY, viewWidth, offsetY + displayedHeight)
            }
            binding.cropOverlayView.setVideoDisplayRect(left, top, right, bottom)
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
        exoPlayer?.release()
        transformer?.cancel()
    }
}