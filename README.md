# Bluetooth Mic Monitor (Android)

A native Kotlin Android app that routes live phone-microphone PCM audio to Android's selected media output, including a paired Bluetooth speaker.

## Build and install

1. Install **Android Studio** with Android SDK Platform 35 and JDK 17 (Android Studio's bundled JDK works).
2. Open the `BluetoothMicMonitor` folder as a project. Let Android Studio download Gradle and dependencies. Internet access is required for the initial sync.
3. Connect an Android 10+ phone with USB debugging enabled, select it in Android Studio, then click **Run**.
4. Alternatively, use **Build > Build Bundle(s) / APK(s) > Build APK(s)** and install the debug APK on your phone.
5. Pair the Bluetooth speaker in Android Settings. Make sure it is the selected **media audio** output (not merely paired).
6. Lower the speaker volume, keep it away from the phone microphone, open the app, grant microphone permission, and tap **Start monitoring**.
7. Adjust microphone gain carefully. Use **Stop monitoring** or the notification's **Stop** action to end capture.

## Important behavior

- This app **does not force a specific Bluetooth device**. It sends audio to Android's selected media output. If audio plays through the phone, choose the Bluetooth speaker in Android's media output picker or Bluetooth settings.
- Bluetooth A2DP latency is controlled largely by the speaker, codec, and Android audio stack. The 10 ms app processing chunks do not imply 10 ms end-to-end latency.
- If Bluetooth headphones with a microphone are connected, Android's route and recording source may vary by device. This app requests the phone microphone using `MediaRecorder.AudioSource.MIC`, but some manufacturers may still route capture differently.
- Android 14+ requires starting the microphone foreground service while the app is visible and microphone permission is granted. The app uses a microphone foreground service and a persistent notification.
- Android may revoke or interrupt capture for phone calls, privacy controls, or system resource management.
- The app does not save or transmit recordings to the internet.
- A Bluetooth speaker placed near the phone can create severe acoustic feedback. Start with low volume.

## Source files

- `app/src/main/java/com/example/bluetoothmicmonitor/MainActivity.kt`: UI and permission flow
- `app/src/main/java/com/example/bluetoothmicmonitor/MonitorService.kt`: foreground service and real-time audio loop
- `app/src/main/AndroidManifest.xml`: permissions and service declaration

## Known limitations / next improvements

- No automatic reconnect or route-change detection.
- No echo cancellation, limiter, or noise suppression.
- Mute and gain are not persisted across app restarts.
- Status is simple and may not reflect all audio hardware errors.
- No instrumented tests included. Verify on a physical Android device and speaker.
- This source archive does not include the Gradle wrapper or a compiled APK. Android Studio can sync/build using its configured Gradle environment. If Gradle is not configured, generate a wrapper using Gradle 8.7 (`gradle wrapper --gradle-version 8.7`).
