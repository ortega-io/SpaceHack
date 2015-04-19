package in.spacehack.android.client;

/* Import sections ========================================================== */

import android.app.ListActivity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import in.spacehack.android.bluetooth.BluetoothClient;


/**

Paired Devices

Show list of paired bluetooth devices


@package    Android
@subpackage Bluetooth
@author     Otoniel Ortega <ortega_x2@hotmail.com>
@copyright  2015 Otoniel Ortega (c)
@version    1.0
@license    CC BY-NC 4.0 (https://creativecommons.org/licenses/by-nc/4.0/)

*/

public class BluetoothPairedDevices extends ListActivity
{

    /* Bluetooth Client Service Related Variables [START] =================== */

    private Messenger           bluetoothClient;
    private boolean             bluetoothClientBound;
    final   Messenger           bluetoothRepliesMessenger   = new Messenger(new IncomingReplyHandler());
    private ServiceConnection   bluetoothClientConnection   = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {

            bluetoothClient        = new Messenger(service);
            bluetoothClientBound   = true;

            // Check if service is connected //

            Message isConnectedMsg  = new Message();
            isConnectedMsg.what     = BluetoothClient.IS_CONNECTED;
            isConnectedMsg.replyTo  = bluetoothRepliesMessenger;

            try
            {
                bluetoothClient.send(isConnectedMsg);

            }catch(RemoteException e)
            {
                BluetoothClient.log("Failed to send isConnectedMsg");
            }

            onServiceBound();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            bluetoothClient        = null;
            bluetoothClientBound   = false;

        }

    };

    private static boolean              bluetoothAdapterPresent  = true;
    private static boolean              bluetoothAdapterStatus   = true;
    private static boolean              bluetoothTriggerIsDown   = false;
    private static boolean              bluetoothClientConnected = false;
    private ArrayList<BluetoothDevice>  bluetoothPairedDevices;

    /* Bluetooth Client Service Related Variables [END] ===================== */




    /**
    * ----------------------------------------------------
    *  void onCreate(Bundle savedInstanceState)
    *  ----------------------------------------------------
    *
    *  onCreate method for the activity
    *
    *  @return void
    *
    */

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set up custom layout //
        setContentView(R.layout.bluetooth_activity_paired_devices);

        // Load TypeFaces //

        Typeface font_orbitron = Typeface.createFromAsset(getAssets(), "fonts/orbitron_light.otf");

        // Load TextView Elements //

        TextView titleTextView = (TextView)findViewById(R.id.title_paired_devices);

        // Set TypeFace to elements //
        titleTextView.setTypeface(font_orbitron);

    }


    @Override
    protected void onStart()
    {
        super.onStart();

        // Bluetooth Client: Bind to Service //

        Intent intent = new Intent(this, BluetoothClient.class);
        bindService(intent, bluetoothClientConnection, Context.BIND_AUTO_CREATE);


    }

    @Override
    protected void onStop()
    {
        super.onStop();

        // Bluetooth Client: Unbind from Service //

        if (bluetoothClientBound)
        {
            unbindService(bluetoothClientConnection);
            bluetoothClientBound = false;
        }

    }


    protected void onServiceBound()
    {

        if(bluetoothAdapterStatus== BluetoothClient.ADAPTER_ENABLED)
        {

            // Check if adapter is present //

            Message getAvailableDevicesMsg  = new Message();
            getAvailableDevicesMsg.what     = BluetoothClient.GET_DEVICES_AVAILABLE;
            getAvailableDevicesMsg.replyTo  = bluetoothRepliesMessenger;

            try
            {
                bluetoothClient.send(getAvailableDevicesMsg);

            }
            catch(RemoteException e)
            {
                BluetoothClient.log("Failed to send getDevicesMsg");
            }

        }
        else
        {
            sendNotification("Turn On Bluetooth Adapter First!");
        }
    }


    protected void sendNotification(String message)
    {

        Toast toast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT);
        toast.show();

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        BluetoothDevice selectedDevice = bluetoothPairedDevices.get(position);


        Intent connecting = new Intent(this, Connecting.class);
        startActivity(connecting);


        // Connect to Device //

        Message connectDevicesMsg  = new Message();
        connectDevicesMsg.what     = BluetoothClient.CONNECT;
        connectDevicesMsg.replyTo  = bluetoothRepliesMessenger;
        connectDevicesMsg.obj      = selectedDevice;

        try
        {
            bluetoothClient.send(connectDevicesMsg);

        }
        catch(RemoteException e)
        {
            BluetoothClient.log("Failed to send connectDevicesMsg");
        }


    }


    // ===================================================================== //
    // IPC Messages Handler [START] ========================================== //

    /**
     * -------------------------------------------------------------
     * 	IncomingHandler
     * -------------------------------------------------------------
     *
     * Process IPC messages to client.
     *
     */

    class IncomingReplyHandler extends Handler
    {

        @Override
        public void handleMessage(Message message)
        {
            switch (message.what)
            {


                // GET_ADAPTER_STATUS_REPLY ================================= //

                case BluetoothClient.GET_ADAPTER_STATUS_REPLY:

                    bluetoothAdapterStatus = (message.arg1==1) ? true : false;

                break;


                // GET_DEVICES_AVAILABLE_REPLY ============================== //

                case BluetoothClient.GET_DEVICES_AVAILABLE_REPLY:

                    bluetoothPairedDevices = (ArrayList<BluetoothDevice>)message.obj;

                    if (bluetoothPairedDevices != null)
                    {
                        BluetoothDevicesAdapter adapter = new BluetoothDevicesAdapter(getBaseContext(), bluetoothPairedDevices);
                        setListAdapter(adapter);
                    }


                break;


                default:
                    //
            }
        }
    }


    // IPC Messages Handler [END] ========================================== //
    // ===================================================================== //

}