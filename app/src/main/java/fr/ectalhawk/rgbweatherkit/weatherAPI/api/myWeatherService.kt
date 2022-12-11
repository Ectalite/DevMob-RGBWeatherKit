package fr.ectalhawk.rgbweatherkit.weatherAPI.api

import fr.ectalhawk.rgbweatherkit.weatherAPI.dataModels.MyWeather
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

const val API_KEY: String = "7aa7bd654a237514d612a76d2774afef"

interface MyWeatherServiceInterface {
    @GET("data/2.5/weather?appid=$API_KEY")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("units") units: String,
        @Query("lang") language: String
    ): Response<MyWeather>
}
