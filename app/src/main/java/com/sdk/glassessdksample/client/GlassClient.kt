package com.sdk.glassessdksample.client

import android.content.Context
import androidx.lifecycle.LiveData
import java.io.File

interface GlassClient {
    // Observable LiveData representing the current connection state of the smart glasses
    val connectionState: LiveData<ConnectionState>
    
    // Observable LiveData representing the name of the currently connected glasses
    val connectedDeviceName: LiveData<String?>

    // Observable LiveData representing the list of scanned devices
    val scannedDevices: LiveData<List<GlassDevice>>

    // Starts Bluetooth LE discovery for HeyCyan glasses
    fun startScan(context: Context)

    // Stops Bluetooth LE discovery
    fun stopScan(context: Context)

    // Connects to a specific HeyCyan glasses device
    fun connect(context: Context, device: GlassDevice)

    // Disconnects from the currently connected glasses
    fun disconnect()

    // Triggers photo capture on the glasses and returns the downloaded file
    suspend fun takePicture(context: Context): File?
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class GlassDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isReal: Boolean
)
