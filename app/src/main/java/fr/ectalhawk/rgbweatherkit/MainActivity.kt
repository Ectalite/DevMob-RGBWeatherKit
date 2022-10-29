package fr.ectalhawk.rgbweatherkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        //Création d'un objet BLEInterface servant à la communication entre android et le rapsberry Pi
        val oBLEInterface = BLEinterface(this, applicationContext)
        //Initialisation du BLE et démarrage du scan
        oBLEInterface.prepareAndStartBleScan()
    }

    override fun onStop() {
        super.onStop()

    }

    override fun onDestroy() {
        super.onDestroy()

    }
}