package com.sdk.glassessdksample.client

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MockGlassClient : GlassClient {
    private val _connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    override val connectionState: LiveData<ConnectionState> get() = _connectionState

    private val _connectedDeviceName = MutableLiveData<String?>(null)
    override val connectedDeviceName: LiveData<String?> get() = _connectedDeviceName

    private val _scannedDevices = MutableLiveData<List<GlassDevice>>(emptyList())
    override val scannedDevices: LiveData<List<GlassDevice>> get() = _scannedDevices

    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false

    private val scanRunnable = Runnable {
        if (isScanning) {
            val mockDevices = listOf(
                GlassDevice("HeyCyan Air (Demo)", "71:33:1D:2C:CF:A0", -45, isReal = false),
                GlassDevice("HeyCyan Neo (Demo)", "3A:2C:12:F3:99:A5", -68, isReal = false),
                GlassDevice("HeyCyan Pro (Demo)", "99:D1:D4:FF:E2:08", -82, isReal = false)
            )
            _scannedDevices.postValue(mockDevices)
        }
    }

    override fun startScan(context: Context) {
        if (isScanning) return
        isScanning = true
        _scannedDevices.value = emptyList()
        // Simulate scanning delay before finding mock devices
        handler.postDelayed(scanRunnable, 1500)
    }

    override fun stopScan(context: Context) {
        isScanning = false
        handler.removeCallbacks(scanRunnable)
    }

    override fun connect(context: Context, device: GlassDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        _connectedDeviceName.value = device.name

        // Simulate a 1.5-second connection delay
        handler.postDelayed({
            _connectionState.value = ConnectionState.CONNECTED
        }, 1500)
    }

    override fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDeviceName.value = null
    }

    override suspend fun takePicture(context: Context): File? {
        // Simulate capture delay (camera trigger + image download)
        delay(2000)

        // Copy the high-fidelity sample image from assets to the cache directory
        return try {
            val file = File(context.cacheDir, "sample_glass_photo.png")
            // Always overwrite to simulate a newly taken picture
            context.assets.open("sample_glass_photo.png").use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
