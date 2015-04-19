package in.spacehack.android.bluetooth;

/* Import sections ========================================================== */

// Bluetooth Related Imports ------------------------------------------------ //

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

// Connection Relates Imports ----------------------------------------------- //
// Misc imports ------------------------------------------------------------- //


/**
 *
 * BluetoothClient
 *
 * Implements a Bound Service that provides the methods needed to connect to a Bluetooth Service,
 * transmission and reception of data.
 *
 *
 * @package     Android
 * @subpackage  Bluetooth
 * @author      Otoniel Ortega <ortega_x2@hotmail.com>
 * @copyright   2015 Otoniel Ortega (c)
 * @version     1.0
 * @license     CC BY-NC 4.0 (https://creativecommons.org/licenses/by-nc/4.0/)
 *
 */

public class BluetoothClient extends Service
{

    /* Private Variables ==================================================== */


    /* Devices related */

    private BluetoothAdapter            bluetoothAdapter;
    private BluetoothSocket 	        bluetoothSocket;
    private BluetoothWatcher            bluetoothWatcher;
    private BroadcastMessagesHandler    broadcastMessagesHandler;
    private ArrayList<BluetoothDevice>  pairedDevices;
    private ArrayList<BluetoothDevice>  devicesInRange;
    private BluetoothDevice             connectedDevice;
    private boolean                     isAdapterPresent;
    private boolean                     isAdapterEnabled;
    private boolean                     deviceDiscoveryOn;
    private static boolean              isConnected     = false;


    /* Communication link related */

    private ConcurrentLinkedQueue<String>   outboundMessages;
    private ConcurrentLinkedQueue<String>   inboundMessages;
    private static final UUID               SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    /* Miscellaneous */

    private Timer                       timer;
    private InputHandler                inputHandler;
    private OutputHandler               outputHandler;
    private final   Messenger           serviceMessenger = new Messenger(new IncomingMessageHandler());


    /* Public Variables ===================================================== */

    public  static final boolean        ADAPTER_ENABLED                 = true;
    public  static final boolean        ADAPTER_DISABLED                = false;
    public  static final int            REQUEST_ENABLE_DEVICE           = 20000;
    public  static final int            BLUETOOTH_DISCOVERY_INTERVAL    = 15;
    public  static final String         BLUETOOTH_CLIENT_LOG_TAG        = "BluetoothClient";
    public  static final String         DEVICE_DISCOVERY_FINISHED       = "in.spacehack.android.BluetoothClient.DEVICE_DISCOVERY_FINISHED";
    public  static final String         NEW_OUTBOUND_MESSAGE            = "in.spacehack.android.BluetoothClient.NEW_OUTBOUND_MESSAGE";
    public  static final String         NEW_INBOUND_MESSAGE             = "in.spacehack.android.BluetoothClient.NEW_INBOUND_MESSAGE";
    public  static final String         MESSAGE_DATA                    = "in.spacehack.android.BluetoothClient.MESSAGE_DATA";
    public  static final String         CONNECTION_ESTABLISHED          = "in.spacehack.android.bluetooth.BluetoothClient.CONNECTION_ESTABLISHED";


    /* IPC Method Calls ===================================================== */

    public static final int IS_ADAPTER_PRESENT		= 100;
    public static final int GET_ADAPTER_STATUS		= 101;
    public static final int IS_CONNECTED			= 102;
    public static final int GET_DEVICES_AVAILABLE	= 103;
    public static final int GET_DEVICES_IN_RANGE	= 104;
    public static final int START_DISCOVERY		    = 105;
    public static final int PAIR_DEVICE			    = 106;
    public static final int UNPAIR_DEVICE			= 107;
    public static final int CONNECT				    = 108;
    public static final int SEND_DATA				= 109;
    public static final int DISCONNECT				= 110;

    public static final int IS_ADAPTER_PRESENT_REPLY	= 200;
    public static final int GET_ADAPTER_STATUS_REPLY	= 201;
    public static final int IS_CONNECTED_REPLY			= 202;
    public static final int GET_DEVICES_AVAILABLE_REPLY	= 203;
    public static final int GET_DEVICES_IN_RANGE_REPLY	= 204;
    public static final int START_DISCOVERY_REPLY		= 205;
    public static final int PAIR_DEVICE_REPLY			= 206;
    public static final int UNPAIR_DEVICE_REPLY			= 207;
    public static final int CONNECT_REPLY				= 208;
    public static final int SEND_DATA_REPLY				= 209;
    public static final int DISCONNECT_REPLY			= 210;



