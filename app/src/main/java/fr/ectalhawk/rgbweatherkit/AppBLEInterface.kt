package fr.ectalhawk.rgbweatherkit

import android.annotation.SuppressLint
import android.app.Application
import fr.ectalhawk.rgbweatherkit.weatherAPI.api.MyWeatherServiceInterface

class AppBLEInterface : Application() {
    //Singleton for BLEinterface for entire Application. -> is created at start of application
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var oBLEInterface : BLEinterface
        lateinit var weatherService: MyWeatherServiceInterface
    }
}