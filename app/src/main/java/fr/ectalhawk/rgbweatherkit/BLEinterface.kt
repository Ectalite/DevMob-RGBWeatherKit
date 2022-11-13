package fr.ectalhawk.rgbweatherkit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
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

data class BLEinterface(val act: MainActivity, val context: Context) {
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

    private val list = act.findViewById<ListView>(R.id.deviceList)!!
    private val deviceList = ArrayList<BluetoothDevice>()
    private val deviceListAdapter = MyListAdapter(act,deviceList)

    private var lifecycleState = BLELifecycleState.Disconnected
        @SuppressLint("SetTextI18n")
        set(value) {
            field = value
            //Must run on UIThread or android 7 makes an exception
            act.runOnUiThread {
                myLogger("status = $value")
                val textViewLifecycleState : TextView = act.findViewById(R.id.textViewLifecycleState)
                textViewLifecycleState.text = "State: ${value.name}"
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
                Toast.makeText(context,"Could not connect to device please select another one", Toast.LENGTH_LONG).show()
            }
        }
    }

    //Fonctions pouvant être utilisés en instanciant la classe

    fun prepareAndStartBleScan(){
        myLogger("Start scanning")
        val scanButton = act.findViewById<Button>(R.id.scanButton)
        ensureBluetoothCanBeUsed { isSuccess, message ->
            myLogger(message)
            if (isSuccess) {
                scanButton.isEnabled = false
                safeStartBleScan()
            }
        }
    }

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


    //Methodes utilisées exclusivement par la classe
    fun sendPixel(posX: Int, posY: Int, color: Int) {
        val gatt = connectedGatt ?: run {
            myLogger("ERROR: write failed, no connected device")
            return
        }
        if (posX > 63 || posY > 31) {
            myLogger("ERROR: pixel is offgrid. PosX : $posX PosY : $posY")
            return
        }
        //characteristicForWrite.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        var bufferPosx = ByteArray(1)
        var bufferPosY = ByteArray(1)
        var bufferColor = ByteArray(3)
        var bufferSend = ByteArray(1)
        bufferPosx[0] = posX.toByte()
        pixelXCharacteristic!!.value = bufferPosx
        bufferPosY[0] = posY.toByte()
        pixelYCharacteristic!!.value = bufferPosY
        bufferColor = byteArrayOf(color.toByte(), color.shr(8).toByte(), color.shr(16).toByte())
        colorCharacteristic!!.value = bufferColor
        bufferSend[0] = 1
        sendCharacteristic!!.value = bufferSend
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
        val time : Long = 50 //Have to wait before sending the next one
        gatt.writeCharacteristic(pixelXCharacteristic)
        Thread.sleep(time)
        gatt.writeCharacteristic(pixelYCharacteristic)
        Thread.sleep(time)
        gatt.writeCharacteristic(colorCharacteristic)
        Thread.sleep(time)
        gatt.writeCharacteristic(sendCharacteristic)
        Thread.sleep(time)

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
        val scanButton = act.findViewById<Button>(R.id.scanButton)
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
            //myLogger("Got result!")


            if (result.device.name != null) {
                myLogger("onScanResult name=$name address= ${result.device?.address}")
                //val uuidList = getServiceUUIDsList(result)
                /*for (uuidnumber in 1 until uuidList.size) {
                    myLogger("UUID " + uuidnumber + " : " + uuidList[uuidnumber].toString())
                }*/
                if(!deviceList.contains(result.device)) {
                    deviceList.add(result.device)
                    deviceListAdapter.notifyDataSetChanged()
                }
            }
        }

        /*private fun getServiceUUIDsList(scanResult: ScanResult): List<UUID> {
            val parcelUuids = scanResult.scanRecord!!.serviceUuids
            val serviceList: MutableList<UUID> = ArrayList()
            for (i in parcelUuids.indices) {
                val serviceUUID = parcelUuids[i].uuid
                if (!serviceList.contains(serviceUUID)) serviceList.add(serviceUUID)
            }
            return serviceList
        }*/

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            myLogger("onBatchScanResults, ignoring")
        }

        override fun onScanFailed(errorCode: Int) {
            myLogger("onScanFailed errorCode=$errorCode")
            safeStopBleScan()
            lifecycleState = BLELifecycleState.Disconnected
            bleRestartLifecycle()
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
                    bleRestartLifecycle()
                }
            } else if ((status == 8) || (status == 133)) {
                //https://stackoverflow.com/questions/55529511/android-ble-connectgatt-timeout
                myLogger("Le telephone a du se reconnecter parce qu'il est malfaisant: $status")
                gatt.disconnect()
                gatt.close()
                //Fix #xxx1 https://stackoverflow.com/questions/45442838/type-checking-has-run-into-a-recursive-in-kotlin
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
                //bleRestartLifecycle()
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
                if(pixelXCharacteristic != null && pixelYCharacteristic != null &&
                    colorCharacteristic != null && sendCharacteristic != null){
                    myLogger("Found all 4 characteristics, YEAH !")
                    lifecycleState = BLELifecycleState.Connected
                    act.runOnUiThread { //FUCK android 7
                        safeStopBleScan() //Stop scanning to reduce logs
                    }
                    act.goToTest()
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

        //TODO: Dans le futur: override ces fonction pour récupérer des logs ?
        /*
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid == UUID.fromString(CHAR_FOR_READ_UUID)) {
                val strValue = characteristic.value.toString(Charsets.UTF_8)
                val log = "onCharacteristicRead " + when (status)
                {
                    BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                    else -> "error $status"
                }
                appendLog(log)
                runOnUiThread {
                    textViewReadValue.text = strValue
                }
            } else {
                appendLog("onCharacteristicRead unknown uuid $characteristic.uuid")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid == UUID.fromString(CHAR_FOR_WRITE_UUID)) {
                val log: String = "onCharacteristicWrite " + when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "OK"
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "not allowed"
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "invalid length"
                    else -> "error $status"
                }
                appendLog(log)
            } else {
                appendLog("onCharacteristicWrite unknown uuid $characteristic.uuid")
            }
        }*/
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
            //val intentString = BluetoothAdapter.ACTION_REQUEST_ENABLE
            val requestCode = ENABLE_BLUETOOTH_REQUEST_CODE

            // set activity result handler
            activityResultHandlers[requestCode] = { result ->
                val isSuccess = result == Activity.RESULT_OK
                if (isSuccess || askType != AskType.InsistUntilSuccess) {
                    activityResultHandlers.remove(requestCode)
                    completion(isSuccess)
                } else {
                    // start activity for the request again
                   // startActivityForResult(Intent(intentString), requestCode)
                }
            }

            // start activity for the request
            //startActivityForResult(Intent(intentString), requestCode)
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

    private var activityResultHandlers = mutableMapOf<Int, (Int) -> Unit>()
    private var permissionResultHandlers = mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()


    private fun setConnectedGattToNull() {
        connectedGatt = null
        pixelXCharacteristic = null
        pixelYCharacteristic = null
        colorCharacteristic = null
        sendCharacteristic = null
    }

    private fun bleRestartLifecycle() {
        /*act.runOnUiThread {
            prepareAndStartBleScan()
        }*/
    }

    private fun hasPermissions(permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionArray(permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(act, permissions, requestCode)
    }

    private fun myLogger(message: String) {
        Log.d("BLEinterface", message)
    }
}