    /**
     * ----------------------------------------------------
     * onCreate()
     * ----------------------------------------------------
     *
     * Initialize values.
     *
     * @return void
     */

    @Override
    public void onCreate()
    {

        super.onCreate();


        // Initializing variables //

        /* Devices related */

        this.isAdapterEnabled   = false;
        this.bluetoothAdapter   = BluetoothAdapter.getDefaultAdapter();
        this.bluetoothSocket    = null;
        this.pairedDevices      = new ArrayList<BluetoothDevice>();
        this.devicesInRange     = new ArrayList<BluetoothDevice>();
        this.connectedDevice    = null;
        this.deviceDiscoveryOn  = false;


        /* Communication link related */

        this.inboundMessages    = new ConcurrentLinkedQueue<String>();


        /* Check if there was any adapter available */

        if(this.bluetoothAdapter==null)
        {
            this.isAdapterPresent = false;
            this.isAdapterEnabled = false;

            return;
        }
        else
        {
            this.isAdapterPresent = true;
        }



        /* Set up watcher to handle changes in adapter status */

        this.bluetoothWatcher           = new BluetoothWatcher();
        IntentFilter filterStateChanged = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter filterDeviceFound  = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filterDevicePair   = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(this.bluetoothWatcher, filterStateChanged);
        registerReceiver(this.bluetoothWatcher, filterDeviceFound);
        registerReceiver(this.bluetoothWatcher, filterDevicePair);



        /* Set up broadcast receivers for inbound/outbound messages */

        this.broadcastMessagesHandler       = new BroadcastMessagesHandler();
        IntentFilter filterInboundMessages  = new IntentFilter(NEW_INBOUND_MESSAGE);
        IntentFilter filterOutboundMessages = new IntentFilter(NEW_OUTBOUND_MESSAGE);

        registerReceiver(this.broadcastMessagesHandler, filterInboundMessages);
        registerReceiver(this.broadcastMessagesHandler, filterOutboundMessages);



        /* Check if the adapter is enabled */

        if(this.bluetoothAdapter.isEnabled())
        {
            // Adapter enabled we are good to go //
            this.isAdapterEnabled = true;
        }
        else
        {
            // Prompt user to enable adapter //
            this.isAdapterEnabled = false;
        }


    }


