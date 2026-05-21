package com.sdk.glassessdksample.client

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import java.io.File

class GlassClientProvider private constructor() : GlassClient {
    private val realClient = RealGlassClient()
    private val mockClient = MockGlassClient()

    private val _connectionState = MediatorLiveData<ConnectionState>()
    override val connectionState: LiveData<ConnectionState> get() = _connectionState

    private val _connectedDeviceName = MediatorLiveData<String?>()
    override val connectedDeviceName: LiveData<String?> get() = _connectedDeviceName

    private val _scannedDevices = MediatorLiveData<List<GlassDevice>>()
    override val scannedDevices: LiveData<List<GlassDevice>> get() = _scannedDevices

    private var activeClient: GlassClient = realClient
    private var isFallbackScheduled = false
    private val handler = Handler(Looper.getMainLooper())

    private val fallbackRunnable = Runnable {
        // If we are scanning and no real devices were discovered, automatically switch to mock mode
        if (activeClient == realClient && (realClient.scannedDevices.value.isNullOrEmpty())) {
            switchToMockClient()
        }
    }

    init {
        // Configure MediatorLiveData to delegate to the currently active client
        _connectionState.addSource(realClient.connectionState) { if (activeClient == realClient) _connectionState.value = it }
        _connectionState.addSource(mockClient.connectionState) { if (activeClient == mockClient) _connectionState.value = it }

        _connectedDeviceName.addSource(realClient.connectedDeviceName) { if (activeClient == realClient) _connectedDeviceName.value = it }
        _connectedDeviceName.addSource(mockClient.connectedDeviceName) { if (activeClient == mockClient) _connectedDeviceName.value = it }

        _scannedDevices.addSource(realClient.scannedDevices) { if (activeClient == realClient) _scannedDevices.value = it }
        _scannedDevices.addSource(mockClient.scannedDevices) { if (activeClient == mockClient) _scannedDevices.value = it }
    }

    private fun switchToMockClient() {
        activeClient = mockClient
        // Trigger scan on the mock client so it publishes the mock devices
        mockClient.startScan(contextForScan ?: return)
        // Update mediator sources to ensure immediate UI feedback
        _connectionState.value = mockClient.connectionState.value
        _connectedDeviceName.value = mockClient.connectedDeviceName.value
        _scannedDevices.value = mockClient.scannedDevices.value
    }

    private fun switchToRealClient() {
        activeClient = realClient
        _connectionState.value = realClient.connectionState.value
        _connectedDeviceName.value = realClient.connectedDeviceName.value
        _scannedDevices.value = realClient.scannedDevices.value
    }

    private var contextForScan: Context? = null

    override fun startScan(context: Context) {
        contextForScan = context
        switchToRealClient()
        
        // Start real scan
        realClient.startScan(context)

        // Schedule fallback check in 4 seconds
        handler.removeCallbacks(fallbackRunnable)
        handler.postDelayed(fallbackRunnable, 4000)
    }

    override fun stopScan(context: Context) {
        handler.removeCallbacks(fallbackRunnable)
        realClient.stopScan(context)
        mockClient.stopScan(context)
    }

    override fun connect(context: Context, device: GlassDevice) {
        // Choose connecting client based on the device properties
        activeClient = if (device.isReal) realClient else mockClient
        activeClient.connect(context, device)
    }

    override fun disconnect() {
        activeClient.disconnect()
        // Reset to real client by default for the next flow
        switchToRealClient()
    }

    override suspend fun takePicture(context: Context): File? {
        return activeClient.takePicture(context)
    }

    companion object {
        @Volatile
        private var instance: GlassClientProvider? = null

        fun getInstance(): GlassClientProvider {
            return instance ?: synchronized(this) {
                instance ?: GlassClientProvider().also { instance = it }
            }
        }
    }
}
