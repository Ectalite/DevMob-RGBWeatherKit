package fr.ectalhawk.rgbweatherkit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_selection)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1001
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        //Création d'un objet BLEInterface servant à la communication entre android et le rapsberry Pi

        val button = findViewById<Button>(R.id.scanButton)

        val text = findViewById<TextView>(R.id.textViewLifecycleState)
        text.text = "Status: BuildSDK ${Build.VERSION.SDK_INT}"

        AppBLEInterface.oBLEInterface = BLEinterface(MainActivity@this, applicationContext) //not compatible with android 7
        //AppBLEInterface.oBLEInterface = BLEinterface(this, MainActivity@this)

        button.setOnClickListener {
            //Initialisation du BLE et démarrage du scan
            AppBLEInterface.oBLEInterface.prepareAndStartBleScan()
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

    fun goToTest(){
        Log.d("BLEinterface", "GoToTest was called")
        val intent = Intent(this, TestActivity::class.java)
        startActivity(intent)
    }

    //La fonction est buggé pour android < 9.0
    /*override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>,
                                   grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("BLEinterface", "onRequestPermissionsResult was called")
        when (requestCode) {
            3 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // permission was granted
                } else {
                    // permission denied
                    Toast.makeText(
                        this@MainActivity,
                        "Permission denied to use Bluetooth",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }*/
}

class MyListAdapter(private val context: Activity, private val device: ArrayList<BluetoothDevice>)
    : ArrayAdapter<BluetoothDevice>(context, R.layout.device_list, device) {
    @SuppressLint("ViewHolder", "SetTextI18n", "InflateParams")
    override fun getView(position: Int, view: View?, parent: ViewGroup): View { val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.device_list, null, true)
        val nameText = rowView.findViewById(R.id.Name) as TextView
        val uuidText = rowView.findViewById(R.id.UUID) as TextView
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("BLEinterface", "MyListAdapter n'a pas les permissions nécessaires pour scanner")
                val intent = Intent(context, NoBLEAuthorization::class.java)
                context.startActivity(intent)
            }
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