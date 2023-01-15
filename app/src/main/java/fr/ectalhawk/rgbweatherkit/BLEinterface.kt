package fr.ectalhawk.rgbweatherkit

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog.Builder
import androidx.core.app.ActivityCompat
import java.util.*


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private const val BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE = 3
private const val UUID_CHAR_PIXELX = "BADDCAFE-0000-0000-0000-000000000002"
private const val UUID_CHAR_PIXELY = "BADDCAFE-0000-0000-0000-000000000003"
private const val UUID_CHAR_COLOR = "BADDCAFE-0000-0000-0000-000000000004"
private const val UUID_CHAR_SEND = "BADDCAFE-0000-0000-0000-000000000005"
private const val UUID_CHAR_TEXT = "BADDCAFE-0000-0000-0000-000000000006"

data class BLEinterface(val act: MenuPrincipal, val context: Context) {
    //Enum and variables of class
    enum class BLELifecycleState {
        Disconnected,
        Scanning,
        Connecting,
        ConnectedDiscovering,
        ConnectedSubscribing,
        Connected
    }
    enum class AskType {
        //AskOnce,
        InsistUntilSuccess
    }
    //L'enum, la classe et la variable sont utilisé en tant que buffer d'envoi de characteristic
    enum class CharacteristicEnum {
        PixelX,
        PixelY,
        Color,
        Text,
        Send
    }
    class CharacteristicList(characteristic_ : CharacteristicEnum, buffer_ : ByteArray) {
        val characteristic = characteristic_
        val buffer = buffer_
    }
    private val sendList = ArrayList<CharacteristicList>()

    //Cette méthode sert à récupérer l'adaptateur bluetooth d'android.
    private val bluetoothAdapter: BluetoothAdapter by lazy{
        val bluetoothManager: BluetoothManager? = act.getSystemService(BluetoothManager::class.java)
        if(bluetoothManager == null)
        {
            myLogger("BluetoothManager was null")
        }
        bluetoothManager!!.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private var isScanning = false
    var connectedGatt: BluetoothGatt? = null
    private var pixelXCharacteristic: BluetoothGattCharacteristic? = null
    private var pixelYCharacteristic: BluetoothGattCharacteristic? = null
    private var colorCharacteristic: BluetoothGattCharacteristic? = null
    private var sendCharacteristic: BluetoothGattCharacteristic? = null
    private var textCharacteristic: BluetoothGattCharacteristic? = null

    private val list = act.findViewById<ListView>(R.id.deviceList)!!
    private val deviceList = ArrayList<BluetoothDevice>()
    private val deviceListAdapter = MyListAdapter(act,deviceList)
    //Pour la prochaine ligne, BLEinterface doit être initialisé depuis FragmentBTSettings
    private val scanButton = act.findViewById<Button>(R.id.scanButton)
    private val textViewLifecycleState : TextView = act.findViewById(R.id.textViewLifecycleState)

    //Cette variable sert à voir l'état de la connection Blueooth
    private var lifecycleState = BLELifecycleState.Disconnected
        set(value) {
            field = value
            //Must run on UIThread or android 7 makes an exception
            act.runOnUiThread {
                myLogger("status = $value")
                textViewLifecycleState.text = buildString {
                    append(R.string.status.toString())
                    append(value.name)
                }
            }
        }

    //Constructeur appelé à l'instanciation
    init {
        list.adapter = deviceListAdapter
        list.setOnItemClickListener{ adapterView, _, position, _ ->
            val itemAtPos = adapterView.getItemAtPosition(position) as? BluetoothDevice
            //val itemIdAtPos = adapterView.getItemIdAtPosition(position)
            if(itemAtPos != null)
            {
                connectToDevice(itemAtPos)
            }
            else
            {
                Toast.makeText(context,R.string.DeviceNull, Toast.LENGTH_LONG).show()
            }
        }
    }

    //Fonctions pouvant être utilisés en instanciant la classe
    fun prepareAndStartBleScan(){
        myLogger("Start scanning")
        ensureBluetoothCanBeUsed { isSuccess, message ->
            if(verifyLocation()) //verify if localisation is on or ask to go to settings
            {
                myLogger(message)
                if (isSuccess) {
                    scanButton.isEnabled = false
                    safeStartBleScan()
                }
            }
        }
    }

    //Cette fonction sert à envoyer un pixel sur la matrice de LED
    @Suppress("DEPRECATION")
    fun sendPixel(posX: Int, posY: Int, color: Int, bDisplay : Boolean) {
        //On vient verifier que les paramètres sont bons
        val gatt = connectedGatt ?: run {
            myLogger("ERROR: write failed, no connected device")
            return
        }
        if (posX > 63 || posY > 31) {
            myLogger("ERROR: pixel is offgrid. PosX : $posX PosY : $posY")
            return
        }
        //on vient remplir les ByteArray qui seront envoyer
        val bufferPosx = ByteArray(1)
        val bufferPosY = ByteArray(1)
        val bufferSend = ByteArray(1)
        bufferPosx[0] = posX.toByte()
        bufferPosY[0] = posY.toByte()
        val bufferColor = byteArrayOf(color.shr(16).toByte(), color.shr(8).toByte(), color.toByte())
        bufferSend[0] = ((0 shl 2) or ((if (bDisplay) 1 else 0) shl 1) or (1 shl 0)).toByte()
        //                0 = PixelMode      1 = Display                    1 = Send

        //Permission check
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                myLogger("sendPixel n'a pas les permissions nécessaires pour envoyer les données")
                val intent = Intent(act, NoBLEAuthorization::class.java)
                act.startActivity(intent)
                return
            }
        }
        //On vient ajouer les valeurs à envoyer dans la liste d'envoi
        sendList.add(CharacteristicList(CharacteristicEnum.PixelY,bufferPosY))
        sendList.add(CharacteristicList(CharacteristicEnum.Color,bufferColor))
        sendList.add(CharacteristicList(CharacteristicEnum.Send,bufferSend))

