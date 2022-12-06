package fr.ectalhawk.rgbweatherkit.weatherAPI.respository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.ectalhawk.rgbweatherkit.weatherAPI.api.MyWeatherServiceInterface
import fr.ectalhawk.rgbweatherkit.weatherAPI.utils.NetworkUtils
import fr.ectalhawk.rgbweatherkit.weatherAPI.dataModels.MyWeather
import java.util.*

//Repository is used to manage our data. It needs access to retrofit service to do so.
class WeatherRepository(
        private val myWeatherService: MyWeatherServiceInterface,
        private val applicationContext: Context
) {

    private val weatherLiveData = MutableLiveData<Response<MyWeather>>()
    //Will be called in MainViewModel.
    val weather: LiveData<Response<MyWeather>>
        get() = weatherLiveData

    suspend fun getWeather(location: String, units: String) {
        if(NetworkUtils.isInternetAvailable(applicationContext)){
                val result = myWeatherService.getWeather(location,units, Locale.getDefault().language)
                if(result.body() != null){
                    weatherLiveData.postValue(Response.Success(result.body()))
                }
                else{
                    weatherLiveData.postValue(Response.Error("Error 404: Location not found"))
                }
        }
        else{
            //handle no internet
            weatherLiveData.postValue(Response.Error("No Internet"))
        }

    }
}
