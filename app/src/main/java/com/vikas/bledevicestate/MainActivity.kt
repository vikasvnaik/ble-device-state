package com.vikas.bledevicestate

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {
    val TAG = MainActivity::class.java.simpleName
    private val REQUEST_PERMISSION = 101
    private var permissionManager: PermissionManager? = null
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    @RequiresApi(Build.VERSION_CODES.S)
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionManager = PermissionManager(this)
        if (isAllPermissionsPermitted()) {
            checkBluetoothStatus()
            startService(Intent(this, BluetoothLeService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction("ACTION_DATA_AVAILABLE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        stopService(Intent(this, BluetoothLeService::class.java))
    }

    var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val batteryLevel = intent.getIntExtra("DEVICE_BATTERY", 0)
            Log.v(
                TAG,
                "characteristic.getStringValue(1) = $batteryLevel"
            )
            findViewById<TextView>(R.id.status).visibility = View.GONE
            findViewById<TextView>(R.id.state_of_charge_title).visibility = View.VISIBLE
            findViewById<TextView>(R.id.distance_to_empty).visibility = View.VISIBLE
            findViewById<BatteryView>(R.id.battery_view).visibility = View.VISIBLE
            //findViewById<ImageView>(R.id.distance_to_empty_icon).visibility = View.VISIBLE
            findViewById<TextView>(R.id.distance_to_empty_tittle).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.state_of_charge_ll).visibility = View.VISIBLE

            findViewById<BatteryView>(R.id.battery_view).setPercent(batteryLevel)
            findViewById<TextView>(R.id.state_of_charge).text = "$batteryLevel%"
            val distance = (batteryLevel * 1.2)
            findViewById<TextView>(R.id.distance_to_empty).text = String.format("%.1f", distance) + "km"//"${(batteryLevel * 1.2)} km"
        }
    }

    private fun isAllPermissionsPermitted(): Boolean {
        for (permission in locationPermissions) {
            if (permissionManager?.hasPermission(permission, this) != true) {
                checkPermission(permission)
                return false
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (permission in bluetoothPermissions) {
                if (permissionManager?.hasPermission(permission, this) != true) {
                    Log.d(TAG,"Denied permissions : $permission")
                    checkPermission(permission)
                    return false
                }
            }
        }
        return true
    }

    private fun checkPermission(permission: String) {
        val permissions = when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION ->
                locationPermissions

            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothPermissions
                } else {
                    arrayOf()
                }

            else -> {
                arrayOf()
            }
        }
        permissionManager?.checkPermission(
            this,
            permission,
            object : PermissionManager.PermissionAskListener {
                override fun onNeedPermission() {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        permissions,
                        REQUEST_PERMISSION
                    )
                }

                override fun onPermissionPreviouslyDenied() {
                    when (permission) {
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION ->
                            openAppSetting(
                                getString(R.string.goto_settings_to_grant_location_permission)
                            )

                        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT ->
                            openAppSetting(
                                getString(R.string.goto_settings_to_grant_bluetooth_permission)
                            )
                    }

                }

                override fun onPermissionPreviouslyDeniedWithNeverAskAgain() {
                    Log.d(TAG,"Permission to check : $permission")
                    openAppSetting(getString(R.string.goto_settings_to_grant_permission))
                }

                override fun onPermissionGranted() {
                    if (isAllPermissionsPermitted()) {
                        checkBluetoothStatus()
                        startService(Intent(this@MainActivity, BluetoothLeService::class.java))
                    }
                }
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION -> {
                if (isAllPermissionsPermitted()) {
                    checkBluetoothStatus()
                    startService(Intent(this@MainActivity, BluetoothLeService::class.java))
                }
            }
        }
    }

    private fun openAppSetting(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun checkBluetoothStatus() {
        try {
            val mBtAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBtAdapter != null) {
                if (!mBtAdapter.isEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Toast.makeText(this, "Please enable Bluetooth and relaunch the app", Toast.LENGTH_LONG).show()
                    }
                    mBtAdapter.enable()
                    Handler(Looper.getMainLooper()).postDelayed({
                            startService(Intent(this@MainActivity, BluetoothLeService::class.java))
                    }, 1000)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}