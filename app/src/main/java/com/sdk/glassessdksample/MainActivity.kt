package com.sdk.glassessdksample

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hjq.permissions.OnPermissionCallback
import com.sdk.glassessdksample.client.ConnectionState
import com.sdk.glassessdksample.client.GlassClientProvider
import com.sdk.glassessdksample.client.GlassDevice
import com.sdk.glassessdksample.client.GlassDeviceAdapter
import com.sdk.glassessdksample.databinding.AcitivytMainBinding
import com.sdk.glassessdksample.ui.hasBluetooth
import com.sdk.glassessdksample.ui.requestBluetoothPermission
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: AcitivytMainBinding
    private val glassClient = GlassClientProvider.getInstance()
    private lateinit var deviceAdapter: GlassDeviceAdapter
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AcitivytMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupClickListeners()
        observeClientState()
    }

    private fun setupRecyclerView() {
        deviceAdapter = GlassDeviceAdapter(emptyList()) { device ->
            // Tap a device -> Connect
            connectToDevice(device)
        }
        binding.rvScannedDevices.layoutManager = LinearLayoutManager(this)
        binding.rvScannedDevices.adapter = deviceAdapter
    }

    private fun setupClickListeners() {
        // Connect Button -> Starts scanning
        binding.btnActionConnect.setOnClickListener {
            if (!hasBluetooth(this)) {
                requestBluetoothPermission(this, object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                        if (all) startScanning()
                    }
                    override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        Toast.makeText(this@MainActivity, "Bluetooth permission required to connect", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                startScanning()
            }
        }

        // Take Picture Button
        binding.btnActionTakePicture.setOnClickListener {
            capturePhoto()
        }

        // Disconnect Button
        binding.btnActionDisconnect.setOnClickListener {
            glassClient.disconnect()
        }

        // Back button on full screen photo preview
        binding.btnPhotoBack.setOnClickListener {
            binding.layoutFullscreenPhoto.visibility = View.GONE
            binding.layoutConnected.visibility = View.VISIBLE
        }
    }

    private fun startScanning() {
        if (isScanning) return
        isScanning = true
        
        binding.btnActionConnect.text = "SCANNING"
        binding.btnActionConnect.isEnabled = false
        binding.tvScanStatus.text = "Searching for HeyCyan smart glasses..."
        binding.scanProgressBar.visibility = View.VISIBLE
        binding.layoutDiscoveredDevices.visibility = View.VISIBLE
        
        // Start custom radar/pulse micro-animations
        binding.rippleOuter.alpha = 0.5f
        binding.rippleInner.alpha = 0.5f
        triggerRippleAnimation()

        // Trigger client discovery
        glassClient.startScan(this)
    }

    private fun triggerRippleAnimation() {
        if (!isScanning) return
        
        binding.rippleOuter.scaleX = 1f
        binding.rippleOuter.scaleY = 1f
        binding.rippleOuter.alpha = 0.6f
        binding.rippleOuter.animate()
            .scaleX(1.8f)
            .scaleY(1.8f)
            .alpha(0f)
            .setDuration(1500)
            .withEndAction {
                if (isScanning) triggerRippleAnimation()
            }
            .start()

        binding.rippleInner.scaleX = 1f
        binding.rippleInner.scaleY = 1f
        binding.rippleInner.alpha = 0.6f
        binding.rippleInner.animate()
            .scaleX(1.4f)
            .scaleY(1.4f)
            .alpha(0f)
            .setDuration(1500)
            .start()
    }

    private fun stopScanningAnimations() {
        isScanning = false
        binding.rippleOuter.animate().cancel()
        binding.rippleInner.animate().cancel()
        binding.rippleOuter.alpha = 0f
        binding.rippleInner.alpha = 0f
        binding.scanProgressBar.visibility = View.GONE
        binding.btnActionConnect.text = "CONNECT"
        binding.btnActionConnect.isEnabled = true
    }

    private fun connectToDevice(device: GlassDevice) {
        stopScanningAnimations()
        glassClient.stopScan(this)
        glassClient.connect(this, device)
    }

    private fun observeClientState() {
        // Scanned devices list
        glassClient.scannedDevices.observe(this) { devices ->
            deviceAdapter.updateDevices(devices)
            if (devices.isNotEmpty() && isScanning) {
                binding.tvScanStatus.text = "Found ${devices.size} glasses nearby"
            }
        }

        // Connected Device Name
        glassClient.connectedDeviceName.observe(this) { deviceName ->
            binding.tvConnectedDeviceTitle.text = deviceName ?: "HeyCyan Glasses"
        }

        // Connection State transitions
        glassClient.connectionState.observe(this) { state ->
            when (state) {
                ConnectionState.DISCONNECTED -> {
                    binding.layoutCaptureLoading.visibility = View.GONE
                    binding.layoutConnected.visibility = View.GONE
                    binding.layoutFullscreenPhoto.visibility = View.GONE
                    binding.layoutLaunch.visibility = View.VISIBLE
                    binding.tvScanStatus.text = "Connect disconnected"
                    stopScanningAnimations()
                }
                ConnectionState.CONNECTING -> {
                    binding.layoutLaunch.visibility = View.GONE
                    binding.layoutConnected.visibility = View.GONE
                    binding.layoutFullscreenPhoto.visibility = View.GONE
                    
                    binding.tvCaptureLoadingText.text = "Establishing connection to smart glasses..."
                    binding.layoutCaptureLoading.visibility = View.VISIBLE
                }
                ConnectionState.CONNECTED -> {
                    binding.layoutCaptureLoading.visibility = View.GONE
                    binding.layoutLaunch.visibility = View.GONE
                    binding.layoutFullscreenPhoto.visibility = View.GONE
                    
                    binding.layoutConnected.visibility = View.VISIBLE
                    Toast.makeText(this, "Successfully connected to glasses", Toast.LENGTH_SHORT).show()
                }
                null -> {}
            }
        }
    }

    private fun capturePhoto() {
        binding.tvCaptureLoadingText.text = "Triggering glasses camera lens..."
        binding.layoutCaptureLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Trigger taking photo in background thread
            val photoFile = glassClient.takePicture(this@MainActivity)
            
            if (photoFile != null && photoFile.exists()) {
                binding.tvCaptureLoadingText.text = "Downloading captured image..."
                // Simple delay for realistic experience
                kotlinx.coroutines.delay(800)
                
                // Show photo fullscreen
                loadCapturedPhoto(photoFile)
            } else {
                binding.layoutCaptureLoading.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Failed to capture photo. Device disconnected.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCapturedPhoto(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        binding.ivCapturedPhoto.setImageBitmap(bitmap)
        
        binding.layoutCaptureLoading.visibility = View.GONE
        binding.layoutConnected.visibility = View.GONE
        binding.layoutFullscreenPhoto.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanningAnimations()
    }
}