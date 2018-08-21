package bleproj.bluetooth;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.zxing.common.StringUtils;

import java.util.*;

import static bleproj.bluetooth.UUID.UARTService;


public class BluetoothHelper {

    private final static String tag = "BluetoothHelper";
    private static String substring="";
    private final static int RECONNECTION_DELAY = 5000;
    public interface ControlHelperListener {
        /**
         * Called when the available state change.
         *
         * @param available True if available.
         */
        void onAvailableStateChanged(boolean available);

        /**
         * Called when a bluetooth error occurs.
         */
        void onError();

        /**
         * Decode Report
         *
         * @param data
         */
        void DecodeReport(String data,int type);

    }
    private ControlHelperListener mListener;
    private int controlState = UNAVAILABLE_STATE;
    public final static int AVAILABLE_STATE = 0;
    public final static int UNAVAILABLE_STATE = 1;

    public final static int DISCONNECTED = 0;
    public final static int CONNECTED = 1;
    public final static int DISCONNECTING = 2;
    public final static int CONNECTING = 3;
    private int connectionState = DISCONNECTED;
    private static boolean status=false;
    private Context mContext;
    private BluetoothDevice mBtDevice;
    private BluetoothLeService mBluetoothLeService;
    private Handler delayHandler;
    private static String firstString="";
    private static String secString="";
    private static String comString="";
    private static String lastString="";
    private static String incompString="";
    private static int halfStateIndex=0;
    private static int counter=0;
    private static boolean incompleteStr=false;
    private static boolean halfState=false;
    private String complete_string;
    private ArrayList<BluetoothCommand> readingQueue = new ArrayList<>();

    // Manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();


            if (mBluetoothLeService.getConnectionState() == BluetoothProfile.STATE_CONNECTED) {
                // Assume services were discovered..


                mBluetoothLeService.discoverServices();
                return;
            }
            if (!mBluetoothLeService.initialize()) {
              Log.d(tag, "Unable to initialize Bluetooth");
                mListener.onError();
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.d(tag, "Connecting to device after service connected.");

            mBluetoothLeService.connect(mBtDevice);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            delayHandler.removeCallbacksAndMessages(null);
            if(handler!=null)
                handler.removeCallbacksAndMessages(null);



            /**
             * Data Available
             */
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                Log.d(tag,"data available");
               String firmwareavailable = intent.getStringExtra(IntentRequest.DataKey.FIRMWARE_VERSION);
                Log.d(tag,"firmware data available"+firmwareavailable);
                String hardwareavailable = intent.getStringExtra(IntentRequest.DataKey.HARDWARE_REVISION);
                Log.d(tag,"hardware data available"+hardwareavailable);
                String manufactureravailable = intent.getStringExtra(IntentRequest.DataKey.MANUFACTURER_NAME);
                Log.d(tag,"manufacturer data available"+manufactureravailable);
                //String UARTavailable = intent.getStringExtra(IntentRequest.DataKey.UART_DATA);
                //Log.d(tag,"UART data available"+UARTavailable);
                if(firmwareavailable != null){
                    mListener.DecodeReport(firmwareavailable,2);
                }
                if(hardwareavailable != null){
                    mListener.DecodeReport(hardwareavailable,1);
                }
                if(manufactureravailable != null){
                    mListener.DecodeReport(manufactureravailable,0);
                }
               // if(UARTavailable != null){
                   // mListener.DecodeReport(UARTavailable,3);
                //}

                if (!readingQueue.isEmpty()) {
                    BluetoothCommand command = readingQueue.remove(0);
                    readCharacteristic(
                            command.serviceUUID, command.characteristicUUID);
                    return;
                }


            }
            /**
             * Characteristic written
             */
            else if (BluetoothLeService.ACTION_CHAR_WRITTEN.equals(action)) {
                Log.d(tag, "Characteristic written..");

            }
            else if (BluetoothLeService.ACTION_UART_DATA_AVAILABLE.equals(action)) {
                Log.d(tag, "before parsing uart intent");
                parseUARTIntent(intent);
            }

            /**
             * Device connected
             */
            else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connectionState = CONNECTED;

