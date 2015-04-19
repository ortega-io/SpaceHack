package in.spacehack.android.client;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import in.spacehack.android.bluetooth.BluetoothClient;


public class Scanning extends Activity
{

    BluetoothDiscoveryWatcher discoveryWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);

        // Load TypeFaces //

        Typeface font_orbitron = Typeface.createFromAsset(getAssets(), "fonts/orbitron_light.otf");

        // Load TextView Elements //

        TextView connectingLabel = (TextView)findViewById(R.id.label_connecting);

        // Set TypeFace to elements //

        connectingLabel.setTypeface(font_orbitron);

    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Register Discovery Watcher //

        IntentFilter filter     = new IntentFilter(BluetoothClient.DEVICE_DISCOVERY_FINISHED);
        discoveryWatcher        = new BluetoothDiscoveryWatcher();

        registerReceiver(discoveryWatcher, filter);

    }

    @Override
    protected void onStop()
    {
        super.onStop();

        unregisterReceiver(discoveryWatcher);
    }


    // Bluetooth discovery watcher [START] ================================== //

    /**
     *
     * BluetoothWatcher
     *
     * Implements a watcher to handle changes in the status of the bluetooth adapter.
     *
     */

    private class BluetoothDiscoveryWatcher extends BroadcastReceiver
    {

        /**
         * ----------------------------------------------------
         * void onReceive(Context context, Intent intent)
         * ----------------------------------------------------
         *
         * Handles changes in the status of the bluetooth adapter.
         *
         * @param  Context context
         * @param  Intent  intent
         *
         *
         */

        @Override
        public void onReceive(Context context, Intent intent)
        {

            final String action = intent.getAction();

            // Handling changes in adapter status //

            if (action.equals(BluetoothClient.DEVICE_DISCOVERY_FINISHED))
            {

                Log.d(BluetoothClient.BLUETOOTH_CLIENT_LOG_TAG, "Discovery Broadcast Received");


                Intent devicesList = new Intent(getBaseContext(), BluetoothDiscoveredDevices.class);
                startActivity(devicesList);


            }

        }

    }

    // Bluetooth discovery watcher [END] ==================================== //

}
