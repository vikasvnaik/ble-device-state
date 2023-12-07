package com.vikas.bledevicestate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivityBle"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startService(Intent(this, BluetoothLeService::class.java))
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction("ACTION_DATA_AVAILABLE")
        registerReceiver(receiver, filter)
    }

    var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val batteryLevel = intent.getIntExtra("DEVICE_BATTERY", 0)
            Log.v(
                TAG,
                "characteristic.getStringValue(1) = $batteryLevel"
            )
            findViewById<TextView>(R.id.state_of_charge).text = batteryLevel.toString()
            // do something with battery level
        }
    }
}