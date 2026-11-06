# Build the APK using GitHub Actions (no Android Studio)

1. Sign in at https://github.com and create a **private** repository named `BluetoothMicMonitor`. Do not add a README or license while creating it.
2. Extract this ZIP. Upload **the contents of the BluetoothMicMonitor folder** to the root of your repository, including the hidden `.github/workflows/build-apk.yml` file. GitHub's web uploader can upload folders, but verify that `.github/workflows/build-apk.yml` is visible in the repository afterward. Alternatively use Git from a computer to push all project files.
3. Open the repository's **Actions** tab. If prompted, enable workflows. Select **Build Android APK**, click **Run workflow**, and run it on the main branch. Pushing to main also triggers the workflow automatically.
4. Wait for a green checkmark. Open that workflow run, scroll to **Artifacts**, and download **BluetoothMicMonitor-debug-apk**.
5. Extract the downloaded artifact ZIP to get `app-debug.apk`. Transfer the APK to your Android phone, open it, and allow installation from that source when Android asks.
6. Pair your Bluetooth speaker and set it as the media output. Open the app, allow microphone access, and tap **Start monitoring**. Start at low volume to avoid feedback.

## Troubleshooting
- No workflow appears: verify `.github/workflows/build-apk.yml` was uploaded in the correct directory on the default branch.
- Red X: open the failed step in Actions and read its log. The source has not been compiled or device-tested in this environment.
- Artifact not shown: build must complete successfully; check the upload step.
- No Bluetooth output: connect the speaker and select it as the Android media-output device before starting the app.
- Delayed sound: Bluetooth A2DP adds latency that cannot be removed by the app.

This is a **debug-signed** APK intended for personal testing, not a Play Store release. GitHub-hosted builds are subject to GitHub's availability and usage policies.
