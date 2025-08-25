# Wireless ADB Setup Guide

This guide explains how to set up wireless ADB debugging for Android devices.

## Prerequisites
- Android device with USB debugging enabled
- USB cable (for initial setup)
- ADB (Android Debug Bridge) installed on your computer
- Both device and computer connected to the same network

## Manual Setup Process

1. Connect your Android device via USB cable
2. Ensure the device is recognized by ADB:
   ```
   adb devices
   ```

3. Enable TCP/IP mode on port 5555:
   ```
   adb tcpip 5555
   ```

4. Get your device's IP address:
   - Method 1 (via ADB):
     ```
     adb shell ip addr show wlan0
     ```
   - Method 2: On your Android device, go to Settings > About Phone > Status > IP address

5. Connect to your device wirelessly:
   ```
   adb connect <DEVICE_IP>:5555
   ```

6. Verify the connection:
   ```
   adb devices
   ```
   You should see your device listed with its IP address

7. You can now unplug the USB cable

## Automated Setup
We've provided a PowerShell script (`wireless_adb_connect.ps1`) that automates this process. Simply run:
```
.\wireless_adb_connect.ps1
```

## Troubleshooting
1. If the connection fails:
   - Ensure both devices are on the same network
   - Check if the IP address is correct
   - Try reconnecting via USB and restart the process
   - Restart ADB server:
     ```
     adb kill-server
     adb start-server
     ```

2. If the device disconnects:
   - Run the PowerShell script again
   - Or manually reconnect using `adb connect <DEVICE_IP>:5555`
