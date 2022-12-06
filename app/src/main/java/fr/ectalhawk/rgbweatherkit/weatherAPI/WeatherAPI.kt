package fr.ectalhawk.rgbweatherkit.weatherAPI

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import fr.ectalhawk.rgbweatherkit.AppBLEInterface
import fr.ectalhawk.rgbweatherkit.R
import fr.ectalhawk.rgbweatherkit.weatherAPI.api.MyWeatherServiceInterface
import fr.ectalhawk.rgbweatherkit.weatherAPI.api.RetrofitHelper
import fr.ectalhawk.rgbweatherkit.weatherAPI.respository.Response
import fr.ectalhawk.rgbweatherkit.weatherAPI.respository.WeatherRepository
import fr.ectalhawk.rgbweatherkit.weatherAPI.viewmodel.MainViewModel

class WeatherAPI : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_api)

        val weatherService = RetrofitHelper.getRetrofitInstance().create(MyWeatherServiceInterface::class.java)
        val weatherRepository = WeatherRepository(weatherService, applicationContext)
        val mainViewModel = MainViewModel(weatherRepository)

        val textCity = findViewById<TextInputEditText>(R.id.textCity)
        val btnResearch = findViewById<ImageView>(R.id.btnResearch)
        val iconWeather = findViewById<ImageView>(R.id.iconWeather)

        btnResearch.setOnClickListener {
            btnResearch.isEnabled = false
            Toast.makeText(this, R.string.researchWeather, Toast.LENGTH_SHORT).show()

            mainViewModel.location = textCity.text.toString()
            mainViewModel.setOnSearchbtnClick()

            mainViewModel.locationStatus.observe(this) {
                if (mainViewModel.locationStatus.value == false) {
                    Toast.makeText(this, "Error 404: No Location found", Toast.LENGTH_SHORT).show()
                }
            }

            mainViewModel.weather.observe(this) { fetchedData ->
                when(fetchedData){
                    is Response.Loading ->{
                        Toast.makeText(this,R.string.loadingWeather,Toast.LENGTH_SHORT).show()
                    }
                    is Response.Error ->{
                        fetchedData.errorMessage
                        Toast.makeText(this,fetchedData.errorMessage,Toast.LENGTH_SHORT).show()
                    }
                    is Response.Success ->{
                        fetchedData.data?.let {
                            Glide.with(this)
                                .load("https://openweathermap.org/img/wn/".plus(it.weather[0].icon).plus("@4x.png"))
                                .into(iconWeather)
                            AppBLEInterface.oBLEInterface.clearMatrix()
                            AppBLEInterface.oBLEInterface.sendText(
                                0, 0,0xFFFFFF,"Weather:",false)
                            AppBLEInterface.oBLEInterface.sendText(
                                0, 10,0xFFFFFF, it.name,false)
                            AppBLEInterface.oBLEInterface.sendText(
                                0, 22,0xFFFFFF,it.main.temp.toString().plus(" ° C"),true)

                        }

                    }
                }
            }
            btnResearch.isEnabled = true
        }

        val btnReturn = findViewById<Button>(R.id.btnReturn)
        btnReturn.setOnClickListener {
            finish()
        }
    }
}