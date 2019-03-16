package com.appbusso.mapsapp

import android.app.Application
import com.appbusso.mapsapp.UI.moduleMain
import com.facebook.FacebookSdk
import org.koin.android.ext.android.startKoin


class MapsApplication: Application() {


    companion object {
        private val TAG = MapsApplication::class.java.simpleName

        @get:Synchronized lateinit var instance: MapsApplication
            private set
    }


    override fun onCreate() {
        super.onCreate()
        instance = this

        FacebookSdk.sdkInitialize(applicationContext)
        startKoin(this, listOf(moduleMain))

    }
}