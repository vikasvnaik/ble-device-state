package com.vikas.bledevicestate

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.UUID

class BluetoothLeService : Service() {
    val TAG =  BluetoothLeService::class.java.simpleName
    var scanner: BluetoothLeScanner? = null
    var BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    var BATTERY_LEVEL_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    val EXTRAS_DEVICE_BATTERY = "DEVICE_BATTERY"
    public val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
    val scanResult: ArrayList<String> = ArrayList()
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startScanning()
    }

    override fun onDestroy() {
        super.onDestroy()

    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }

    fun startScanning(){
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        scanner = adapter.bluetoothLeScanner

        val serviceUUIDs = arrayOf(BATTERY_SERVICE_UUID)
        var filters: MutableList<ScanFilter?>? = null
        if (serviceUUIDs != null) {
            filters = ArrayList()
            for (serviceUUID in serviceUUIDs) {
                val filter = ScanFilter.Builder()
                    .setServiceData(ParcelUuid(serviceUUID), null)
                    .build()
                filters.add(filter)
            }
        }
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L)
            .build()
        Log.d(TAG, "scanner setup")
        if (scanner != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            Log.d(TAG, "BLE permission granted")
            scanner?.startScan(filters, scanSettings, scanCallback)
            Log.d(TAG, "scan started")
        }  else {
            Log.e(TAG, "could not get scanner object")
        }
    }

    var scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice =
                result.device // ...do whatever you want with this found device
            //if (device.address == "C3:B1:F2:7E:FC:C1"){
            Log.d(TAG, "scan result : ${device.name} ${device.address}")
            connectToDevice(device)
            //}
        }

        override fun onBatchScanResults(results: List<ScanResult?>?) {
            // Ignore for now
        }

        override fun onScanFailed(errorCode: Int) {
            // Ignore for now
        }
    }

    fun connectToDevice(device: BluetoothDevice){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        device.connectGatt(this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val  bluetoothGattCallback: BluetoothGattCallback =  object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.d(TAG, "onConnectionStateChange : $status $newState")
            if (newState == BluetoothProfile.STATE_CONNECTED){
                gatt?.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                getBattery(gatt!!)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(
                    TAG,
                    "characteristic.getStringValue(0) = " + characteristic.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT8,
                        0
                    )
                )
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }
    }

    fun getBattery(mGatt: BluetoothGatt) {
        val batteryService = mGatt.getService(BATTERY_SERVICE_UUID)
        if (batteryService == null) {
            return
        }
        val batteryLevel = batteryService.getCharacteristic(BATTERY_LEVEL_UUID)
        if (batteryLevel == null) {
            return
        }
        Log.i(TAG, mGatt.readCharacteristic(batteryLevel).toString())
        scanner?.stopScan(scanCallback)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent()
        intent.action = action
        Log.v(
            TAG,
            "characteristic.getStringValue(0) = " + characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8,
                0
            )
        )
        intent.putExtra(
            EXTRAS_DEVICE_BATTERY,
            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
        )
        sendBroadcast(intent)
    }
}