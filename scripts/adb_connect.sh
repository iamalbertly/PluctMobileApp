#!/system/bin/sh

# Function to get WiFi IP
get_wifi_ip() {
    ip addr show wlan0 | grep -w inet | awk '{print $2}' | cut -d/ -f1
}

# Function to check ADB status
check_adb_status() {
    if getprop init.svc.adbd | grep -q "running"; then
        echo "ADB service is running"
        return 0
    else
        echo "ADB service is not running"
        return 1
    fi
}

# Enable ADB over TCP/IP
enable_adb_wireless() {
    # Set ADB port
    setprop service.adb.tcp.port 5555
    
    # Stop and start adbd to apply changes
    stop adbd
    start adbd
    sleep 2
    
    # Get current IP
    WIFI_IP=$(get_wifi_ip)
    
    # Save IP to file for the HTML interface
    echo "$WIFI_IP" > /storage/emulated/0/Download/adb_ip.txt
    
    # Output status
    if check_adb_status; then
        echo "ADB Wireless enabled on $WIFI_IP:5555"
        echo "Status: READY" > /storage/emulated/0/Download/adb_status.txt
    else
        echo "Failed to enable ADB Wireless"
        echo "Status: ERROR" > /storage/emulated/0/Download/adb_status.txt
    fi
}

# Main execution
enable_adb_wireless