package bleproj.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import bleproj.R;
import bleproj.bluetooth.*;


public class BluetoothScanningActivity extends AppCompatActivity implements View.OnClickListener, ScanManager.DeviceScanListener,
        AdapterView.OnItemClickListener {

    private ScanManager scanManager;
    private BluetoothAdapter btAdapter;
    private List<String> knownAddress = new ArrayList<String>();
    private int selectedPosition=-1;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION=0;

    private BluetoothLeService mBluetoothLeService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetoothfragment);


        scanManager = new BluetoothScanManager(this);
        btAdapter = new BluetoothAdapter(this, R.layout.bluetooth_device_item);
        scanManager.setOnNewDeviceFoundListener(this);
        scanManager.startScanning();

        ((ListView) findViewById(R.id.device_list)).setAdapter(btAdapter);
        ((ListView) findViewById(R.id.device_list)).setOnItemClickListener(this);
    }


    @Override
    public void onBackPressed() {

            super.onBackPressed();

    }



    @Override
    public void onClick(View view) {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        scanManager.stopScanning();
        selectedPosition = position;

        PairDevice(getBluetoothDevice(position));
    }

    private void PairDevice(BluetoothDevice device){
        Intent newData = new Intent();
        newData.putExtra(IntentRequest.DataKey.BLUETOOTHDEVICE, device);
        setResult(Activity.RESULT_OK, newData);
        finish();
    }

    /**
     * Return bluetooth device.
     *
     * @param position Device's position
     * @return BluetoothDevice.
     */
    private BluetoothDevice getBluetoothDevice(int position) {
        if (btAdapter == null
                || btAdapter.getCount() <= position
                || position < 0)
            return null;

        return btAdapter.getItem(position);
    }
    @Override
    public void onDeviceFound(BluetoothDevice bluetoothDevice) {
        if (btAdapter == null) return;
        if(knownAddress.contains(bluetoothDevice.getAddress()))
            return;

        knownAddress.add(bluetoothDevice.getAddress());

        btAdapter.add(bluetoothDevice);
        btAdapter.notifyDataSetChanged();
    }
}
