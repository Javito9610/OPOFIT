package com.opofit.miapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class OpoFitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.d("OpoFitApp", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("OpoFitApp", "Firebase initialization failed — Google Sign-In will not work", e)
        }
    }
}
