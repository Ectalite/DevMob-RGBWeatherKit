package fr.ectalhawk.rgbweatherkit

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import yuku.ambilwarna.AmbilWarnaDialog


class TestActivity : AppCompatActivity() {
    private var selectedColor = Color.RED

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        if(AppBLEInterface.oBLEInterface.connectedGatt!!.device.name != null) {
            val textViewName = findViewById<TextView>(R.id.textConnected)
            val deviceName = AppBLEInterface.oBLEInterface.connectedGatt!!.device.name
            textViewName.text = "Connected to: $deviceName"
        }
        else
        {
            Log.d("BLEinterface", "Something is wrong by TestActivity")
        }

        val pixelXBar = findViewById<SeekBar>(R.id.pixelXBar)
        val pixelYBar = findViewById<SeekBar>(R.id.pixelYBar)
        val pixelXTextView = findViewById<TextView>(R.id.PixelX)
        val pixelYTextView = findViewById<TextView>(R.id.PixelY)
        val pickColorButton = findViewById<Button>(R.id.pick_color_button)
        val sendButton = findViewById<Button>(R.id.buttonSend)

        pixelXBar.max = 63
        pixelYBar.max = 31

        pixelXBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                pixelXTextView.text = "PixelX: $progress"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        pixelYBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                pixelYTextView.text = "PixelY: $progress"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        pickColorButton.setOnClickListener {
            openColorPickerDialogue(selectedColor)
        }

        sendButton.setOnClickListener {
            //On enlève l'alpha de la couleur
            val sendColor = selectedColor and 0x00FFFFFF
            Log.d("BLEinterface", "Sending pixel PosX ${pixelXBar.progress} | " +
                    "PosX ${pixelYBar.progress} | color ${Integer.toHexString(sendColor)}")
            AppBLEInterface.oBLEInterface.sendPixel(pixelXBar.progress,pixelYBar.progress,sendColor,true)
        }

        val btnWriteText = findViewById<Button>(R.id.btnWriteText)
        btnWriteText.setOnClickListener {
            //val sendColor = selectedColor and 0x00FFFFFF
            //OLD:
            //val textManager = TextManager()
            //textManager.loadFont(this)
            //textManager.writeText("TEST",5,5,0xFFFFFF)
            //NEW:
            AppBLEInterface.oBLEInterface.sendText(pixelXBar.progress,pixelYBar.progress,0xFFFFFF,"TEST",true)
        }
    }

    private fun openColorPickerDialogue(selectedColor : Int) {
        val previewColor = findViewById<View>(R.id.preview_selected_color)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val colorPickerDialogue = AmbilWarnaDialog(this, selectedColor,
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog?) {
                    }

                    override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                        this@TestActivity.selectedColor = color
                        previewColor.setBackgroundColor(selectedColor)
                    }
                })
            colorPickerDialogue.show()
        } else {
            val colorPickerDialogue = AmbilWarnaDialog(this, 0,
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog?) {
                    }

                    override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                        this@TestActivity.selectedColor = color
                        previewColor.setBackgroundColor(selectedColor)
                    }
                })
            colorPickerDialogue.show()
        }
    }
}