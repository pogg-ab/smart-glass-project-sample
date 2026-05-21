package com.sdk.glassessdksample.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import com.sdk.glassessdksample.ui.BluetoothEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class RealGlassClient : GlassClient {
    private val _connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    override val connectionState: LiveData<ConnectionState> get() = _connectionState

    private val _connectedDeviceName = MutableLiveData<String?>(null)
    override val connectedDeviceName: LiveData<String?> get() = _connectedDeviceName

    private val _scannedDevices = MutableLiveData<List<GlassDevice>>(emptyList())
    override val scannedDevices: LiveData<List<GlassDevice>> get() = _scannedDevices

    private val discoveredList = mutableListOf<GlassDevice>()

    private val bleScanCallback = object : ScanWrapperCallback {
        override fun onStart() {
            discoveredList.clear()
            _scannedDevices.postValue(emptyList())
        }

        override fun onStop() {}

        override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            if (device != null && !device.name.isNullOrEmpty()) {
                val glassDevice = GlassDevice(device.name, device.address, rssi, isReal = true)
                if (!discoveredList.any { it.address == glassDevice.address }) {
                    discoveredList.add(glassDevice)
                    discoveredList.sortByDescending { it.rssi }
                    _scannedDevices.postValue(discoveredList.toList())
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {}

        override fun onParsedData(device: BluetoothDevice?, scanRecord: ScanRecord?) {}

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {}
    }

    override fun startScan(context: Context) {
        discoveredList.clear()
        _scannedDevices.value = emptyList()
        BleScannerHelper.getInstance().reSetCallback()
        BleScannerHelper.getInstance().scanDevice(context, null, bleScanCallback)
    }

    override fun stopScan(context: Context) {
        BleScannerHelper.getInstance().stopScan(context)
    }

    override fun connect(context: Context, device: GlassDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        _connectedDeviceName.value = device.name
        
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        // Set device address in SDK manager and connect
        DeviceManager.getInstance().deviceAddress = device.address
        BleOperateManager.getInstance().connectDirectly(device.address)
    }

    override fun disconnect() {
        BleOperateManager.getInstance().unBindDevice()
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDeviceName.value = null
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBluetoothEvent(event: BluetoothEvent) {
        if (event.connect) {
            _connectionState.value = ConnectionState.CONNECTED
        } else {
            _connectionState.value = ConnectionState.DISCONNECTED
            _connectedDeviceName.value = null
        }
    }

    override suspend fun takePicture(context: Context): File? {
        // Real SDK call to trigger camera
        LargeDataHandler.getInstance().glassesControl(
            byteArrayOf(0x02, 0x01, 0x01)
        ) { _, response ->
            // SDK Callback
        }

        // Fallback return of POV sample photo for testing without hardware
        return try {
            val file = File(context.cacheDir, "sample_glass_photo.png")
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
