package in.spacehack.android.client;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import in.spacehack.android.bluetooth.BluetoothClient;


public class Connecting extends Activity
{

    BluetoothConnectionWatcher connectionWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connecting);

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

        IntentFilter filter     = new IntentFilter(BluetoothClient.CONNECTION_ESTABLISHED);
        connectionWatcher       = new BluetoothConnectionWatcher();

        registerReceiver(connectionWatcher, filter);

    }

    @Override
    protected void onStop()
    {
        super.onStop();

        unregisterReceiver(connectionWatcher);
    }


    // Bluetooth connection watcher [START] ================================= //

    /**
     *
     * BluetoothWatcher
     *
     * Implements a watcher to handle changes in the status of the bluetooth adapter.
     *
     */

    private class BluetoothConnectionWatcher extends BroadcastReceiver
    {

        /**
         * ----------------------------------------------------
         * void onReceive(Context context, Intent intent)
         * ----------------------------------------------------
         *
         * Handles changes in the status of the bluetooth connection.
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

            if (action.equals(BluetoothClient.CONNECTION_ESTABLISHED))
            {

                // Return to dashboard //

                Intent dashboard = new Intent(context, Dashboard.class);

                dashboard.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(dashboard);


            }

        }

    }

    // Bluetooth connection watcher [END] =================================== //

}
