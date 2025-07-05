package com.example.anadrome

import android.app.Application
import com.google.android.material.color.DynamicColors

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // This line applies the user's dynamic theme to your application.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}