package bleproj.bluetooth;


import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;

import java.util.List;


public class BluetoothScanManager implements ScanManager, BluetoothAdapter.LeScanCallback {
    public static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final String tag = "bluetoothServiceManager";
    private static final int SCAN_PERIOD = 20000;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private Activity mActivity;
    private Boolean mScanning = false;

    //Listener
    private DeviceScanListener deviceFoundListener;

    /**
     * Scan callback for SDK version < 21. Triggered on devices found.
     */
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (deviceFoundListener != null) deviceFoundListener.onDeviceFound(device);
    }

    /**
     * Scan callback for SDK version >= 21. Triggered on devices found.
     * note: The callback is implemented when called to avoid some compatibility issues.
     */
    private ScanCallback scanCallback;

    /**
     * Constructor initializing the class.
     *
     * @param activity Activity to be used to access the bluetooth manager.
     */
    public BluetoothScanManager(Activity activity) {
        this.mActivity = activity;
        mBluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    /**
     * Set the listener for a new bluetooth device found.
     *
     * @param deviceScanListener Listener
     */
    public void setOnNewDeviceFoundListener(DeviceScanListener deviceScanListener) {
        this.deviceFoundListener = deviceScanListener;
    }

    /**
     * Return a bluetooth device if it is connected. Return null otherwise.
     *
     * @param address Bluetooth address
     * @return BluetoothDevice.
     */
    public BluetoothDevice getConnectedDevice(String address) {
        List<BluetoothDevice> btList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

        for (BluetoothDevice device: btList) {
            if (device.getAddress().equals(address))
                return device;
        }

        return null;
    }

    /**
     * Return whether the Connection Manager is scanning.
     *
     * @return True if scanning.
     */
    public boolean isScanning() {
        return mScanning;
    }

    /**
     * Start scanning for new bluetooth devices. Will stop scanning after SCAN_PERIOD is elapsed.
     */
    public void startScanning() {
        if (mScanning) stopScanning();

        // Start bluetooth
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
            return;
        }

        // Set timer.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
            }
        }, SCAN_PERIOD);

        // Add already bonded devices
        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            if (deviceFoundListener != null) deviceFoundListener.onDeviceFound(device);
        }

        // The scan methods were changed after SDK 21..
        if (Build.VERSION.SDK_INT >= 21) {
            scanCallback = new ScanCallback() {
                @Override
                @TargetApi(21)
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (deviceFoundListener != null) deviceFoundListener.onDeviceFound(result.getDevice());
                }

                @Override
                @TargetApi(21)
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    for (ScanResult result : results) {
                        if (deviceFoundListener != null) deviceFoundListener.onDeviceFound(result.getDevice());
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                }
            };
            mBluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
        } else {
            mBluetoothAdapter.startLeScan(this);
        }

        mScanning = true;
    }

    /**
     * Stop the scan for new Bluetooth devices in function of the SDK version.
     */
    public void stopScanning() {
        if (!mScanning) return;

        // Again, the scan methods were changed after SDK 21
        if (Build.VERSION.SDK_INT >= 21) {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(this);
        }

        mScanning = false;
    }
}