package bleproj.bluetooth;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Method;



public class BluetoothLeService extends Service {

    private final static String tag = BluetoothLeService.class.getSimpleName();


    public final static String ACTION_GATT_CONNECTED =
            "pages.ipc.com.ipcpages.bluetooth.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "pages.ipc.com.ipcpages.bluetooth.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "pages.ipc.com.ipcpages.bluetooth.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "pages.ipc.com.ipcpages.bluetooth.ACTION_DATA_AVAILABLE";
    public final static String ACTION_UART_DATA_AVAILABLE =
            "pages.ipc.com.ipcpages.bluetooth.ACTION_UART_DATA_AVAILABLE";
    public final static String ACTION_CHAR_WRITTEN =
            "pages.ipc.com.ipcpages.bluetooth.ACTION_CHAR_WRITTEN";
    public final static String EXTRA_DATA =
            "pages.ipc.com.ipcpages.bluetooth.EXTRA_DATA";
    public final static String ACTION_GATT_DISCONNECTED_ERROR =
            "pages.ipc.com.ipcpages.bluetooth.ACTION_GATT_DISCONNECTED_ERROR";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private final IBinder mBinder = new LocalBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);

                Log.d( tag, "Connected to GATT server: " + status);
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                Log.d( tag,"Disconnected from GATT server: " + status);

                refreshDeviceCache(gatt);
            }
        }
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {

        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d( tag, "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
            else {
                // todo: recover from error
            }
        }
        private void broadcastUpdate(final String action) {
            final Intent intent = new Intent(action);
            sendBroadcast(intent);
        }
        private void broadcastUpdate(final String action,
                                     final BluetoothGattCharacteristic characteristic) {
            final Intent intent = new Intent(action);
            Bundle mBundle = new Bundle();
            // Putting the byte value read for GATT Db
            mBundle.putByteArray(IntentRequest.DataKey.EXTRA_BYTE_VALUE,
                    characteristic.getValue());
            mBundle.putString(IntentRequest.DataKey.EXTRA_BYTE_UUID_VALUE,
                    characteristic.getUuid().toString());
            mBundle.putInt(IntentRequest.DataKey.EXTRA_BYTE_INSTANCE_VALUE,
                    characteristic.getInstanceId());
            mBundle.putString(IntentRequest.DataKey.EXTRA_BYTE_SERVICE_UUID_VALUE,
                    characteristic.getService().getUuid().toString());
            mBundle.putInt(IntentRequest.DataKey.EXTRA_BYTE_SERVICE_INSTANCE_VALUE,
                    characteristic.getService().getInstanceId());
            if(characteristic.getUuid().toString().equalsIgnoreCase(UUID.FirmwareVersion)) {
                Intent FirmwareIntent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
                FirmwareIntent.putExtra(IntentRequest.DataKey.FIRMWARE_VERSION, characteristic.getStringValue(0));
                //mLastIntentSent = FirmwareIntent;
                sendBroadcast(FirmwareIntent);
            }
            else if(characteristic.getUuid().toString().equalsIgnoreCase(UUID.HardwareRevision)) {
                Intent HardwareIntent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
                HardwareIntent.putExtra(IntentRequest.DataKey.HARDWARE_REVISION, characteristic.getStringValue(0));
                //mLastIntentSent = FirmwareIntent;
                sendBroadcast(HardwareIntent);
            }
            else if(characteristic.getUuid().toString().equalsIgnoreCase(UUID.ManufacturerName)) {
                Intent ManufacturerIntent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
                ManufacturerIntent.putExtra(IntentRequest.DataKey.MANUFACTURER_NAME, characteristic.getStringValue(0));
                //mLastIntentSent = FirmwareIntent;
                sendBroadcast(ManufacturerIntent);
            }
            else if (characteristic.getUuid().toString().equalsIgnoreCase(UUID.Tx)) {
                Log.d(tag,"Uart data available Tx");
                Intent mIntentUART = new Intent(BluetoothLeService.ACTION_UART_DATA_AVAILABLE);
                mIntentUART.putExtra(IntentRequest.DataKey.UART_DATA,characteristic.getValue());
                Log.d(tag,"Before sending Uart broadcast Tx");
                //mIntentUART.putExtras(mBundle);
                //mLastIntentSent = mIntentOTA;
                sendBroadcast(mIntentUART);
            }
            else {
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                }


                intent.putExtras(mBundle);
                sendBroadcast(intent);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d( tag, "Characteristic read: "
                    + characteristic.getUuid().toString() + " - status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }

        }


        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(tag,"characteristic changed "+characteristic.getValue());
            broadcastUpdate(ACTION_UART_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d( tag, "Char: " + characteristic.getUuid().toString()
                    + " -- Write status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }



        @Override
        public void onDescriptorWrite(
                BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            Log.d( tag,"Descriptor written status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }
    };

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {

            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.d( tag,
                    "BluetoothAdapter not initialized or unspecified address on connect.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.d( tag, "Device not found.  Unable to connect.");
            return false;
        }

        // connectToDevice(device);

        return true;
    }
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param device The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(BluetoothDevice device) {
        if (mBluetoothAdapter == null || device == null) {
            Log.d( tag, "BluetoothAdapter not initialized or no device on connect.");
            return false;
        }

        connectToDevice(device);
        return true;
    }

    private void connectToDevice(BluetoothDevice device) {
//

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = null;
        if (Build.VERSION.SDK_INT >= 23) {
            mBluetoothGatt = device.connectGatt(
                    this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        }
        else {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }

        Log.d( tag, "Trying to create a new connection.");
    }

    public boolean refreshDeviceCache(BluetoothGatt gatt) {
        Log.d( tag, "Refreshing BLE cache of device.");
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh");
            if (localMethod != null) {
                return (Boolean) localMethod.invoke(localBluetoothGatt);
            }
        }
        catch (Exception localException) {
            Log.d( tag, "An exception occurred while refreshing device");
        }
        return false;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d( tag, "BluetoothAdapter not initialized on disconnect");
            return;
        }
        Log.d( tag, "disconnecting..");
        mBluetoothGatt.disconnect();
    }
    /**
     * Return the connection state.
     *
     * @return BluetoothProfile states
     */
    public int getConnectionState() {
        if (mBluetoothManager == null || mBluetoothGatt == null) return BluetoothGatt.STATE_DISCONNECTED;

        return mBluetoothManager.getConnectionState(mBluetoothGatt.getDevice(), BluetoothProfile.GATT);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     * @return True if the request was sent successfully.
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(tag,
                    "BluetoothAdapter not initialized on readCharacteristic");
            return false;
        }
        return mBluetoothGatt.readCharacteristic(characteristic);
    }
    public void exchangeGattMtu(int mtu) {

        boolean status = false;

        mBluetoothGatt.requestMtu(mtu);



    }


    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}. The write result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to write to.
     * @return True if the request was sent successfully.
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(tag, "BluetoothAdapter not initialized on write char");
            return false;
        }
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }


    /**
     * Method to rediscover the services of a device. Refresh and disconnect.
     */
    public void rediscoverServices() {
        if (mBluetoothGatt == null) return;

        refreshDeviceCache(mBluetoothGatt);
        disconnect();
    }

    /**
     * Return the service requested.
     *
     * @param serviceUUID UUID of the service requested.
     * @return Null on failure
     */
    public BluetoothGattService getService(java.util.UUID serviceUUID) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return null;
        }
        return mBluetoothGatt.getService(serviceUUID);
    }

    /**
     * Method to discover the services of a device.
     */
    public void discoverServices() {
        if (mBluetoothGatt == null) return;

        mBluetoothGatt.discoverServices();
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        Log.d(tag, "Set the char notif: " +
                mBluetoothGatt.setCharacteristicNotification(characteristic, enabled));

        if (characteristic.getDescriptor(java.util.UUID
                .fromString(UUID.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                        java.util.UUID.fromString(UUID.CLIENT_CHARACTERISTIC_CONFIG));
                Log.d(tag, "Setting value: " +
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
                Log.d(tag, "Writing request: "
                        + mBluetoothGatt.writeDescriptor(descriptor));
            } else {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                        java.util.UUID.fromString(UUID.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        }
        else {
            Log.d(tag, "Descriptor not found");
        }
    }
}
