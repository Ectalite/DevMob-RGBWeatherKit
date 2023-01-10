package fr.ectalhawk.rgbweatherkit

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.adevinta.leku.ADDRESS
import com.adevinta.leku.LocationPickerActivity
import com.google.android.material.textfield.TextInputEditText
import fr.ectalhawk.rgbweatherkit.weatherAPI.respository.Response
import fr.ectalhawk.rgbweatherkit.weatherAPI.respository.WeatherRepository
import fr.ectalhawk.rgbweatherkit.weatherAPI.viewmodel.MainViewModel
import java.util.Locale

class FragmentWeather : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_weather, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val weatherRepository = WeatherRepository(AppBLEInterface.weatherService, activity as MenuPrincipal)
        val mainViewModel = MainViewModel(weatherRepository)

        val textCity = requireView().findViewById<TextInputEditText>(R.id.inputTextCity)
        val btnResearch = requireView().findViewById<Button>(R.id.btnSend)

        btnResearch.setOnClickListener {
            btnResearch.isEnabled = false
            Toast.makeText(activity as MenuPrincipal, R.string.researchWeather, Toast.LENGTH_SHORT).show()

            mainViewModel.location = textCity.text.toString()
            mainViewModel.setOnSearchbtnClick()

            mainViewModel.locationStatus.observe(activity as MenuPrincipal) {
                if (mainViewModel.locationStatus.value == false) {
                    Toast.makeText(activity as MenuPrincipal, "Error 404: No Location found", Toast.LENGTH_SHORT).show()
                }
            }

            mainViewModel.weather.observe(activity as MenuPrincipal) { fetchedData ->
                when(fetchedData){
                    is Response.Loading ->{
                        Toast.makeText(activity as MenuPrincipal,R.string.loadingWeather, Toast.LENGTH_SHORT).show()
                    }
                    is Response.Error ->{
                        fetchedData.errorMessage
                        Toast.makeText(activity as MenuPrincipal,fetchedData.errorMessage, Toast.LENGTH_SHORT).show()
                    }
                    is Response.Success ->{
                        fetchedData.data?.let {
                            if(!btnResearch.isEnabled) {
                                AppBLEInterface.oBLEInterface.clearMatrix()
                                //Si la langue du téléphone est le français alors on envoie Météo a la place de weather
                                if(Locale.getDefault() == Locale.FRANCE)
                                {
                                    AppBLEInterface.oBLEInterface.sendText(
                                        0, 0,0xFFFFFF,"Meteo:",false)
                                }
                                else
                                {
                                    AppBLEInterface.oBLEInterface.sendText(
                                        0, 0,0xFFFFFF,"Weather:",false)
                                }
                                AppBLEInterface.oBLEInterface.sendText(
                                    0, 10,0xFFFFFF, it.name,false)
                                AppBLEInterface.oBLEInterface.sendText(
                                    0, 22,0xFFFFFF,it.main.temp.toString().plus(" ° C"),true)
                                btnResearch.isEnabled = true
                            }
                        }

                    }
                }
            }

        }

        val btnMap = requireView().findViewById<Button>(R.id.btnMap)
        btnMap.setOnClickListener {
            //On crée un Intent avec l'activité de LocationPicker
            //https://github.com/AdevintaSpain/Leku
            val locationPickerIntent = LocationPickerActivity.Builder()
                .withLocation(41.4036299, 2.1743558)
                .withGeolocApiKey("AIzaSyDcTZkFzllJvhN2nqjusn0fn-2ULWsD0nw")
                .withGooglePlacesApiKey("AIzaSyDkzRR9DFJ05Om8e_evsiee4iUhPsPiJ-4")
                .withSearchZone("fr_CH")
                //.withSearchZone(SearchZoneRect(LatLng(26.525467, -18.910366), LatLng(43.906271, 5.394197)))
                .withDefaultLocaleSearchZone()
                .shouldReturnOkOnBackPressed()
                .withStreetHidden()
                .withCityHidden()
                .withZipCodeHidden()
                .withSatelliteViewHidden()
                //.withGooglePlacesEnabled()
                .withGoogleTimeZoneEnabled()
                .withVoiceSearchHidden()
                .withUnnamedRoadHidden()
                //.withSearchBarHidden()
                .build(activity as MenuPrincipal)

            resultLauncher.launch(locationPickerIntent)
            //Deprecated
            //startActivityForResult(locationPickerIntent, 1)
        }
    }

    //Fonction de retour de l'Intent google maps
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            //On récupère les données retourné par l'Intent
            val data: Intent? = result.data
            //On isole l'adresse
            val fullAddress = data?.parcelable<Address>(ADDRESS)
            if (fullAddress != null) {
                val city =
                    fullAddress.toString().substringAfterLast("locality=").substringBefore(",")
                Log.d("City", city)
                val textCity = requireView().findViewById<TextInputEditText>(R.id.inputTextCity)
                textCity.setText(city)
            }
        }
    }
}

//https://stackoverflow.com/questions/73019160/android-getparcelableextra-deprecated
//getParcelableExtra was deprecated in SDK < 33
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}