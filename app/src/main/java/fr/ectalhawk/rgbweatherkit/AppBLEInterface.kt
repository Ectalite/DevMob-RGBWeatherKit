package fr.ectalhawk.rgbweatherkit

import android.app.Application

class AppBLEInterface : Application() {

    companion object {
        lateinit var oBLEInterface : BLEinterface
    }

    override fun onCreate() {
        super.onCreate()
    }

}