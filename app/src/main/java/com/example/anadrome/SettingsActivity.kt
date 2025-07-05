package com.example.anadrome

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.anadrome.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        prefs = getSharedPreferences("video_wallpaper_prefs", Context.MODE_PRIVATE)

        // Load and apply saved settings
        loadSettings()

        // Set up listeners for the switches
        setupListeners()
    }

    private fun loadSettings() {
        binding.respectBatterySaverSwitch.isChecked = prefs.getBoolean("respect_battery_saver", true)
        binding.animateContinuouslySwitch.isChecked = prefs.getBoolean("animate_continuously", false)
        binding.animateOnReturnSwitch.isChecked = prefs.getBoolean("animate_on_return", false)
    }

    private fun setupListeners() {
        binding.respectBatterySaverSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            // Add Haptic Feedback
            buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            prefs.edit().putBoolean("respect_battery_saver", isChecked).apply()
        }

        binding.animateContinuouslySwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            // Add Haptic Feedback
            buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            prefs.edit().putBoolean("animate_continuously", isChecked).apply()
        }

        binding.animateOnReturnSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            // Add Haptic Feedback
            buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            prefs.edit().putBoolean("animate_on_return", isChecked).apply()
        }
    }
}