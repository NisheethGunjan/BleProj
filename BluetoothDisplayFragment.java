package bleproj.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import bleproj.R;
import bleproj.bluetooth.BluetoothHelper;
import bleproj.bluetooth.IntentRequest;
import bleproj.bluetooth.UUID;

public class BluetoothDisplayFragment extends AppCompatActivity implements View.OnClickListener,BluetoothHelper.ControlHelperListener {

    String tag = "BluetoothDispFragment";
    TextView displayformat_text1;
    TextView displayformat_text2;
    TextView displayformat_text3;
    TextView displayformat_text4;
    TextView displayformat_text5;
    TextView displayformat_text6;

    ListView connection_display;
    Button backbtn;
    BluetoothHelper helper;
    private BluetoothDevice btDevice;
    private final static String BLUETOOTH_DEVICE_KEY = "bluetoothDevice";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_display);
        Initialize();

        if (savedInstanceState == null) {

            btDevice = getIntent().getParcelableExtra(IntentRequest.DataKey.BLUETOOTHDEVICE);
        }
        else {
            btDevice = savedInstanceState.getParcelable(BLUETOOTH_DEVICE_KEY);
        }

        if (btDevice == null) {
            finish();
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IntializeConnection();

            }
        });
    }


    private void Initialize(){
        displayformat_text1 = (TextView) findViewById(R.id.display_text1);
        displayformat_text2 = (TextView) findViewById(R.id.display_text2);
        displayformat_text3 = (TextView) findViewById(R.id.display_text3);
        displayformat_text4 = (TextView) findViewById(R.id.display_text4);
        displayformat_text5 = (TextView) findViewById(R.id.display_text5);
        displayformat_text6 = (TextView) findViewById(R.id.display_text6);
        connection_display = (ListView) findViewById(R.id.listview);
        backbtn = (Button)findViewById(R.id.back_btn);
        backbtn.setOnClickListener(this);
    }
    @Override
    public void onBackPressed()
    {
        if(helper!=null) {

            helper.setListener(null);
            helper.onDestroy();
        }
        finish();
        super.onBackPressed();
    }

    //start ble connection
    private void IntializeConnection() {
        helper = new BluetoothHelper(this, btDevice);
        helper.initialize();
        helper.setListener(this);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back_btn:
            //if(helper.getConnectionState() == BluetoothHelper.CONNECTED)
               // helper.ReadFirmwareVersion();

                //else{
                //helper.initialize();
                helper.setListener(null);
                helper.disconnect();
                finish();
           // }

                break;
        }
    }

    @Override
    public void onAvailableStateChanged(boolean available) {
            if(available){
              Log.d(tag, "Successfully Connected");
                Handler handler = new Handler();
                helper.ReadFirmwareVersion();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        helper.ReadHardwareRevision();
                    }},500);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        helper.ReadManufacturerName();
                    }},1000);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        helper.notifyUARTCommand();
                    }
                }, 2000);
                /**
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        helper.readUARTData();

                    }
                }, 3000);
                 */
                /**
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        helper.ReadUARTData();
                    }},2000);
                 */



            }
    }

    @Override
    public void onError() {

    }

    @Override
    public void DecodeReport(String data, int type) {
   switch(type) {
       case 0:
           displayformat_text1.setText("Manufacturer Name: "+data);
           break;
       case 1:
           displayformat_text2.setText("Hardware Revision: "+data);
           break;
       case 2:
           displayformat_text3.setText("Firmware Version: "+data);
           break;
       case 3:

           displayformat_text4.setText("UART Data - Accelerometer: "+data);

           break;
       case 4:
           displayformat_text5.setText("UART Data - ECG: "+data);
           break;
       case 5:
           displayformat_text6.setText("Notification per second: "+data);
           break;
       default:
           break;
   }
       //Toast.makeText(this,data, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BLUETOOTH_DEVICE_KEY, btDevice);
    }


    @Override
    protected void onResume() {
        super.onResume();
        helper.initialize();
        helper.setListener(this);


    }

    @Override
    protected void onPause() {
        super.onPause();

        if (helper != null) {
            helper.onPause();
            helper.setListener(null);
        }
    }

}
