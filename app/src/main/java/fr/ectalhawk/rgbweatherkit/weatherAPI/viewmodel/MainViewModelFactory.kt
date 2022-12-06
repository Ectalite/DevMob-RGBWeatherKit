package fr.ectalhawk.rgbweatherkit.weatherAPI.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.ectalhawk.rgbweatherkit.weatherAPI.respository.WeatherRepository

//Helps in creating ViewModel objects.
class MainViewModelFactory(private val repository: WeatherRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}