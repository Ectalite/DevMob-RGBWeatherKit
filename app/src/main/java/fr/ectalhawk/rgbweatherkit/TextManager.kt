package fr.ectalhawk.rgbweatherkit

import android.content.Context
import fr.ectalhawk.rgbweatherkit.AppBLEInterface.Companion.oBLEInterface
import java.util.*

//Old TextManager, was replaced by BLEinterface sendText because it was too slow.
//Trying to repoduce https://github.com/hzeller/rpi-rgb-led-matrix/blob/master/lib/bdf-font.cc in Kotlin
class TextManager {
    //Singleton glyph
    object Glyph {
        var baseLine = 0
        var height = 0
        var width = 0
        //3 Dimensionnal array: first dimension is for letters, second for rows and third for pixels in row
        val alphabet = mutableListOf<List<BitSet>>()
        val alphabetName = mutableListOf<Int>()
    }

    fun loadFont( context: Context)
    {
        var startConvert = false
        var letter = 0
        var row = -1
        var fontHeight : Int
        var tempChar = ""
        var letterBitsets = mutableListOf<BitSet>()
        val myFont = readFile("textBitmap/4x6.bdf", context)
        if(myFont != null)
        {
            oBLEInterface.myLogger("Starting to load Font")
            for (line in myFont)
            {

                val parts = line.split(" ")
                when (parts.size) {
                    5 -> {
                        if (parts[0] == "FONTBOUNDINGBOX")
                        {
                            fontHeight = parts[2].toInt()
                            Glyph.baseLine = parts[4].toInt() +  fontHeight
                        }
                        else if(parts[0] == "BBX")
                        {
                            Glyph.width = parts[1].toInt()
                            Glyph.height = parts[2].toInt()
                            //Should I resize the alphabet list ? Don't think so
                            //Glyph.alphabet.size = Glyph.height
                            row = -1
                        }
                    }
                    1 -> {
                        if (parts[0] == "BITMAP")
                        {
                            row = 0
                            startConvert = true
                            letterBitsets = mutableListOf<BitSet>()
                            //oBLEInterface.myLogger("Start letter $letter: " + Glyph.alphabetName[letter])
                        }
                        else if (parts[0] == "ENDCHAR")
                        {
                            startConvert = false
                            //oBLEInterface.myLogger("Loading Font done.")
                            //oBLEInterface.myLogger("End letter $letter: " + Glyph.alphabetName[letter])
                            //At the end of the letter we put it in the
                            Glyph.alphabet.add(letter,letterBitsets.deepCopy())
                            letterBitsets.clear()
                            letter++
                            row = -1
                        }
                        else{
                            if(row >= 0 && startConvert)
                            {
                                val tempBitset = BitSet(Glyph.width)
                                for (pos in 0 until Glyph.width) {
                                    //we take the byte and push bit per bit in the biteset
                                    if((parts[0].toInt(16) and (0x80 shr pos)) != 0)
                                    {
                                        tempBitset.set(pos, true)
                                    } else {
                                        tempBitset.set(pos, false)
                                    }

                                }
                                //Add a row to the letter
                                letterBitsets.add(row, tempBitset)
                                row++
                            }

                        }
                    }
                    2 -> {
                        /*if (parts[0] == "STARTCHAR") {
                            tempChar = parts[1]
                        }
                        else*/
                        if (parts[0] == "ENCODING") {
                            //oBLEInterface.myLogger("Now processing letter " + tempChar + " with coding number ${parts[1]}")
                            Glyph.alphabetName.add(letter, parts[1].toInt())
                        }
                    }

                }
            }
        }
        else
        {
            oBLEInterface.myLogger("Could not open file while loading Font")
        }
    }

    private fun findGlyph(codingNumber : Int): Int {
        //We are here searching the index number of a letter Bitmap in the List alphabetName
        var indexNumber = -1
        for (pos in 0 .. Glyph.alphabetName.size) {
            if(Glyph.alphabetName[pos] == codingNumber)
            {
                indexNumber = pos
                break
            }
        }
        //Return -1 if the choosed letter was not found
        return indexNumber
    }

    private fun writeGlyph(char : Char, posX : Int, posY : Int, color : Int){
        oBLEInterface.myLogger("Searching index for char ${char.code}")
        val index = findGlyph(char.code)
        if(index >= 0) {
            for (Y in 0 until Glyph.height)
            {
                for (X in 0 until Glyph.width) {
                    if(Glyph.alphabet[index][Y][X]) {
                        oBLEInterface.sendPixel(X+posX, Y+posY, color, true)
                    }
                    else {
                        //Remove pixel -> black pixel
                        oBLEInterface.sendPixel(X+posX, Y+posY, 0x000000, true)
                    }
                }
            }
        }
        else {
            oBLEInterface.myLogger("Couldn't find index for this char")
        }
    }

    fun writeText(text : String, posX : Int, posY : Int, color : Int) {
        //Char must be placed at posX + sizeOfChar*counter + 1 (1 is space between two chars)
        var counter = 0
        for (char in text){
            oBLEInterface.myLogger("Writing char $char")
            writeGlyph(char, posX+Glyph.width*counter+1, posY, color)
            counter++
        }
    }

    private fun readFile(fileName: String, context: Context): List<String>
    {
        return context.assets.open(fileName).bufferedReader().readLines()
        //return this::class.java.getResourceAsStream(fileName)?.bufferedReader()?.readLines()
    }
}

typealias NativeArray = java.lang.reflect.Array
@Suppress("UNCHECKED_CAST")
fun <T> T.deepCopy(): T {
    return when (this) {
        is Array<*> -> {
            val type = this.javaClass.componentType
            type?.let {
                NativeArray.newInstance(it, size).also {
                    this.forEachIndexed { i, item ->
                        NativeArray.set(it, i, item.deepCopy())
                    }
                }
            } as T
        }
        is MutableList<*> -> this.mapTo(mutableListOf()) { it.deepCopy() } as T
        //is List<*> -> this.map { it.deepCopy() } as T
        is Cloneable -> this.javaClass.getDeclaredMethod("clone").let {
            it.isAccessible = true
            it.invoke(this) as T
        }
        else -> this
    }
}