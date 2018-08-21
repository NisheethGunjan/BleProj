package bleproj.bluetooth;



import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import bleproj.R;



public class BluetoothAdapter extends ArrayAdapter<BluetoothDevice> {
    public BluetoothAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        // First check to see if the view is null. if so, we have to inflate it.
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.bluetooth_device_item, null);
        }

        // Get the object to render.
        BluetoothDevice i = super.getItem(position);

        if (i != null) {
            TextView text_name = (TextView) v.findViewById(R.id.device_name);

            // Check to see if each individual textview is null.
            // if not, assign some text!
            if (text_name != null) {
                if (i.getName() != null) {

                    text_name.setText(i.getName());

                } else
                    text_name.setText("UNKNOWN");
            }
        }
        return v;
    }
}