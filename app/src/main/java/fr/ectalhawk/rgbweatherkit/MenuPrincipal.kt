package fr.ectalhawk.rgbweatherkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import fr.ectalhawk.rgbweatherkit.databinding.ActivityMenuPrincipalBinding

class MenuPrincipal : AppCompatActivity() {

    private lateinit var binding : ActivityMenuPrincipalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuPrincipalBinding.inflate(layoutInflater)
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

    private fun replaceFragment(fragment : Fragment){

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
        fragmentTransaction.commit()
    }
}