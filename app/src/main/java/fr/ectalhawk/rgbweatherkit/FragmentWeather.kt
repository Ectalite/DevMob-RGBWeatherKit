package fr.ectalhawk.rgbweatherkit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import fr.ectalhawk.rgbweatherkit.weatherAPI.respository.Response
import fr.ectalhawk.rgbweatherkit.weatherAPI.respository.WeatherRepository
import fr.ectalhawk.rgbweatherkit.weatherAPI.viewmodel.MainViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentWeather.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentWeather : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_weather, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val weatherRepository = WeatherRepository(AppBLEInterface.weatherService, activity as MenuPrincipal)
        val mainViewModel = MainViewModel(weatherRepository)

        val textCity = requireView().findViewById<TextInputEditText>(R.id.textCity5)
        val btnResearch = requireView().findViewById<Button>(R.id.button4)

        //textCity.setText("")

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
                                AppBLEInterface.oBLEInterface.sendText(
                                    0, 0,0xFFFFFF,"Weather:",false)
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
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FragmentWeather.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentWeather().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}