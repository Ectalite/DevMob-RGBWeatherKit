package fr.ectalhawk.rgbweatherkit

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class FragmentBTSettings() : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bt_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Création d'un objet BLEInterface servant à la communication entre android et le rapsberry Pi
        val button = requireView().findViewById<Button>(R.id.scanButton)
        val text = requireView().findViewById<TextView>(R.id.textViewLifecycleState)
        text.text = "Status: BuildSDK ${Build.VERSION.SDK_INT}"

        //Initalise BLEinterface
        AppBLEInterface.oBLEInterface = BLEinterface(activity as MenuPrincipal, requireActivity().applicationContext)

        button.setOnClickListener {
            //Initialisation du BLE et démarrage du scan
            AppBLEInterface.oBLEInterface.prepareAndStartBleScan()
        }
    }
}