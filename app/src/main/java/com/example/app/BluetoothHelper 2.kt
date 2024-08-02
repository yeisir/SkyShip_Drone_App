package com.example.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class BluetoothHelper(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val discoveredDevices = ArrayList<BluetoothDevice>()
    private var discoveryCallback: ((List<BluetoothDevice>) -> Unit)? = null

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (!discoveredDevices.contains(it)) {
                        discoveredDevices.add(it)
                        discoveryCallback?.invoke(discoveredDevices)
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                Log.d(TAG, "Discovery finished")
            }
        }
    }

    init {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        context.registerReceiver(discoveryReceiver, filter)
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(callback: (List<BluetoothDevice>) -> Unit) {
        discoveryCallback = callback
        discoveredDevices.clear()
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, callback: (BluetoothSocket?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Thread {
                var socket: BluetoothSocket? = null
                try {
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
                    socket?.connect()
                } catch (e: IOException) {
                    Log.e(TAG, "Could not connect to device", e)
                    try {
                        socket?.close()
                    } catch (closeException: IOException) {
                        Log.e(TAG, "Could not close the socket", closeException)
                    }
                }
                callback(socket)
            }.start()
        } else {
            Log.e(TAG, "Bluetooth connect permission not granted")
            callback(null)
        }
    }

    fun cleanup() {
        context.unregisterReceiver(discoveryReceiver)
    }

    companion object {
        private const val TAG = "BluetoothHelper"
        private const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB" // UUID for SPP (Serial Port Profile)
    }
}
