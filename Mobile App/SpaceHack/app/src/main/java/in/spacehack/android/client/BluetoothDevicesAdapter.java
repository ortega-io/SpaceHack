package in.spacehack.android.client;


/* Import sections ========================================================== */


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 *
 * BluetoothDevicesAdapter
 *
 * Provides a custom adapter to populate ListViews with bluetooth devices
 *
 *
 * @package    Android
 * @subpackage Bluetooth
 * @author     Otoniel Ortega <ortega_x2@hotmail.com>
 * @copyright  {Year} Otoniel Ortega (c)
 * @version    1.0
 * @license    CC BY-NC 4.0 (https://creativecommons.org/licenses/by-nc/4.0/)
 *
 */


public class BluetoothDevicesAdapter extends ArrayAdapter<BluetoothDevice>
{

    /**
     * ----------------------------------------------------
     *  BluetoothDevicesAdapter(Context context, ArrayAdapter<BluetoothDevice> devices)
     *  ----------------------------------------------------
     *
     *  Creates a new instance of the class an initialize values.
     *
     *  @return BluetoothDevicesAdapter
     *
     */

    public BluetoothDevicesAdapter(Context context, ArrayList<BluetoothDevice> devices)
    {
        super(context, 0, devices);
    }


    /**
     * ----------------------------------------------------
     * getView(int position, View convertView, ViewGroup parent)
     * ----------------------------------------------------
     *
     * Populates listview
     *
     * @param   int         position
     * @param   View        convertView
     * @param   ViewGroup   parent
     *
     * return View
     *
     */

    public View getView(int position, View convertView, ViewGroup parent)
    {

        // Get the data item for this position //

        BluetoothDevice device = getItem(position);


        // Check if an existing view is being reused, otherwise inflate the view //

        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bluetooth_row_paired_device, parent, false);
        }


        // Lookup row elements //

        TextView deviceName = (TextView) convertView.findViewById(R.id.device_name);
        TextView deviceMac  = (TextView) convertView.findViewById(R.id.device_mac);

        // Populate the data //

        deviceName.setText(device.getName());
        deviceMac.setText(device.getAddress());

        return convertView;
    }

}

