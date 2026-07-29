package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MainApplication : Application(), ImageLoaderFactory {

    override fun attachBaseContext(base: Context) {
        val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && base.attributionTag == null) {
            base.createAttributionContext("default")
        } else {
            base
        }
        super.attachBaseContext(attributionContext)
    }

    override fun onCreate() {
        super.onCreate()
        // Safeguard icon resources against AGP resource shrinker in release builds
        val masterIconRes = R.drawable.master_icon
        val launcherRes = R.mipmap.ic_launcher
        Log.d("MainApplication", "App icon resources initialized: $masterIconRes, $launcherRes")
        initFirebase()
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024L * 1024L) // 100 MB disk cache
                    .build()
            }
            .components {
                add(SvgDecoder.Factory())
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
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
