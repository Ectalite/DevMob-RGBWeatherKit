package fr.ectalhawk.rgbweatherkit

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        //Création d'un objet BLEInterface servant à la communication entre android et le rapsberry Pi

        val button = findViewById<Button>(R.id.button)

        val oBLEInterface = BLEinterface(this, applicationContext)
        button.setOnClickListener {
            //Initialisation du BLE et démarrage du scan
            oBLEInterface.prepareAndStartBleScan()
        }



        //Stopper après 10sec, aucune idée de comment faire ?
        //oBLEInterface.safeStopBleScan()
    }

    override fun onStop() {
        super.onStop()

    }

    override fun onDestroy() {
        super.onDestroy()

    }
}

class MyListAdapter(private val context: Activity, private val device: ArrayList<BluetoothDevice>)
    : ArrayAdapter<BluetoothDevice>(context, R.layout.device_list, device) {
    override fun getView(position: Int, view: View?, parent: ViewGroup): View { val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.device_list, null, true)
        val nameText = rowView.findViewById(R.id.Name) as TextView
        val uuidText = rowView.findViewById(R.id.UUID) as TextView
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("BLEinterface","safeStartBleScan n'a pas les permissions nécessaires pour scanner")
            val intent = Intent(context, NoBLEAuthorization::class.java)
            context.startActivity(intent)
        }
        if (device[position].name == "") {
            nameText.text = "no name"
        }
        else {
            nameText.text = device[position].name
        }
        uuidText.text = device[position].address
        //Log.i("Bonjour", "Test1")
        return rowView
    }
}