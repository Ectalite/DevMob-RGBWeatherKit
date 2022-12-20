package fr.ectalhawk.rgbweatherkit

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentBTSettings.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentBTSettings() : Fragment() {
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FragmentBTSettings.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentBTSettings().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}