                if (mBluetoothLeService == null) {
                    Log.d(tag, "BluetoothLEService not connected.");
                    return;
                }
                mBluetoothLeService.discoverServices();

            }
            /**
             * Device disconnected
             */
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                int oldConnectionState = connectionState;
                connectionState = DISCONNECTED;
                controlState = UNAVAILABLE_STATE;
                if (mListener != null) mListener.onAvailableStateChanged(false);

                Log.d(tag, "BLE disconnected.");

                if (oldConnectionState != DISCONNECTING) {
                    delayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(tag,"Reconnecting..");
                            if(mBluetoothLeService!=null)
                                mBluetoothLeService.connect(mBtDevice);
                        }
                    }, RECONNECTION_DELAY);
                }
            }
            /**
             * Service discovered
             */
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                controlState = AVAILABLE_STATE;
                Log.d(tag,"Service Discovered");
                mBluetoothLeService.exchangeGattMtu(512);
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mListener != null) mListener.onAvailableStateChanged(true);
                    }
                }, 200);






            }
            /**
             * Device disconnected on error.
             */
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED_ERROR.equals(action)) {
                int oldConnectionState = connectionState;
                connectionState = DISCONNECTED;
                controlState = UNAVAILABLE_STATE;
                if (mListener != null) mListener.onAvailableStateChanged(false);

                Log.d(tag, "BLE disconnected with error.");

                if (oldConnectionState != DISCONNECTING) {
                    delayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            Log.d(tag, "Reconnecting..");
                            if(mBluetoothLeService!=null && mBtDevice!=null)
                                mBluetoothLeService.connect(mBtDevice);
                        }
                    }, RECONNECTION_DELAY);
                }
            }


        }


    };

    /**
     * Set the listener of the ControlHelperListener.
     *
     * @param listener Listener.
     */
    public void setListener(ControlHelperListener listener) {
        mListener = listener;
    }
    /**
     * Construct the object and bind to the BLE service.
     *
     * @param context Activity's context
     * @param btDevice Bluetooth device to connect to.
     */
    public  BluetoothHelper(Context context, BluetoothDevice btDevice) {


        mContext = context;
        mBtDevice = btDevice;
        Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
        mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Register the broadcast receiver and send a request to connect to the device.
     *
     * Needs to be called onResume().
     */
    public void initialize() {
        // Set the state to unavailable
        controlState = UNAVAILABLE_STATE;
        if (mListener != null) mListener.onAvailableStateChanged(false);

        // Create intent filter
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_UART_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_CHAR_WRITTEN);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED_ERROR);

        // Register the broadcast receiver
        mContext.registerReceiver(mGattUpdateReceiver, intentFilter);

        // Connect to the device.
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mBtDevice);
            Log.d(tag, "Connect request result: " + result);

            if(result)
                controlState=AVAILABLE_STATE;
        }

        delayHandler = new Handler(mContext.getMainLooper());
    }


    public  void ReadFirmwareVersion(){
        readCharacteristic(UUID.HardwareService, UUID.FirmwareVersion);
    }
    public  void ReadHardwareRevision(){
        readCharacteristic(UUID.HardwareService, UUID.HardwareRevision);
    }
    public  void ReadManufacturerName(){
        readCharacteristic(UUID.HardwareService, UUID.ManufacturerName);
    }
    public  void readUARTData(){

        boolean success=readCharacteristic(UARTService, UUID.Tx);
        Log.d(tag,"reading UART: "+success);
    }
    public static int counters=0;
    public static boolean startCounter=true;
    private void parseUARTIntent(Intent intent) {
        byte[] responseArray = intent
                .getByteArrayExtra(IntentRequest.DataKey.UART_DATA);
        //byte[] responseArray = intent
                //.getByteArrayExtra(IntentRequest.DataKey.UART_DATA);

        String hexValue = ByteArrayToHex(responseArray);




        String result = hexValue.trim().replace(" ", "");
        int ind1,ind2,len,str_len;
        String str1,str2,str3;

if(halfState)
{
    String newres="";
    if(halfStateIndex==0) {
        newres = "AA2B".concat(result);

    }
    else if(halfStateIndex==1) {
        newres = "AA2".concat(result);

    }
    else if(halfStateIndex==2) {
        newres = "AA".concat(result);

    }
    else if(halfStateIndex==3) {
        newres = "A".concat(result);

    }
    result=newres;
    halfState=false;
}
            if(result.toUpperCase().contains("AA2B")) {

                ind1 = result.indexOf("AA2B");
                if(ind1!=0) {
                    str1 = result.substring(0, ind1);
                    if(incompleteStr)
                    {
                        String compString=incompString.concat(str1);
                        if(compString.length()>32)
                        processComplete(compString);

                    }

                }
                str2=result.substring(ind1+4);
                str_len=str2.length();
                if(str_len<50)
                {
                    incompleteStr=true;
                    incompString=str2;
                }
                else if(str_len==50)
                {
                    processComplete(str2);
                    incompleteStr=false;
                }
                else if(str_len>50 && str_len<=54)
                {
                    processComplete(str2.substring(0,49));
                    halfState=true;
                    halfStateIndex=4-(str_len-50);
                }
                else if((str_len>54) && (str2.toUpperCase().contains("AA2B"))) {

                    ind2 = str2.indexOf("AA2B");
                    str3=str2.substring(0,ind2);

                    len=str2.length();
                    if((len-1)>(ind2+3))
                    {

                        incompleteStr=true;
                        incompString=str2.substring(ind2+4);
                    }


                        processComplete(str3);

                }
                else {
                    incompleteStr=true;
                    incompString=str2;
                }

            }

counters++;
if(startCounter) {
    startCounter=false;
    handler.postDelayed(new Runnable() {

        @Override
        public void run() {
            Log.d(tag, "counter value:" + counters);
            if (mListener != null)
                mListener.DecodeReport(Integer.toString(counters), 5);
            counters = 0;
            handler.postDelayed(this, 1000);

        }


    }, 1000);
}




    }
    public static String ByteArrayToHex(byte[] bytes) {
        if(bytes!=null){
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString();
        }
        return "";
    }
    public void notifyUARTCommand() {
        BluetoothGattService service = mBluetoothLeService
                .getService(java.util.UUID.fromString(UUID.UARTService));

        if (service == null) {
            Log.d(tag, "Cannot find UART service.");
            mBluetoothLeService.rediscoverServices();
            return;
        }

        BluetoothGattCharacteristic mCharacteristic = service
                .getCharacteristic(java.util.UUID.fromString(UUID.Tx));
        if (mCharacteristic == null) {
            Log.d(tag, "Cannot find Tx characteristic.");
            mBluetoothLeService.rediscoverServices();
            return;
        }


        mBluetoothLeService.setCharacteristicNotification(mCharacteristic, true);
    }
    /**
     * Request to read a characteristic.
     *
     * @param serviceUUID Service id.
     * @param charUUID Characteristic to read.
     * @return True on success
     */

    private boolean readCharacteristic(String serviceUUID, String charUUID) {
        if (mBluetoothLeService == null) {
            Log.d(tag, "Service not available.");
            return false;
        }

        BluetoothGattService service = mBluetoothLeService.getService(java.util.UUID.fromString(serviceUUID));
        if (service == null) {
            Log.d(tag, "Cannot find service.");
            return false;
        }

        BluetoothGattCharacteristic characteristic =
                service.getCharacteristic(java.util.UUID.fromString(charUUID));
        if (characteristic == null) {
            Log.d(tag, "Cannot find characteristic.");
            return false;
        }


        boolean success = mBluetoothLeService.readCharacteristic(characteristic);
        Log.d(tag, "Read request successful: " + success);


        if(!success){

        }



        return success;
    }
    private static int count=0;
    private final Handler handler=new Handler();
    public void processComplete(String complete) {
        count++;
        Log.d(tag, "string found"+count);


           String accel = complete.substring(0,6);
            if (mListener != null)
                mListener.DecodeReport(accel, 3);

if(complete.length()>32) {
    String ecg = complete.substring(14, 32);
    if (mListener != null)
        mListener.DecodeReport(ecg, 4);
}




    }

    /**
     * Disconnect from the device. It is possible to connect again.
     */
    public void disconnect() {
        connectionState = DISCONNECTING;

        if (mBluetoothLeService == null) {
            Log.d(tag, "Cannot disconnect - Service not available");
            return;
        }

        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
    }
    /**
     * Return the connection state.
     *
     * @return state
     */
    public int getConnectionState() {
        return connectionState;
    }

    /**
     * Unregister from the update receiver.
     *
     * Needs to be called onPause();
     */
    public void onPause() {
        delayHandler.removeCallbacksAndMessages(null);
        if(handler!=null)
            handler.removeCallbacksAndMessages(null);
        try {
            mContext.unregisterReceiver(mGattUpdateReceiver);
        }
        catch (IllegalArgumentException e) {
            Log.d(tag, "Trying to unregister a receiver not registered..");
        }
    }


    /**
     * Unbind the BLE Service.
     *
     * Needs to be called onDestroy().
     */
    public void onDestroy() {
        try {
            mContext.unregisterReceiver(mGattUpdateReceiver);
        }
        catch (IllegalArgumentException e) {
            Log.d(tag, "Trying to unregister a receiver not registered..");
        }

        try {
            mContext.unbindService(mServiceConnection);
        }
        catch (IllegalArgumentException e) {
            Log.d(tag, "Trying to unbind a service not registered..");
        }

        mBluetoothLeService = null;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
