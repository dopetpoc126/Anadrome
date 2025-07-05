package com.example.anadrome

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val videoUri: Uri? = result.data?.data
            videoUri?.let { uri ->
                Log.d(TAG, "Video selected: $uri")
                val intent = Intent(this, CropVideoActivity::class.java).apply {
                    putExtra("videoUri", uri.toString())
                }
                startActivity(intent)
            } ?: run {
                Log.w(TAG, "No video URI returned from picker.")
                Toast.makeText(this, "No video selected.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "Video picking cancelled or failed. Result code: ${result.resultCode}")
            Toast.makeText(this, "Video selection cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Storage permission granted.")
            pickVideo()
        } else {
            Log.w(TAG, "Storage permission denied.")
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        findViewById<Button>(R.id.upload_file_button).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showUploadRecommendationDialog()
        }

        // Listener for the new button
        findViewById<Button>(R.id.view_wallpaper_button).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            handleViewCurrentWallpaper()
        }

        findViewById<Button>(R.id.settings_button).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleViewCurrentWallpaper() {
        val wallpaperManager = WallpaperManager.getInstance(this)
        val wallpaperInfo = wallpaperManager.wallpaperInfo

        if (wallpaperInfo != null && wallpaperInfo.packageName == this.packageName) {
            // Your app's wallpaper is active
            val prefs = getSharedPreferences("video_wallpaper_prefs", Context.MODE_PRIVATE)
            val videoUri = prefs.getString("video_uri", null)

            if (videoUri != null) {
                val intent = Intent(this, WallpaperPreviewActivity::class.java).apply {
                    putExtra("video_uri", videoUri)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Could not find current wallpaper video.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // No wallpaper from your app is active
            Toast.makeText(this, "No Anadrome wallpaper applied.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUploadRecommendationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_suggestion, null)
        MaterialAlertDialogBuilder(this)
            .setTitle("Suggestion")
            .setView(dialogView)
            .setPositiveButton("Continue") { _, _ ->
                checkAndRequestPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Storage permission already granted.")
            pickVideo()
        } else {
            Log.d(TAG, "Requesting storage permission: $permission")
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        pickVideoLauncher.launch(Intent.createChooser(intent, "Select Video"))
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage("Storage permission is required to select videos. Please grant the permission in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}