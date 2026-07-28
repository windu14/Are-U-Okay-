package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    initFirebaseWithExplicitOptions()
                } else {
                    Log.d("MainApplication", "FirebaseApp default initialized successfully: ${app.name}")
                }
            } else {
                Log.d("MainApplication", "FirebaseApp already initialized")
            }
        } catch (e: Exception) {
            Log.e("MainApplication", "Default FirebaseApp init failed, attempting fallback explicit options", e)
            initFirebaseWithExplicitOptions()
        }
    }

    private fun initFirebaseWithExplicitOptions() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyAuhsyXg1Q3AtcPkQSBWnypyBQmUEpYQLo")
                    .setApplicationId("1:81299636875:android:823c3fe2bd495a4b524a8e")
                    .setProjectId("areyouokay-c1487")
                    .setGcmSenderId("81299636875")
                    .setStorageBucket("areyouokay-c1487.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("MainApplication", "Firebase initialized with explicit options successfully")
            }
        } catch (e: Exception) {
            Log.e("MainApplication", "Explicit Firebase initialization error", e)
        }
    }
}
