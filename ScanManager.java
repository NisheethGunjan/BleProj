package bleproj.bluetooth;


import android.bluetooth.BluetoothDevice;

/**
 * Interface for the application's external scan.
 *
 */
public interface ScanManager {

    /**
     * Listener for scan events.
     */
    interface DeviceScanListener {
        /**
         * Called when a new device is found.
         *
         * @param bluetoothDevice Device found.
         */
        void onDeviceFound(BluetoothDevice bluetoothDevice);
    }

    /**
     * Return whether a device is connected.
     *
     * @param address Bluetooth address
     */
    BluetoothDevice getConnectedDevice(String address);

    /**
     * Return the state of the scan.
     *
     * @return True if scanning.
     */
    boolean isScanning();

    /**
     * Set the listener for a new bluetooth device found.
     *
     * @param DeviceScanListener Listener
     */
    void setOnNewDeviceFoundListener(DeviceScanListener DeviceScanListener);

    /**
     * Start scanning for new devices.
     */
    void startScanning();

    /**
     * Stop the scanning process.
     */
    void stopScanning();
}
