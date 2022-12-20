package fr.ectalhawk.rgbweatherkit

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import fr.ectalhawk.rgbweatherkit.databinding.ActivityMenuPrincipalBinding
import fr.ectalhawk.rgbweatherkit.weatherAPI.api.MyWeatherServiceInterface
import fr.ectalhawk.rgbweatherkit.weatherAPI.api.RetrofitHelper

class MenuPrincipal : AppCompatActivity() {

    lateinit var binding : ActivityMenuPrincipalBinding

    private var states = arrayOf(
        intArrayOf(android.R.attr.state_enabled),
        intArrayOf(-android.R.attr.state_enabled),
        intArrayOf(android.R.attr.state_selected)
    )

    private var colors = intArrayOf(
        R.color.blue_mid,
        R.color.gray,
        R.color.white
    )

    private var myList = ColorStateList(states, colors)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuPrincipalBinding.inflate(layoutInflater)
        binding.bottomNavigation.itemTextColor = myList

        //Initialisation Instance Retrofit
        AppBLEInterface.weatherService= RetrofitHelper.getRetrofitInstance().create(
            MyWeatherServiceInterface::class.java)

        //On est pas encore connecté en bluetooth -> navbar désactivé
        deactivateBottomNavigation()

        setContentView(binding.root)
        //Init fragment, celui qui s'affichera en premier
        replaceFragment(FragmentBTSettings())

        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_btsettings -> replaceFragment(FragmentBTSettings())
                R.id.navigation_home -> replaceFragment(FragmentHome())
                R.id.navigation_weather -> replaceFragment(FragmentWeather())
                R.id.navigation_pixels -> replaceFragment(FragmentMatrix())
                else->{
                    //On affiche le menu home si on ne sait pas
                    replaceFragment(FragmentHome())
                }
            }
            true
        }
    }



    fun replaceFragment(fragment : Fragment){
        //binding.bottomNavigation.itemTextColor = getColor(R.color.gray)
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
        fragmentTransaction.commit()
    }

    fun activateBottomNavigation(){
        binding.bottomNavigation.isEnabled = true
    }

    fun deactivateBottomNavigation(){
        binding.bottomNavigation.isEnabled = false
    }
}