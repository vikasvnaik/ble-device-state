package com.vikas.bledevicestate

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
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
            startService(Intent(this, BluetoothLeService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction("ACTION_DATA_AVAILABLE")
        registerReceiver(receiver, filter)
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
            findViewById<TextView>(R.id.state_of_charge).text = batteryLevel.toString()
            findViewById<TextView>(R.id.distance_to_empty).text = "${(batteryLevel * 1.2)} km"
            // do something with battery level
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
                    startService(Intent(this@MainActivity, BluetoothLeService::class.java))
                }
            }
        }
    }

    private fun openAppSetting(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}