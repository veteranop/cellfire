package com.veteranop.cellfire

import android.app.Application
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CellFireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CarrierResolver.initialize(this)
        // Enable offline persistence at startup so queued writes survive network loss.
        // Must be called before any other Firebase Database usage.
        Firebase.database("https://veteranopcom-default-rtdb.firebaseio.com")
            .setPersistenceEnabled(true)
    }
}
