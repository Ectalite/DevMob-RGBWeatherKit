package fr.ectalhawk.rgbweatherkit

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import yuku.ambilwarna.AmbilWarnaDialog

class FragmentManual : Fragment() {
    private var selectedColor = Color.WHITE

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manual, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val previewColor = requireView().findViewById<View>(R.id.preview_selected_color)
        previewColor.setBackgroundColor(selectedColor)

        val pixelXBar = requireView().findViewById<SeekBar>(R.id.pixelXBar)
        val pixelYBar = requireView().findViewById<SeekBar>(R.id.pixelYBar)
        val pixelXTextView = requireView().findViewById<TextView>(R.id.PixelX)
        val pixelYTextView = requireView().findViewById<TextView>(R.id.PixelY)
        val pickColorButton = requireView().findViewById<Button>(R.id.pick_color_button)
        val sendButton = requireView().findViewById<Button>(R.id.buttonSend)

        pixelXBar.max = 63
        pixelYBar.max = 31

        pixelXBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                pixelXTextView.text = "PixelX: $progress"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        pixelYBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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

        val btnWriteText = requireView().findViewById<Button>(R.id.btnWriteText)
        btnWriteText.setOnClickListener {
            val sendColor = selectedColor and 0x00FFFFFF
            val sendText = requireView().findViewById<EditText>(R.id.matrixText)
            AppBLEInterface.oBLEInterface.sendText(pixelXBar.progress,pixelYBar.progress,sendColor,sendText.text.toString(),true)
        }

        val btnClearMatrix = requireView().findViewById<Button>(R.id.clearButton)
        btnClearMatrix.setOnClickListener {
            AppBLEInterface.oBLEInterface.clearMatrix()
        }
    }

    private fun openColorPickerDialogue(selectedColor : Int) {
        val previewColor = requireView().findViewById<View>(R.id.preview_selected_color)
        val colorPickerDialogue = AmbilWarnaDialog(activity as MenuPrincipal, selectedColor,
            object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {
                }

                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    this@FragmentManual.selectedColor = color
                    previewColor.setBackgroundColor(color)
                }
            })
        colorPickerDialogue.show()
    }
}