        //writeCharacteristic étant deprecated depuis Android Tiramisu, on vient vérifier la version de SDK
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            gatt.writeCharacteristic(pixelXCharacteristic!!,bufferPosx,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }
        else
        {
            pixelXCharacteristic!!.value = bufferPosx
            gatt.writeCharacteristic(pixelXCharacteristic)
        }
    }

    //Cette méthode sert à effacer la matrice.
    @Suppress("DEPRECATION")
    fun clearMatrix() {
        val gatt = connectedGatt ?: run {
            myLogger("ERROR: write failed, no connected device")
            return
        }
        val bufferSend = ByteArray(1)
        bufferSend[0] = ((1 shl 3) or (1 shl 0)).toByte()
        //                Bit 3 : Clear Matrix Bit 0: Send

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                myLogger("sendPixel n'a pas les permissions nécessaires pour envoyer les données")
                val intent = Intent(act, NoBLEAuthorization::class.java)
                act.startActivity(intent)
                return
            }
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            gatt.writeCharacteristic(sendCharacteristic!!,bufferSend,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }
        else
        {
            sendCharacteristic!!.value = bufferSend
            gatt.writeCharacteristic(sendCharacteristic)
        }
    }

    //Cette méthode sert à envoyer du texte sur la matrice
    @Suppress("DEPRECATION")
    fun sendText(posX: Int, posY: Int, color: Int, text: String, bDisplay : Boolean) {
        val gatt = connectedGatt ?: run {
            myLogger("ERROR: write failed, no connected device")
            return
        }
        if (posX > 63 || posY > 31) {
            myLogger("ERROR: pixel is offgrid. PosX : $posX PosY : $posY")
            return
        }
        val bufferPosx = ByteArray(1)
        val bufferPosY = ByteArray(1)
        val bufferSend = ByteArray(1)
        val bufferText = ByteArray(20)
        bufferPosx[0] = posX.toByte()
        bufferPosY[0] = posY.toByte()
        val bufferColor = byteArrayOf(color.shr(16).toByte(), color.shr(8).toByte(), color.toByte())
        bufferSend[0] = ((1 shl 2) or ((if (bDisplay) 1 else 0) shl 1) or (1 shl 0)).toByte()
        //                1 = TextMode          1 = Display                1 = Send

        myLogger("Received this text to send: $text")
        for (charNumber in text.indices)
        {
            bufferText[charNumber] = text[charNumber].code.toByte()
            //myLogger("${bufferText[charNumber]} | ${text[charNumber].code} | ${text[charNumber]}")
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                myLogger("sendPixel n'a pas les permissions nécessaires pour envoyer les données")
                val intent = Intent(act, NoBLEAuthorization::class.java)
                act.startActivity(intent)
                return
            }
        }

        sendList.add(CharacteristicList(CharacteristicEnum.PixelY,bufferPosY))
        sendList.add(CharacteristicList(CharacteristicEnum.Color,bufferColor))
        sendList.add(CharacteristicList(CharacteristicEnum.Text,bufferText))
        sendList.add(CharacteristicList(CharacteristicEnum.Send,bufferSend))

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            gatt.writeCharacteristic(pixelXCharacteristic!!,bufferPosx,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }
        else
        {
            pixelXCharacteristic!!.value = bufferPosx
            gatt.writeCharacteristic(pixelXCharacteristic)
        }
    }


    //Methodes utilisées exclusivement par la classe
    //Elles viennent en grande partie du projet BLE Library, mais on été réécrite pour ne plus contenir de warning et
    //pour convenir à nos besoins
    //https://github.com/NordicSemiconductor/Android-BLE-Library
    private fun connectToDevice(device : BluetoothDevice)
    {
        lifecycleState = BLELifecycleState.Connecting
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                myLogger("connectToDevice n'a pas les permissions nécessaires pour se connecter")
                val intent = Intent(act, NoBLEAuthorization::class.java)
                act.startActivity(intent)
                return
            }
        }
        act.runOnUiThread {
            device.connectGatt(context, false, gattCallback, TRANSPORT_LE)
            //YOU HAVE TO SET TRANSPORT_LE OR IT WONT WORK
            //https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
        }
    }

    private fun safeStartBleScan() {
        if (isScanning) {
            myLogger("Already BLE scanning")
            return
        }

        isScanning = true
        lifecycleState = BLELifecycleState.Scanning
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            if (ActivityCompat.checkSelfPermission( context, Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                myLogger("safeStartBleScan n'a pas les permissions nécessaires pour scanner")
                val intent = Intent(act, NoBLEAuthorization::class.java)
                act.startActivity(intent)
                return
            }
        }
        myLogger("Starting BLE scan")
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            safeStopBleScan()
        },10000)
        bleScanner.startScan(scanCallback)
    }

    private fun safeStopBleScan() {
        scanButton.isEnabled = true
        if (!isScanning) {
            myLogger("BLE scanning already stopped")
            return
        }

        myLogger("Stopping BLE scan")
        isScanning = false
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                myLogger("safeStopBleScan n'a pas les permissions nécessaires pour stopper le scan")
                val intent = Intent(act, NoBLEAuthorization::class.java)
                act.startActivity(intent)
                return
            }
        }
        bleScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    myLogger("scanCallback n'a pas les permissions nécessaires pour récupérer le nom du device")
                    val intent = Intent(act, NoBLEAuthorization::class.java)
                    act.startActivity(intent)
                    return
                }
            }
            val name: String? = result.scanRecord?.deviceName ?: result.device.name

            if (result.device.name != null) {
                myLogger("onScanResult name=$name address= ${result.device?.address}")
                if(!deviceList.contains(result.device)) {
                    deviceList.add(result.device)
                    deviceListAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            myLogger("onBatchScanResults, ignoring")
        }

        override fun onScanFailed(errorCode: Int) {
            myLogger("onScanFailed errorCode=$errorCode")
            safeStopBleScan()
            lifecycleState = BLELifecycleState.Disconnected
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS ) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    myLogger("Connected to $deviceAddress")

                    // recommended on UI thread https://punchthrough.com/android-ble-guide/
                    Handler(Looper.getMainLooper()).post {
                        lifecycleState = BLELifecycleState.ConnectedDiscovering
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (ActivityCompat.checkSelfPermission(
                                    context, Manifest.permission.BLUETOOTH_CONNECT
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                myLogger("onConnectionStateChange n'a pas les permissions nécessaires pour se deconnecter")
                                val intent = Intent(act, NoBLEAuthorization::class.java)
                                act.startActivity(intent)
                            }
                        }
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    myLogger("Disconnected from $deviceAddress")
                    setConnectedGattToNull()
                    gatt.close()
                    lifecycleState = BLELifecycleState.Disconnected
                    //Si on se fait déconnecter, on l'écrit sur le menu home
                    AppBLEInterface.connectedDevice = context.getString(R.string.disconnected)
                    //On désactive la navbar et on revient sur le menu de sélection des devices
                    act.runOnUiThread {
                        act.deactivateBottomNavigation()
                        act.replaceFragment(FragmentBTSettings())
                        act.binding.bottomNavigation.selectedItemId = R.id.navigation_btsettings
                    }
                }
            } else if ((status == 8) || (status == 133)) {
                //https://stackoverflow.com/questions/55529511/android-ble-connectgatt-timeout
                //Si la connection BLE fait un timeout
                myLogger("Le telephone a du se reconnecter parce qu'il est malfaisant: $status")
                gatt.disconnect()
                gatt.close()
                //Fix https://stackoverflow.com/questions/45442838/type-checking-has-run-into-a-recursive-in-kotlin
                val mHandler = Handler(context.mainLooper)
                // Connect to BLE device from mHandler
                mHandler.post {
                    gatt.device.connectGatt(context, false, this)
                }
            } else {

                myLogger("ERROR: onConnectionStateChange status=$status deviceAddress=$deviceAddress, disconnecting")

                setConnectedGattToNull()
                gatt.close()
                lifecycleState = BLELifecycleState.Disconnected
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            myLogger("onServicesDiscovered services.count=${gatt.services.size} status=$status")
            lifecycleState = BLELifecycleState.ConnectedSubscribing

            if (status == 129 /*GATT_INTERNAL_ERROR*/) {
                // it should be a rare case, this article recommends to disconnect:
                // https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
                myLogger("ERROR: status=129 (GATT_INTERNAL_ERROR), disconnecting")
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        myLogger("sonServicesDiscovered n'a pas les permissions nécessaires pour se deconnecter")
                        val intent = Intent(act, NoBLEAuthorization::class.java)
                        act.startActivity(intent)
                        return
                    }
                }
                gatt.disconnect()
                return
            }

            //On scan tous les services disponibles sur l'appareil pour trouver les 4 charactérstiques
            val services = gatt.services
            for (service in services){
                pixelXCharacteristic = service.getCharacteristic(UUID.fromString(UUID_CHAR_PIXELX))
                pixelYCharacteristic = service.getCharacteristic(UUID.fromString(UUID_CHAR_PIXELY))
                colorCharacteristic = service.getCharacteristic(UUID.fromString(UUID_CHAR_COLOR))
                sendCharacteristic = service.getCharacteristic(UUID.fromString(UUID_CHAR_SEND))
                textCharacteristic = service.getCharacteristic(UUID.fromString(UUID_CHAR_TEXT))
                if(pixelXCharacteristic != null && pixelYCharacteristic != null &&
                    colorCharacteristic != null && sendCharacteristic != null &&
                    textCharacteristic != null){
                    myLogger("Found all 5 characteristics, YEAH !")
                    lifecycleState = BLELifecycleState.Connected
                    act.runOnUiThread { //FUCK android 7
                        safeStopBleScan() //Stop scanning to reduce logs
                    }
                    //On récupère le nom du device pour pouvoir l'afficher dans le menu Home.
                    AppBLEInterface.connectedDevice = gatt.device.name
                    //Une fois connecté, on active les boutons de la navbar et on va au menu principal
                    act.runOnUiThread {
                        act.activateBottomNavigation()
                        act.replaceFragment(FragmentHome())
                        act.binding.bottomNavigation.selectedItemId = R.id.navigation_home
                    }
                    break
                }
            }

            connectedGatt = gatt

            if(pixelXCharacteristic == null || pixelYCharacteristic == null ||
                colorCharacteristic == null || sendCharacteristic == null){
                myLogger("Could not find the right chracteristics on the device, disconnecting...")
                gatt.disconnect()
            }
        }

        //Utilisé pour envoyé chaque éléments de SendList une fois celui d'avant envoyé
        @Suppress("DEPRECATION")
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if(sendList.isNotEmpty()){
                val charToSend = sendList[0]
                sendList.removeAt(0)
                //Permission check
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        myLogger("sendPixel n'a pas les permissions nécessaires pour envoyer les données")
                        val intent = Intent(act, NoBLEAuthorization::class.java)
                        act.startActivity(intent)
                        return
                    }
                }
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when(charToSend.characteristic){
                        CharacteristicEnum.PixelX -> {
                            gatt.writeCharacteristic(pixelXCharacteristic!!,charToSend.buffer,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        }
                        CharacteristicEnum.PixelY -> {
                            gatt.writeCharacteristic(pixelYCharacteristic!!,charToSend.buffer,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        }
                        CharacteristicEnum.Color -> {
                            gatt.writeCharacteristic(colorCharacteristic!!,charToSend.buffer,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        }
                        CharacteristicEnum.Text -> {
                            gatt.writeCharacteristic(textCharacteristic!!,charToSend.buffer,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        }
                        CharacteristicEnum.Send -> {
                            gatt.writeCharacteristic(sendCharacteristic!!,charToSend.buffer,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        }
                    }
                } else {
                    when(charToSend.characteristic){
                        CharacteristicEnum.PixelX -> {
                            pixelXCharacteristic!!.value = charToSend.buffer
                            gatt.writeCharacteristic(pixelXCharacteristic)
                        }
                        CharacteristicEnum.PixelY -> {
                            pixelYCharacteristic!!.value = charToSend.buffer
                            gatt.writeCharacteristic(pixelYCharacteristic)
                        }
                        CharacteristicEnum.Color -> {
                            colorCharacteristic!!.value = charToSend.buffer
                            gatt.writeCharacteristic(colorCharacteristic)
                        }
                        CharacteristicEnum.Text -> {
                            textCharacteristic!!.value = charToSend.buffer
                            gatt.writeCharacteristic(textCharacteristic)
                        }
                        CharacteristicEnum.Send -> {
                            sendCharacteristic!!.value = charToSend.buffer
                            gatt.writeCharacteristic(sendCharacteristic)
                        }
                    }
                }
            } else {
                //myLogger("Nothing to send anymore")
            }
        }
    }

    private fun ensureBluetoothCanBeUsed(completion: (Boolean, String) -> Unit) {
        grantBluetoothCentralPermissions(AskType.InsistUntilSuccess) { isGranted ->
            if (!isGranted) {
                completion(false, "Bluetooth permissions denied")
                myLogger("Bluetooth permissions denied, asking for Permission")
                return@grantBluetoothCentralPermissions
            }

            enableBluetooth(AskType.InsistUntilSuccess) { isEnabled ->
                if (!isEnabled) {
                    completion(false, "Bluetooth OFF")
                    myLogger("Bluetooth is off, enabling it")
                    return@enableBluetooth
                }

                grantLocationPermissionIfRequired(AskType.InsistUntilSuccess) { isGranted ->
                    if (!isGranted) {
                        completion(false, "Location permission denied")
                        myLogger("Location permission denied, asking it")
                        return@grantLocationPermissionIfRequired
                    }

                    completion(true, "Bluetooth ON, permissions OK, ready")
                }
            }
        }
    }

    private fun enableBluetooth(askType: AskType, completion: (Boolean) -> Unit) {
        if (bluetoothAdapter.isEnabled) {
            completion(true)
        } else {
            val requestCode = ENABLE_BLUETOOTH_REQUEST_CODE

            // set activity result handler
            activityResultHandlers[requestCode] = { result ->
                val isSuccess = result == Activity.RESULT_OK
                if (isSuccess || askType != AskType.InsistUntilSuccess) {
                    activityResultHandlers.remove(requestCode)
                    completion(isSuccess)
                }
            }
        }
    }

    private fun grantBluetoothCentralPermissions(askType: AskType, completion: (Boolean) -> Unit) {
        val wantedPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            emptyArray()
        }

        if (wantedPermissions.isEmpty() || hasPermissions(wantedPermissions)) {
            completion(true)
        } else {
            act.runOnUiThread {
                val requestCode = BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE

                // set permission result handler
                permissionResultHandlers[requestCode] = { _ /*permissions*/, grantResults ->
                    val isSuccess = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                    if (isSuccess || askType != AskType.InsistUntilSuccess) {
                        permissionResultHandlers.remove(requestCode)
                        completion(isSuccess)
                    } else {
                        // request again
                        requestPermissionArray(wantedPermissions, requestCode)
                    }
                }

                requestPermissionArray(wantedPermissions, requestCode)
            }
        }
    }

    private fun grantLocationPermissionIfRequired(askType: AskType, completion: (Boolean) -> Unit) {
        val wantedPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // BLUETOOTH_SCAN permission has flag "neverForLocation", so location not needed
            completion(true)
        } else if (hasPermissions(wantedPermissions)) {
            completion(true)
        } else {
            act.runOnUiThread {
                val requestCode = LOCATION_PERMISSION_REQUEST_CODE

                // prepare motivation message
                val builder = Builder(context)
                builder.setTitle("Location permission required")
                builder.setMessage("BLE advertising requires location access, starting from Android 6.0")
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissionArray(wantedPermissions, requestCode)
                }
                builder.setCancelable(false)

                // set permission result handler
                permissionResultHandlers[requestCode] = { _, grantResults ->
                    val isSuccess = grantResults.firstOrNull() != PackageManager.PERMISSION_DENIED
                    if (isSuccess || askType != AskType.InsistUntilSuccess) {
                        permissionResultHandlers.remove(requestCode)
                        completion(isSuccess)
                        myLogger("Permission was given")
                    } else {
                        // show motivation message again
                        builder.create().show()
                    }
                }

                // show motivation message
                builder.create().show()
            }
        }
    }

    private fun verifyLocation(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnabled = false
        var networkEnabled = false

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            myLogger(ex.toString())
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
            myLogger(ex.toString())
        }

        if (!gpsEnabled && !networkEnabled) {
            // notify user
            Builder(act)
                .setMessage(R.string.gps_network_not_enabled)
                .setPositiveButton(R.string.open_location_settings
                ) { _, _ ->
                    act.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                }
                .setNegativeButton(R.string.Cancel
                ) { _, _ ->
                    myLogger("La localisation est éteinte")
                    val intent = Intent(act, NoBLEAuthorization::class.java)
                    act.startActivity(intent)
                }
                .setOnDismissListener {
                    Toast.makeText(context,R.string.dismissLocationError, Toast.LENGTH_LONG).show()
                }
                .show()
            return false
        }
        else
        {
            return true
        }
    }

    private var activityResultHandlers = mutableMapOf<Int, (Int) -> Unit>()
    private var permissionResultHandlers = mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()


    private fun setConnectedGattToNull() {
        connectedGatt = null
        pixelXCharacteristic = null
        pixelYCharacteristic = null
        colorCharacteristic = null
        sendCharacteristic = null
    }

    private fun hasPermissions(permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionArray(permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(act, permissions, requestCode)
    }

    fun myLogger(message: String) {
        Log.d("BLEinterface", message)
    }
}
