package fr.ectalhawk.rgbweatherkit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.app.ActivityCompat
import fr.ectalhawk.rgbweatherkit.weatherAPI.api.MyWeatherServiceInterface

class AppBLEInterface : Application() {
    //Singleton for BLEinterface for entire Application. -> is created at start of application
    //Ce Singleton sert à pouvoir accéder à l'interface BLEinterface à partir de l'ensemble de l'application
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var oBLEInterface : BLEinterface
        lateinit var weatherService: MyWeatherServiceInterface
        var connectedDevice : String = "Device"
    }
}

//MyListAdapter est un adapteur pour avoir la liste des devices trouvés par Bluetooth
class MyListAdapter(private val context: Activity, private val device: ArrayList<BluetoothDevice>)
    : ArrayAdapter<BluetoothDevice>(context, R.layout.device_list, device) {
    @SuppressLint("ViewHolder", "SetTextI18n", "InflateParams")
    override fun getView(position: Int, view: View?, parent: ViewGroup): View { val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.device_list, null, true)
        val nameText = rowView.findViewById(R.id.Name) as TextView
        val uuidText = rowView.findViewById(R.id.UUID) as TextView
        //Si l'application pas les permissions suffisantes
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("BLEinterface", "MyListAdapter n'a pas les permissions nécessaires pour scanner")
                val intent = Intent(context, NoBLEAuthorization::class.java)
                context.startActivity(intent)
            }
        }
        //On va afficher le nom du device dans la liste
        if (device[position].name == "") {
            nameText.text = "no name"
        }
        else {
            nameText.text = device[position].name
        }
        uuidText.text = device[position].address
        return rowView
    }
}