    /**
     * ----------------------------------------------------
     * onStartCommand(Intent intent, int flags, int startId)
     * ----------------------------------------------------
     *
     * Called after onStart.
     *
     * @return void
     */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // Let it continue running until it is stopped.
        return START_STICKY;
    }


    /**
     * ----------------------------------------------------
     * onDestroy()
     * ----------------------------------------------------
     *
     * onDestroy method of the service.
     *
     * @return void
     */

    @Override
    public void onDestroy()
    {
        unregisterReceiver(this.bluetoothWatcher);
        unregisterReceiver(this.broadcastMessagesHandler);
        super.onDestroy();
    }


    /**
     * ----------------------------------------------------
     * onBind(Intent intent)
     * ----------------------------------------------------
     *
     * Returns Messenger
     *
     * param  Intent intent
     *
     * return IBinder
     *
     */

    @Override
    public IBinder onBind(Intent intent)
    {
        return serviceMessenger.getBinder();
    }



    /**
     * ----------------------------------------------------
     * isAdapterPresent()
     * ----------------------------------------------------
     *
     * Reports if a bluetooth adapter was found.
     *
     *
     * return boolean
     *
     * */


    public boolean isAdapterPresent()
    {
        return this.isAdapterPresent;
    }


    /**
     * ----------------------------------------------------
     * log(String message)
     * ----------------------------------------------------
     *
     * Send log message to console.
     *
     *
     * @return void
     *
     * */

    public static void log(String message)
    {
        Log.d(BLUETOOTH_CLIENT_LOG_TAG, message);
    }


    /**
     * ----------------------------------------------------
     * getAdapterStatus()
     * ----------------------------------------------------
     *
     * Returns the status of the Bluetooth device.
     *
     * return boolean
     *
     */

    public boolean getAdapterStatus()
    {
        return this.isAdapterEnabled;
    }


    /**
     * ----------------------------------------------------
     * isConnected()
     * ----------------------------------------------------
     *
     * Returns the status of the connection.
     *
     * return boolean
     *
     */

    public boolean isConnected()
    {
        return this.isConnected;
    }


    /**
     * ----------------------------------------------------
     * getDevicesAvailable()
     * ----------------------------------------------------
     *
     * Returns an ArrayList with all the Bluetooth devices paired.
     *
     * @return ArrayList<BluetoothDevice>
     *
     */

    public ArrayList<BluetoothDevice> getDevicesAvailable()
    {

        // Short circuit check //
        if(!this.isAdapterEnabled)
        {
            return null;
        }

        // Get paired devices //

        ArrayList<BluetoothDevice>  pairedDevices   = new ArrayList<BluetoothDevice>();
        Set<BluetoothDevice>        devices         = bluetoothAdapter.getBondedDevices();

        for(BluetoothDevice device : devices)
        {
            pairedDevices.add(device);
        }

        return pairedDevices;

    }



    /**
     * ----------------------------------------------------
     * getDevicesInRange()
     * ----------------------------------------------------
     *
     * Returns an ArrayList with all the Bluetooth devices discovered.
     *
     * @return ArrayList<BluetoothDevice>
     *
     */

    public ArrayList<BluetoothDevice> getDevicesInRange()
    {

        // Short circuit check //
        if(!this.isAdapterEnabled)
        {
            return null;
        }

        // Return discovered devices //

        return this.devicesInRange;

    }


    /**
     * ----------------------------------------------------
     * startDiscovery()
     * ----------------------------------------------------
     *
     * Start device discovery on Bluetooth Adapter.
     *
     * @return void
     *
     */

    public void startDiscovery()
    {
        this.devicesInRange.clear();
        this.deviceDiscoveryOn  = true;

        bluetoothAdapter.startDiscovery();

        this.timer = new Timer();
        this.timer.schedule(new stopDiscovery(), BLUETOOTH_DISCOVERY_INTERVAL * 1000);

    }



    /**
     * ----------------------------------------------------
     * pairDevice(BluetoothDevice device)
     * ----------------------------------------------------
     *
     * Pairs with remote device.
     *
     * param  BluetoothDevice device
     *
     * @return boolean
     *
     */

    public boolean pairDevice(BluetoothDevice device)
    {
        try
        {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

            return true;
        }
        catch (Exception e)
        {
            return  false;
        }

    }



    /**
     * ----------------------------------------------------
     * unpairDevice(BluetoothDevice device)
     * ----------------------------------------------------
     *
     * Unpairs with remote device.
     *
     * param  BluetoothDevice device
     *
     * @return boolean
     *
     */

    public boolean unpairDevice(BluetoothDevice device)
    {
        try
        {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

            return true;

        }
        catch (Exception e)
        {
            return  false;
        }

    }



    /**
     * ----------------------------------------------------
     * connect(BluetoothDevice device)
     * ----------------------------------------------------
     *
     * Connects with remote server.
     *
     * param  BluetoothDevice device
     *
     * @return boolean
     *
     */

    public boolean connect(BluetoothDevice device)
    {

        try
        {

            this.bluetoothSocket = device.createRfcommSocketToServiceRecord(this.SERVICE_UUID);


            try
            {
                this.isConnected    = true;

                this.bluetoothSocket.connect();

                this.inputHandler   = new InputHandler(this.bluetoothSocket.getInputStream()   );
                this.outputHandler  = new OutputHandler(this.bluetoothSocket.getOutputStream() );

            }
            catch (IOException e)
            {

                this.isConnected    = false;
                this.bluetoothSocket.close();
                return false;
            }

            return true;

        }
        catch (IOException e)
        {
            this.isConnected        = false;
            return false;
        }


    }



    /**
     * ----------------------------------------------------
     * sendData(String message)
     * ----------------------------------------------------
     *
     * Adds message to outbound queue.
     *
     * param  String message
     *
     * @return void
     *
     */

    public void sendData(String message)
    {

        this.outputHandler.pushMessage(message);

    }


    /**
     * ----------------------------------------------------
     * sendData(BluetoothMessage message)
     * ----------------------------------------------------
     *
     * Adds message to outbound queue.
     *
     * param  BluetoothMessage message
     *
     * @return void
     *
     */

    public void sendData(BluetoothMessage message)
    {
        this.outputHandler.pushMessage(message.toString()+"\n");
    }


    /**
     * ----------------------------------------------------
     * disconnect()
     * ----------------------------------------------------
     *
     * Disconnects from the remote server.
     *
     * @return boolean
     *
     */

    public boolean disconnect()
    {


        if(this.bluetoothSocket.isConnected())
        {
            try
            {
                this.isConnected    = false;
                this.bluetoothSocket.close();
                return true;
            }
            catch (IOException e)
            {
                this.isConnected    = false;
                return false;
            }
        }
        else
        {
            return false;
        }

    }



    /**
     * ----------------------------------------------------
     * processInboundMessage(String message)
     * ----------------------------------------------------
     *
     * Process inbound message from the server.
     *
     * param  String message
     *
     * @return void
     *
     */

    public void processInboundMessage(String message)
    {

        log("New reply from server: "+message);

    }



    // Private inner Classes [START] ======================================== //
    // ====================================================================== //


    // Bluetooth status watcher [START] ===================================== //

    /**
     *
     * BluetoothWatcher
     *
     * Implements a watcher to handle changes in the status of the bluetooth adapter.
     *
     */

    private class BluetoothWatcher extends BroadcastReceiver
    {

        /**
         * ----------------------------------------------------
         * void onReceive(Context context, Intent intent)
         * ----------------------------------------------------
         *
         * Handles changes in the status of the bluetooth adapter.
         *
         * param  Context context
         * param  Intent  intent
         *
         *
         */

        @Override
        public void onReceive(Context context, Intent intent)
        {

            final String action = intent.getAction();

            // Handling changes in adapter status //

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {

                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch(state)
                {

                    // Adapter was turned off //
                    case BluetoothAdapter.STATE_OFF:

                        isConnected      = false;
                        isAdapterEnabled = false;

                    break;

                    // Adapter was turned on //
                    case BluetoothAdapter.STATE_ON:
                        isAdapterEnabled = true;
                    break;

                }

            }


            // Handling Device Discovery //

            if(action.equals(BluetoothDevice.ACTION_FOUND))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devicesInRange.add(device);
            }


            // Handling Device Pairing/Unpairing //

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                final int currentState  = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

            }


        }

    }

    // Bluetooth status watcher [END] ======================================= //



    // Inbound/Outbound messages handler [START] ============================ //

    /**
     *
     * BroadcastMessagesHandler
     *
     * Handlers inbound and outbound messages broadcasts.
     *
     */

    private class BroadcastMessagesHandler extends BroadcastReceiver
    {

        /**
         * ----------------------------------------------------
         * void onReceive(Context context, Intent intent)
         * ----------------------------------------------------
         *
         * Handles changes in the status of the bluetooth adapter.
         *
         * param  Context context
         * param  Intent  intent
         *
         *
         */

        @Override
        public void onReceive(Context context, Intent intent)
        {

            final String action = intent.getAction();
            String message      = intent.getStringExtra(MESSAGE_DATA);


            // Handling broadcast messages //

            switch(action)
            {

                // New outbound message //
                case NEW_OUTBOUND_MESSAGE:
                    sendData(message);
                break;

                // New inbound message //
                case NEW_INBOUND_MESSAGE:
                    processInboundMessage(message);
                break;

            }
        }
    }

    // Inbound/Outbound messages handler [END] ============================== //



    // LocalBinder [START] ================================================== //

    /**
     *
     * LocalBinder
     *
     * Implements the binder to run as local service.
     *
     */

    public class LocalBinder extends Binder
    {

        /**
         * ----------------------------------------------------
         * getService()
         * ----------------------------------------------------
         *
         * Returns instance of the service so clients can call public methods.
         *
         * @return  BluetoothClient
         *
         */

        public BluetoothClient getService()
        {
            return BluetoothClient.this;
        }

    }

    // LocalBinder [END] ==================================================== //


    // Bluetooth Devices Discovery Stop [START] ============================= //


    /**
     *
     * stopDiscovery()
     *
     * Stops Bluetooth Devices Discovery.
     *
     */

    public class stopDiscovery extends TimerTask
    {

        public void run()
        {

            bluetoothAdapter.cancelDiscovery();
            timer.cancel();

            deviceDiscoveryOn           = false;
            Intent discoveryFinished    = new Intent();

            discoveryFinished.setAction(DEVICE_DISCOVERY_FINISHED);
            sendBroadcast(discoveryFinished);

        }

    }

    // Bluetooth Devices Discovery Stop [END] =============================== //



    // Input Handler [START] ================================================ //

    /**
     * -------------------------------------------------------------
     * 	InputHandler
     * -------------------------------------------------------------
     *
     * Process incoming messages from server.
     *
     */

    protected class InputHandler extends Thread
    {

        private InputStream                     inputStream;
        private BufferedReader                  inputReader;
        private boolean                         channelOpen;


        /**
         * ----------------------------------------------------
         * InputHandler(InputStream inputStream)
         * ----------------------------------------------------
         *
         * Returns instance of the class and initialize methods.
         *
         * @return  InputHandler
         *
         */

        InputHandler(InputStream inputStream)
        {

            this.inputStream        = inputStream;

            this.inputReader        = new BufferedReader(new InputStreamReader(this.inputStream));
            this.channelOpen        = true;

            this.start();

        }


        public void run()
        {

            while(this.channelOpen)
            {
                try
                {
                    String inboundMessage = this.inputReader.readLine();

                    if(inboundMessage !=null)
                    {
                        if(inboundMessage !="")
                        {

                            inboundMessages.add(inboundMessage);

                            JSONObject message = new JSONObject(inboundMessage);
                            String messageType = (String)message.get("type");

                            Intent newBroadcast    = new Intent();
                            String broadcastType   = "in.spacehack.android.bluetooth.BluetoothClient."+messageType.toUpperCase();

                            newBroadcast.setAction(broadcastType);
                            sendBroadcast(newBroadcast);

                        }
                    }
                    else
                    {
                        disconnect();
                        this.channelOpen = false;
                    }

                }
                catch(Exception e)
                {
                    disconnect();
                    this.channelOpen = false;
                }

            }

        }

    }

    // Input Handler [END] ================================================== //



    // Output Handler [START] =============================================== //

    /**
     * -------------------------------------------------------------
     * 	OutputHandler
     * -------------------------------------------------------------
     *
     * Process outgoing messages to server.
     *
     */

    protected class OutputHandler
    {

        private OutputStream                    outputStream;
        private BufferedWriter                  outputWriter;
        private boolean                         channelOpen;


        /**
         * ----------------------------------------------------
         * OutputHandler(OutputStream stream, ConcurrentLinkedQueue<String> messagesQueue)
         * ----------------------------------------------------
         *
         * Returns instance of the class and initialize methods.
         *
         * @return  OutputHandler
         *
         */

        OutputHandler(OutputStream stream)
        {

            this.outputStream       = stream;
            this.outputWriter       = new BufferedWriter(new OutputStreamWriter(outputStream));
            this.channelOpen        = true;


        }


        public void pushMessage(String outboundMessage)
        {

            if(outboundMessage!=null)
            {
                try
                {



                    SimpleDateFormat sdfDate    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    Date now                    = new Date();
                    String strDate              = sdfDate.format(now);
                    Log.d("", ">> pushed at:"+ strDate);

                    this.outputWriter.write(outboundMessage);
                    this.outputWriter.flush();


                }
                catch (IOException e)
                {
                    disconnect();
                    this.channelOpen    = false;
                }
            }

        }

    }

    // Output Handler [END] ================================================= //


    // ====================================================================== //
    // Private inner Classes [END] ========================================== //



    // ======================================================================= //
    // IPC Messages Handler [START] ========================================== //

    /**
     * -------------------------------------------------------------
     * 	IncomingHandler
     * -------------------------------------------------------------
     *
     * Process IPC messages to service.
     *
     */

    class IncomingMessageHandler extends Handler
    {

        @Override
        public void handleMessage(Message message)
        {
            switch (message.what)
            {

                // IS_ADAPTER_PRESENT ======================================= //

                case IS_ADAPTER_PRESENT:


                    boolean adapterFound          = isAdapterPresent();

                    Message checkAdapterReplyMsg  = new Message();
                    checkAdapterReplyMsg.what     = BluetoothClient.IS_ADAPTER_PRESENT_REPLY;
                    checkAdapterReplyMsg.arg1     = adapterFound ? 1 : 0;
                    try
                    {
                        message.replyTo.send(checkAdapterReplyMsg);

                    }catch(RemoteException e)
                    {
                    }

                break;


                // GET_ADAPTER_STATUS ======================================= //

                case GET_ADAPTER_STATUS:


                    boolean adapterStatus               = getAdapterStatus();

                    Message checkAdapterStatusReplyMsg  = new Message();
                    checkAdapterStatusReplyMsg.what     = BluetoothClient.GET_ADAPTER_STATUS_REPLY;
                    checkAdapterStatusReplyMsg.arg1     = adapterStatus ? 1 : 0;
                    try
                    {
                        message.replyTo.send(checkAdapterStatusReplyMsg);

                    }catch(RemoteException e)
                    {

                    }

                break;


                // IS_CONNECTED ============================================= //

                case IS_CONNECTED:

                    boolean isConnected          = isConnected();

                    Message isConnectedReplyMsg  = new Message();
                    isConnectedReplyMsg.what     = BluetoothClient.IS_CONNECTED_REPLY;
                    isConnectedReplyMsg.arg1     = isConnected ? 1 : 0;
                    try
                    {
                        message.replyTo.send(isConnectedReplyMsg);

                    }catch(RemoteException e)
                    {

                    }

                break;


                // GET_DEVICES_AVAILABLE ==================================== //

                case GET_DEVICES_AVAILABLE:


                    ArrayList<BluetoothDevice>  devices  = getDevicesAvailable();

                    Message getAvailableDevicesReplyMsg  = new Message();
                    getAvailableDevicesReplyMsg.what     = BluetoothClient.GET_DEVICES_AVAILABLE_REPLY;
                    getAvailableDevicesReplyMsg.obj      = devices;

                    try
                    {
                        message.replyTo.send(getAvailableDevicesReplyMsg);

                    }
                    catch(RemoteException e)
                    {

                    }

                break;


                // GET_DEVICES_IN_RANGE ===================================== //

                case GET_DEVICES_IN_RANGE:

                    ArrayList<BluetoothDevice>  devicesInRange  = getDevicesInRange();

                    Message getDevicesInRangeReplyMsg         = new Message();
                    getDevicesInRangeReplyMsg.what            = BluetoothClient.GET_DEVICES_IN_RANGE_REPLY;
                    getDevicesInRangeReplyMsg.obj             = devicesInRange;

                    try
                    {
                        message.replyTo.send(getDevicesInRangeReplyMsg);

                    }
                    catch(RemoteException e)
                    {

                    }

                break;

                // START_DISCOVERY ========================================== //

                case START_DISCOVERY:

                    startDiscovery();

                break;

                // PAIR_DEVICE ============================================== //

                case PAIR_DEVICE:

                    BluetoothDevice  deviceToPair = (BluetoothDevice)message.obj;
                    pairDevice(deviceToPair);

                    Message pairDeviceReplyMsg         = new Message();
                    pairDeviceReplyMsg.what            = BluetoothClient.PAIR_DEVICE_REPLY;

                    try
                    {
                        message.replyTo.send(pairDeviceReplyMsg);

                    }
                    catch(RemoteException e)
                    {

                    }

                break;


                // UNPAIR_DEVICE ============================================ //

                case UNPAIR_DEVICE:


                break;


                // CONNECT ================================================== //

                case CONNECT:

                    BluetoothDevice  selectedDevice = (BluetoothDevice)message.obj;
                    connect(selectedDevice);

                break;

                // SEND_DATA ================================================ //

                case SEND_DATA:

                    if(isConnected())
                    {
                        log("SendingData");
                        BluetoothMessage newMessage = (BluetoothMessage) message.obj;
                        sendData(newMessage);
                    }

                break;


                // DISCONNECT =============================================== //

                case DISCONNECT:


                break;


                default:
                    //
                break;
            }
        }
    }


    // IPC Messages Handler [END] ========================================== //
    // ===================================================================== //

}
