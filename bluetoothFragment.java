package bleproj.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


import bleproj.R;
import bleproj.bluetooth.*;

public class bluetoothFragment extends AppCompatActivity implements View.OnClickListener, ScanManager.DeviceScanListener,
        AdapterView.OnItemClickListener {

    private ScanManager scanManager;
    private BluetoothAdapter btAdapter;
    private List<String> knownAddress = new ArrayList<String>();
    private int selectedPosition=-1;

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
