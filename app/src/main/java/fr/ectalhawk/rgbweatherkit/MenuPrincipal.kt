package fr.ectalhawk.rgbweatherkit

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
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

        setContentView(binding.root)
        //Init fragment, celui qui s'affichera en premier
        replaceFragment(FragmentBTSettings())

        //On est pas encore connecté en bluetooth -> navbar désactivé
        deactivateBottomNavigation()

        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_btsettings -> replaceFragment(FragmentBTSettings())
                R.id.navigation_home -> replaceFragment(FragmentHome())
                R.id.navigation_weather -> replaceFragment(FragmentWeather())
                R.id.navigation_pixels -> replaceFragment(FragmentManual())
                else->{
                    //On affiche le menu home si on ne sait pas
                    replaceFragment(FragmentHome())
                }
            }
            true
        }
    }



    fun replaceFragment(fragment : Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
        fragmentTransaction.commit()
    }

    //Fonction pour désactiver la navbar
    fun activateBottomNavigation(){
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Find the menu item and then disable it
        navView.menu.findItem(R.id.navigation_home).isEnabled = true
        navView.menu.findItem(R.id.navigation_home).setIcon(R.drawable.menu_home_icon_black)
        navView.menu.findItem(R.id.navigation_weather).isEnabled = true
        navView.menu.findItem(R.id.navigation_weather).setIcon(R.drawable.menu_weather_icon_black)
        navView.menu.findItem(R.id.navigation_pixels).isEnabled = true
        navView.menu.findItem(R.id.navigation_pixels).setIcon(R.drawable.menu_pixels_icon_black)

    }

    //Fonction pour activer la navbar
    fun deactivateBottomNavigation(){
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Find the menu item and then disable it
        navView.menu.findItem(R.id.navigation_home).isEnabled = false
        navView.menu.findItem(R.id.navigation_home).setIcon(R.drawable.menu_home_icon_grey)
        navView.menu.findItem(R.id.navigation_weather).isEnabled = false
        navView.menu.findItem(R.id.navigation_weather).setIcon(R.drawable.menu_weather_icon_grey)
        navView.menu.findItem(R.id.navigation_pixels).isEnabled = false
        navView.menu.findItem(R.id.navigation_pixels).setIcon(R.drawable.menu_pixels_icon_grey)
    }
}