package fr.ectalhawk.rgbweatherkit

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

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
        val btnInfo = requireView().findViewById<ImageView>(R.id.btnInfo)
        btnInfo.setOnClickListener {
            val intent = Intent(activity as MenuPrincipal, InfoPopUp::class.java)
            intent.putExtra("popuptitle", "RGBWeatherKit")
            intent.putExtra("popuptext", "Credits:\n - Leku https://adevintaspain.github.io/Leku/\n - BLE Library https://github.com/NordicSemiconductor/Android-BLE-Library\n")
            intent.putExtra("popupbtn", "OK")
            intent.putExtra("darkstatusbar", false)
            startActivity(intent)
        }
    }
}