package fr.ectalhawk.rgbweatherkit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

//Activité servant uniquement lorsque l'application n'a pas les droit nécessaires pour fonctionner.
class NoBLEAuthorization : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_bleauthorization)
    }
}