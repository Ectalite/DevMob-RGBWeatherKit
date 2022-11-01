package fr.ectalhawk.rgbweatherkit

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_selection)
    }

    override fun onStart() {
        super.onStart()
        //Création d'un objet BLEInterface servant à la communication entre android et le rapsberry Pi
        val oBLEInterface = BLEinterface(this, applicationContext)
        val button = findViewById<Button>(R.id.button)
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