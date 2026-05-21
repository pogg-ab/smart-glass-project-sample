# HeyCyan Smart Glasses Camera Companion App
### Android Developer Take-Home Test — ChipChip Recruitment Pipeline

A premium, production-grade Android (Kotlin) application that integrates with the official **HeyCyan Smart Glasses SDK** to scan for, connect to, and capture photos through the smart glasses. 

Since physical hardware is not provided, this application features a **hybrid orchestration layer** that compiles against the real HeyCyan SDK AAR, but **automatically falls back to a mock simulation** if no real glasses are found within 4 seconds. This ensures the app can be verified end-to-end on any standard Android phone.

---

## 📱 App Highlights & User Flow

1. **Launch Screen ( Premium Obsidian-Indigo Gradient )**: A stunning dark dashboard backdrop featuring a **ChipChip Recruitment Evaluation Badge** and a central **"Connect"** button with custom purple-cyan gradients and dynamic pulsing radar/ripple micro-animations.
2. **Auto-Discovery Scan**: Tap "Connect" to request Bluetooth permissions and begin scanning. If no physical glasses are found within **4 seconds**, the list seamlessly populates with mock devices (`HeyCyan Air (Demo)`, `HeyCyan Neo (Demo)`, etc.).
3. **Glassmorphic List View**: High-fidelity, transparent card views displaying device name, BLE MAC address, and real-time signal strength (RSSI) telemetry.
4. **Interactive HUD Viewfinder**: Once connected, you transition to an immersive smart glasses Viewfinder screen containing target crosshairs, telemetry badges (showing active simulated battery status), and active **ChipChip HUD Eyewear Engine** overlays.
5. **POV Capture Display**: Tap the circular camera shutter button to trigger a 2-second capture process (simulates network download). The high-fidelity first-person point-of-view (POV) photo is rendered in full-screen with a slide transition, back button functionality, and a translucent evaluation copy watermark footer.

---

## 🏛️ Architecture & Code Separation

To guarantee clean, testable code, all SDK-specific classes (`BleScannerHelper`, `BleOperateManager`, `LargeDataHandler`) are entirely isolated behind a custom client contract. **Zero SDK calls are made inside UI activities or widgets.**

- **`GlassClient` (Contract)**: Interface defining scanner, connection, and capture operations using Kotlin Coroutines and Architecture Components (`LiveData`).
- **`RealGlassClient`**: Wraps the precompiled `glasses_sdk_20250723_v01.aar` libraries, subscribing to BLE status events using EventBus.
- **`MockGlassClient`**: Fully implements the contract in memory, simulating scan delays, a 1.5s connection delay, and loading a high-resolution HUD POV asset.
- **`GlassClientProvider` (Orchestration)**: Serves as a transparent proxy. It launches real BLE scanner first, and schedules a 4-second timeout. If the real BLE scanner returns no physical glasses within this timeframe, it automatically fallbacks to mock mode.

---

## 📂 Direct APK Download

The compiled, signed debug APK is included directly within the repository for immediate side-loading and review:

👉 **[Download ChipChip-SmartGlass-Companion.apk](file:///c:/Users/hp/Smart%20glass%20project/apk/ChipChip-SmartGlass-Companion.apk)** *(8.09 MB)*

---

## 🛠️ Build and Setup Instructions

### Environment Prerequisites
- **JDK Requirement**: **JDK 17 or JDK 21**. (Note: The default legacy JDK 8 environment may throw SSL handshake/PKIX path validation errors when communicating with Gradle/Maven repositories. It is highly recommended to compile utilizing the modern OpenJDK bundle packaged in Android Studio).
- **Target Android Version**: API 35 (Android 15)
- **Minimum Android Version**: API 24 (Android 7.0 Nougat)

### Building via Terminal
To build the debug APK using the local JetBrains Runtime JDK bundled with your Android Studio, execute the following PowerShell command in the project root:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "C:\Program Files\Android\Android Studio\jbr\bin;" + $env:PATH
.\gradlew assembleDebug
```

Upon successful compilation, the built binary will be written to `app/build/outputs/apk/debug/ChipChip-SmartGlass-Companion.apk`.

---

## 🛡️ Required Permissions

The app requests permissions dynamically at runtime using `XXPermissions`:
- **`android.permission.BLUETOOTH_SCAN`**: Discover nearby Bluetooth LE smart glasses.
- **`android.permission.BLUETOOTH_CONNECT`**: Connect and pair with smart glasses.
- **`android.permission.ACCESS_FINE_LOCATION`**: Required by Android to run BLE discovery.

---

## 🎯 Assumptions & Mock Configurations
1. **Mock Image**: To mimic looking through smart glasses, we generated a gorgeous high-fidelity first-person point-of-view camera overlay image showing HUD telemetry and neon street sunset, which is bundled inside `app/src/main/assets/sample_glass_photo.png`.
2. **SDK Compilation**: The project includes the official precompiled `glasses_sdk_20250723_v01.aar` inside the `app/libs/` directory. All class names and command structures match the original specifications.
3. **P2P Wi-Fi**: The original sample's unused and broken decompiled Java libraries (`ui/wifi`) were safely pruned from the source directories, guaranteeing a lightweight compile chain and clean architecture.
