package fr.ectalhawk.rgbweatherkit

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class FragmentHome : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //On affiche le nom du device connecté
        val deviceString = requireView().findViewById<TextView>(R.id.connectedDevice)
        deviceString.text = AppBLEInterface.connectedDevice

        //Bouton d'information qui affiche une popup
        val btnInfo = requireView().findViewById<ImageView>(R.id.btnInfo)
        btnInfo.setOnClickListener {
            val intent = Intent(activity as MenuPrincipal, InfoPopUp::class.java)
            //On peut customiser la popup en lui donnant un titre, un texte et un bouton de retour.
            intent.putExtra("popuptitle", "RGBWeatherKit")
            intent.putExtra("popuptext", "Xavier Hueber et Noé Lindenlaub ©2022-2023\n\n " +
                    "BuildSDK \n${Build.VERSION.SDK_INT} " +
                    "Credits:\n - Google Maps (Leku)\n https://adevintaspain.github.io/Leku/\n " +
                    "- BLE Library\n https://github.com/NordicSemiconductor/Android-BLE-Library\n")
            intent.putExtra("popupbtn", "OK")
            intent.putExtra("darkstatusbar", false)
            startActivity(intent)
        }
    }
}