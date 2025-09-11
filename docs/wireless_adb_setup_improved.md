# Improved Wireless ADB Setup Guide

This guide provides multiple methods to set up wireless ADB debugging for Android devices, with special focus on devices that may have limitations with terminal access or permissions.

## Method 1: Using Android's Built-in Wireless Debugging (Recommended)

### On Your Android Device:
1. Open **Settings** → **About Phone** → Tap **Build Number** 7 times to enable Developer Options
2. Go back to **Settings** → **System** → **Developer Options**
3. Enable **Wireless Debugging**
4. Tap on **Wireless Debugging** to open settings
5. Tap **Pair device with pairing code**
6. Note the **IP address**, **port**, and **pairing code** displayed

### On Your Computer:
1. Open PowerShell and navigate to the scripts directory
2. Run the connection script with pairing information:
   ```
   .\Robust-ADB-Connect.ps1 -DeviceIP "192.168.1.x" -PairingPort "xxxxx" -PairingCode "xxxxxx"
   ```
3. Alternatively, run the script without parameters and select option 2 from the menu:
   ```
   .\Robust-ADB-Connect.ps1
   ```

## Method 2: Using the Simple HTML Interface

1. Copy the `ADB_Connect_Simple.html` file to your Android device
2. Open the file in any browser on your device
3. Click the "Open Developer Settings" button
4. Follow the on-screen instructions to enable Wireless Debugging
5. Use the PC script to connect using the pairing information

## Method 3: USB Connection Method

If you prefer to start with a USB connection:

1. Connect your device via USB
2. Run the script and select option 1:
   ```
   .\Robust-ADB-Connect.ps1
   ```
3. The script will enable wireless ADB and show the device's IP
4. You can now disconnect the USB cable

## Troubleshooting

### Device Shows as Offline
If your device shows as "offline" in ADB:

1. Run the script and select option 6 to restart the ADB server
2. Try connecting again with option 3
3. If still offline, try the auto-reconnect feature with option 5

### Can't Find Device on Network
If you're having trouble finding your device:

1. Make sure both devices are on the same network
2. Use option 4 to scan the network for Android devices
3. Check your device's IP address in Settings → About Phone → Status

### Permission Issues
If you encounter permission issues:

1. Always use Method 1 (Android's built-in Wireless Debugging)
2. This method doesn't require any special permissions or apps

## Automatic Reconnection

To keep your device connected automatically:

1. Run the script with the auto-reconnect flag:
   ```
   .\Robust-ADB-Connect.ps1 -AutoReconnect
   ```
2. This will monitor the connection and reconnect if it drops

## Network Scanning

To find Android devices on your network:

1. Run the script with the scan flag:
   ```
   .\Robust-ADB-Connect.ps1 -ScanNetwork
   ```
2. The script will scan your local network and attempt to connect to any Android devices it finds

