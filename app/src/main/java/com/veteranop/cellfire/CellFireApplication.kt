package com.veteranop.cellfire

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CellFireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FrequencyCalculator.init(this)
    }
}