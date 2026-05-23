package com.example.bfit

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Application class for BFIT.
 * Initializes core services on app startup.
 */
class BFITApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase (for Auth + Firestore)
        FirebaseApp.initializeApp(this)
    }
}
