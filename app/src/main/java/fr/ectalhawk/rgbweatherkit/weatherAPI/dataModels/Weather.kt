package fr.ectalhawk.rgbweatherkit.weatherAPI.dataModels

data class Weather(
    val description: String,
    val icon: String,
    val id: Int,
    val main: String
)