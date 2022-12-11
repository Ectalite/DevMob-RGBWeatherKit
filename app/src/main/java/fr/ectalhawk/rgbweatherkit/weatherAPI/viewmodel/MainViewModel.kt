package fr.ectalhawk.rgbweatherkit.weatherAPI.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.ectalhawk.rgbweatherkit.weatherAPI.dataModels.MyWeather
import fr.ectalhawk.rgbweatherkit.weatherAPI.respository.Response
import fr.ectalhawk.rgbweatherkit.weatherAPI.respository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//Parameterized ViewModel so it requires a factory.
class MainViewModel(private val repository: WeatherRepository) : ViewModel() {
    var location: String? = null
    var locationStatus = MutableLiveData<Boolean?>()

    fun setOnSearchbtnClick() {
        if(location != null){
                locationStatus.value = true
                viewModelScope.launch(Dispatchers.IO) {
                    //MainViewModel calls getWeather to fetch weather info
                    repository.getWeather(location!!.trim(), "metric")
                }
            }
        else{
            locationStatus.value = false
        }

    }

    val weather: LiveData<Response<MyWeather>>
        get() = repository.weather

}