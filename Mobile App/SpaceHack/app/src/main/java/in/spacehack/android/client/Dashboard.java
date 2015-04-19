package in.spacehack.android.client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Bundle;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import in.spacehack.android.bluetooth.BluetoothClient;
import in.spacehack.android.bluetooth.BluetoothMessage;
import in.spacehack.android.sensors.SensorsService;
//import in.spacehack.android.sensors.observer.SensorChangedObserver;


public class Dashboard extends Activity //implements SensorChangedObserver
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
                log("Failed to send isConnectedMsg");
            }


            // Check if adapter is present //

            Message checkAdapterMsg  = new Message();
            checkAdapterMsg.what     = BluetoothClient.IS_ADAPTER_PRESENT;
            checkAdapterMsg.replyTo  = bluetoothRepliesMessenger;

            try
            {
                bluetoothClient.send(checkAdapterMsg);

            }
            catch(RemoteException e)
            {
                log("Failed to send checkAdapterMsg");
            }

            // Check if adapter is enabled //

            Message checkAdapterStatusMsg    = new Message();
            checkAdapterStatusMsg.what       = BluetoothClient.GET_ADAPTER_STATUS;
            checkAdapterStatusMsg.replyTo    = bluetoothRepliesMessenger;

            try
            {
                bluetoothClient.send(checkAdapterStatusMsg);

            }catch(RemoteException e)
            {
                log("Failed to send checkAdapterStatusMsg");
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            bluetoothClient        = null;
            bluetoothClientBound   = false;

        }

    };

    private BluetoothDiscoveryWatcher   bluetoothDiscoveryWatcher;
    private static boolean              bluetoothAdapterPresent  = false;
    private static boolean              bluetoothAdapterStatus   = false;
    private static boolean              bluetoothTriggerIsDown   = false;
    private static boolean              bluetoothClientConnected = false;


    /* Bluetooth Client Service Related Variables [END] ===================== */



    /* Sensors Related Variables [START] ==================================== */

    private Messenger           sensorsService;
    private boolean             sensorsServiceBound;
    final   Messenger           sensorsRepliesMessenger     = new Messenger(new IncomingReplyHandler());
    private ServiceConnection   sensorsServiceConnection    = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {

            sensorsService      = new Messenger(service);
            sensorsServiceBound = true;

            log("Sensors Service Bound");


        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            sensorsService      = null;
            sensorsServiceBound = false;

            log("Sensors Service Unbound");
        }

    };

    /* Sensors Related Variables [END] ===================================== */


    /* Touchpad actions handling [START] =================================== */

    TouchpadHandler         touchpadHandler         = new TouchpadHandler("touchpad");
    TouchpadHandler         scrollbarHandler        = new TouchpadHandler("scrollbar");
    TouchpadButtonsHandler  touchpadLcClicksHandler = new TouchpadButtonsHandler("key");
    TouchpadButtonsHandler  touchpadRcClicksHandler = new TouchpadButtonsHandler("right_key");

    /* Touchpad actions handling [END] ===================================== */




    private Button      startScanButton;
    private TextView    connectionStatus;


    /* Sensors and media key trigger related variables [START] ============== */


    /* Sensors and media key trigger related variables [END] ================ */




    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        // Load TypeFaces           //

        Typeface font_orbitron = Typeface.createFromAsset(getAssets(), "fonts/orbitron_light.otf");


        // Load TextView Elements   //

        TextView titleTextView = (TextView)findViewById(R.id.title);
        connectionStatus = (TextView)findViewById(R.id.connection_status);



        // Load Button Elements     //

        Button connectButton        = (Button)findViewById(R.id.button_connect);
        Button scanDevicesButton    = (Button)findViewById(R.id.button_scan_devices);
        Button leftClickButton      = (Button)findViewById(R.id.button_left_click);
        Button rightClickButton     = (Button)findViewById(R.id.button_right_click);
        Button keyboardButton       = (Button)findViewById(R.id.button_keyboard);


        // Set TypeFace to elements //

        connectButton.setTypeface(font_orbitron);
        scanDevicesButton.setTypeface(font_orbitron);
        leftClickButton.setTypeface(font_orbitron);
        rightClickButton.setTypeface(font_orbitron);
        keyboardButton.setTypeface(font_orbitron);
        titleTextView.setTypeface(font_orbitron);


        // Define touchpad element //

        LinearLayout touchpad   = (LinearLayout)findViewById(R.id.touchpad);
        LinearLayout scrollbar  = (LinearLayout)findViewById(R.id.scrollbar);

        touchpad.setOnTouchListener(touchpadHandler);
        scrollbar.setOnTouchListener(scrollbarHandler);

        leftClickButton.setOnTouchListener(touchpadLcClicksHandler);
        rightClickButton.setOnTouchListener(touchpadRcClicksHandler);

    }


    @Override
    protected void onStart()
    {

        super.onStart();

        /* Bluetooth Communication Related Actions [START] ================== */

        // Bluetooth Client: Bind to Service    //

        Intent bluetoothClientIntent    = new Intent(this, BluetoothClient.class);
        bindService(bluetoothClientIntent, bluetoothClientConnection, Context.BIND_AUTO_CREATE);
        startService(bluetoothClientIntent);


        // Register Discovery Watcher           //

        IntentFilter filter             = new IntentFilter(BluetoothClient.DEVICE_DISCOVERY_FINISHED);
        bluetoothDiscoveryWatcher       = new BluetoothDiscoveryWatcher();

        registerReceiver(bluetoothDiscoveryWatcher, filter);

        /* Bluetooth Communication Related Actions [END] ==================== */


        /* Sensors Monitoring Related Actions [START] ======================= */

        // Sensors Service: Bind to Service    //

        Intent sensorsServiceintent     = new Intent(this, SensorsService.class);
        bindService(sensorsServiceintent, sensorsServiceConnection, Context.BIND_AUTO_CREATE);
        startService(sensorsServiceintent);

        /* Sensors Monitoring Related Actions [END] ========================= */


    }


    @Override
    protected void onStop()
    {
        super.onStop();

        // Bluetooth Client: Unbind from Service //

        if (bluetoothClientBound)
        {
            unbindService(bluetoothClientConnection);
            unregisterReceiver(bluetoothDiscoveryWatcher);
            bluetoothClientBound = false;
        }

        // Sensors Service: Unbind from Service //

        //*
        if (sensorsServiceBound)
        {
            unbindService(sensorsServiceConnection);
            sensorsServiceBound = false;
        }
        //*/


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

        super.onDestroy();

        /*
        // Stop Services //

        Intent bluetoothClientIntent    = new Intent(this, BluetoothClient.class);
        stopService(bluetoothClientIntent);

        Intent sensorsServiceIntent     = new Intent(this, SensorsService.class);
        stopService(sensorsServiceIntent);
        */

    }


    /* Bluetooth Client Related Methods [START] ============================= */


    /**
     * ----------------------------------------------------
     * listPairedDevices(View view)
     * ----------------------------------------------------
     *
     * Show list of paired devices
     *
     * return void
     *
     * */


    public void listPairedDevices(View view)
    {

        if( bluetoothClientBound && this.bluetoothAdapterPresent)
        {

            if(this.bluetoothAdapterStatus == BluetoothClient.ADAPTER_DISABLED)
            {

                Toast.makeText(getBaseContext(), "You need to enable the Adapter First", Toast.LENGTH_SHORT).show();

                Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnOnIntent, BluetoothClient.REQUEST_ENABLE_DEVICE);

            }
            else
            {
                Intent devicesList  = new Intent(this, BluetoothPairedDevices.class);
                startActivity(devicesList);
            }

        }
        else
        {
            log("NO Bluetooth Adapter is Present");
        }


    }


    /**
     * ----------------------------------------------------
     * onActivityResult(int requestCode, int resultCode, Intent data)
     * ----------------------------------------------------
     *
     * To receive result of BluetoothEnabled
     *
     * return void
     *
     * */

     @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
     {

        if (requestCode == BluetoothClient.REQUEST_ENABLE_DEVICE)
        {


            // Check if adapter is enabled //

            Message checkAdapterStatusMsg    = new Message();
            checkAdapterStatusMsg.what       = BluetoothClient.GET_ADAPTER_STATUS;
            checkAdapterStatusMsg.replyTo    = bluetoothRepliesMessenger;

            try
            {
                bluetoothClient.send(checkAdapterStatusMsg);

            }catch(RemoteException e)
            {
                log("Failed to send checkAdapterStatusMsg");
            }

        }
        else if(requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK)
        {
            List<String> results    = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText       = results.get(0);

            // The command was!!!!
            log(">> Voice command:"+spokenText);

            if(bluetoothClientConnected)
            {

                JSONObject voiceData    = new JSONObject();
                try
                {
                    voiceData.put("command", spokenText);
                }
                catch
                (JSONException e)
                {
                    e.printStackTrace();
                }


                // Create BluetoothMessage ================================= //

                BluetoothMessage newVoiceCommand = new BluetoothMessage();

                newVoiceCommand.setType("voice");
                newVoiceCommand.setData(voiceData);

                Message voiceMsg = new Message();
                voiceMsg.what = BluetoothClient.SEND_DATA;
                voiceMsg.obj = newVoiceCommand;

                try {
                    bluetoothClient.send(voiceMsg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

        }


    }


    /**
     * ----------------------------------------------------
     * scanForDevices(View view)
     * ----------------------------------------------------
     *
     * Start Bluetooth Devices Discovery
     *
     * return void
     *
     * */


    public void scanForDevices(View view)
    {

        // Launch Scanning Window //

        Intent  scanning = new Intent(this, Scanning.class);
        startActivity(scanning);

        // Start Scanning //

        Message startScanning   = new Message();
        startScanning.what      = BluetoothClient.START_DISCOVERY;

        try
        {
            bluetoothClient.send(startScanning);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }

    }

    /* Bluetooth Client Related Methods [END] =============================== */


    /* Voice related methods [START] ======================================== */

    public void voiceCommand(View view)
    {
        (new displaySpeechRecognizer()).run();
    }

    private static final int SPEECH_REQUEST_CODE = 9999;

    /**
     *  This class implement Runnable to run in a new Thread
     */
    public class displaySpeechRecognizer implements Runnable
    {

        @Override
        public void run()
        {
            final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        }

    }

    /* Voice related methods [END] ========================================== */





    // Private inner Classes [START] ======================================== //
    // ====================================================================== //


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

            if (action.equals(BluetoothClient.DEVICE_DISCOVERY_FINISHED))
            {

                startScanButton.setText("Scan for devices");

                Intent devicesList = new Intent(getBaseContext(), BluetoothDiscoveredDevices.class);
                startActivity(devicesList);

            }

        }

    }

    // Bluetooth discovery watcher [END] ==================================== //


    // Touchpad Handler [START] ============================================= //


    private class TouchpadButtonsHandler extends Thread implements View.OnTouchListener
    {
        String keyType;

        TouchpadButtonsHandler(String keyType)
        {
            this.keyType    = keyType;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            int action      = event.getActionMasked();

            switch(action)
            {
                case MotionEvent.ACTION_DOWN:

                    BluetoothMessage  onKeyDown     = new BluetoothMessage();
                    onKeyDown.setType("media_key");
                    JSONObject  eventData           = new JSONObject();

                    try
                    {
                        eventData.put("event_type", this.keyType+"_down");
                    }
                    catch (JSONException e)
                    {
                        // Error?
                    }

                    onKeyDown.setData(eventData);

                    // Send message to server //

                    Message keyDownMsg  = new Message();
                    keyDownMsg.what     = BluetoothClient.SEND_DATA;
                    keyDownMsg.obj      = onKeyDown;

                    try
                    {
                        bluetoothClient.send(keyDownMsg);

                    }
                    catch(RemoteException e)
                    {
                        log("Failed to send keyDownMsg");
                    }

                break;


                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    BluetoothMessage  onKeyUp     = new BluetoothMessage();
                    onKeyUp.setType("media_key");
                    JSONObject  eventDataUp       = new JSONObject();

                    try
                    {
                        eventDataUp.put("event_type", this.keyType+"_up");
                    }
                    catch (JSONException e)
                    {
                        // Error?
                    }

                    onKeyUp.setData(eventDataUp);

                    // Send message to server //

                    Message keyUpMsg  = new Message();
                    keyUpMsg.what     = BluetoothClient.SEND_DATA;
                    keyUpMsg.obj      = onKeyUp;

                    try
                    {
                        bluetoothClient.send(keyUpMsg);

                    }
                    catch(RemoteException e)
                    {
                        log("Failed to send keyDownMsg");
                    }

                break;

            }

            return false;
        }

    }


    private class TouchpadHandler extends Thread implements View.OnTouchListener
    {

        VelocityTracker mVelocityTracker = null;
        boolean         wasMoving        = false;


        String actionType;

        TouchpadHandler(String actionType)
        {
            this.actionType    = actionType;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {

            int index       = event.getActionIndex();
            int action      = event.getActionMasked();
            int pointerId   = event.getPointerId(index);

            switch(action)
            {
                case MotionEvent.ACTION_DOWN:

                    if(mVelocityTracker == null)
                    {
                        // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                        mVelocityTracker = VelocityTracker.obtain();
                    }
                    else
                    {
                        // Reset the velocity tracker back to its initial state.
                        mVelocityTracker.clear();
                    }

                    // Add a user's movement to the tracker.
                    mVelocityTracker.addMovement(event);

                break;

                case MotionEvent.ACTION_MOVE:

                    this.wasMoving  = true;

                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);

                    float speedX = VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId);
                    float speedY = VelocityTrackerCompat.getYVelocity(mVelocityTracker, pointerId);

                    // Create new Bluetooth package ============================= //

                    JSONObject movementData = new JSONObject();
                    JSONObject speedData    = new JSONObject();

                    try
                    {
                        speedData.put("x", speedX);
                        speedData.put("y", speedY);

                    }
                    catch (JSONException e)
                    {
                        // Error ?
                    }

                    try {
                        movementData.put("movement", speedData);
                    } catch (JSONException e) {
                        // Error ?
                    }

                    // Create BluetoothMessage ================================= //

                    BluetoothMessage newMovement = new BluetoothMessage();

                    newMovement.setType(this.actionType);
                    newMovement.setData(movementData);

                    Message coordinatesMsg  = new Message();
                    coordinatesMsg.what     = BluetoothClient.SEND_DATA;
                    coordinatesMsg.obj      = newMovement;

                    Log.d(this.actionType, "X: "+speedX+" Y: "+speedY);

                try
                {
                    SimpleDateFormat sdfDate    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    Date now                    = new Date();
                    String strDate              = sdfDate.format(now);
                    Log.d("", ">> Sent to service:"+ strDate);

                    bluetoothClient.send(coordinatesMsg);
                }
                catch (RemoteException e)
                {
                    e.printStackTrace();
                }

                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    if(!this.wasMoving)
                    {

                        BluetoothMessage  onKeyUp       = new BluetoothMessage();

                        onKeyUp.setType("media_key");

                        JSONObject  eventData           = new JSONObject();

                        try
                        {
                            eventData.put("event_type", "touchpad_click");
                        }
                        catch (JSONException e)
                        {
                            // Error?
                        }

                        onKeyUp.setData(eventData);

                        // Send message to server //

                        Message keyUpMsg  = new Message();
                        keyUpMsg.what     = BluetoothClient.SEND_DATA;
                        keyUpMsg.obj      = onKeyUp;

                        try
                        {
                            bluetoothClient.send(keyUpMsg);

                        }catch(RemoteException e)
                        {
                            log("Failed to send keyDownMsg");
                        }


                    }


                    // Return a VelocityTracker object back to be re-used by others.
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    this.wasMoving   = false;

                break;

            }
            return true;

        }

    }

    // Touchpad Handler [END] =============================================== //



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {

        if( (keyCode==KeyEvent.KEYCODE_HEADSETHOOK) && (!this.bluetoothTriggerIsDown) )
        {

            this.bluetoothTriggerIsDown     = true;


            BluetoothMessage  onKeyDown     = new BluetoothMessage();

            onKeyDown.setType("media_key");

            JSONObject  eventData           = new JSONObject();

            try
            {
                eventData.put("event_type", "key_down");
            }
            catch (JSONException e)
            {
                // Error?
            }

            onKeyDown.setData(eventData);


            // Send message to server //

            Message keyDownMsg  = new Message();
            keyDownMsg.what     = BluetoothClient.SEND_DATA;
            keyDownMsg.obj      = onKeyDown;

            try
            {
                bluetoothClient.send(keyDownMsg);

            }
            catch(RemoteException e)
            {
                log("Failed to send keyDownMsg");
            }


            // Notify sensors service //

            Message notifySensorsService    = new Message();
            notifySensorsService.what       = SensorsService.SET_TRIGGER_STATUS;
            notifySensorsService.obj        = this.bluetoothTriggerIsDown;


            try
            {
                sensorsService.send(notifySensorsService);
            }
            catch (RemoteException e)
            {
                SensorsService.log("Error sending connection status notification");
            }


            return true;
        }
        else
        {
            return super.onKeyDown(keyCode, event);
        }

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if( (keyCode==KeyEvent.KEYCODE_HEADSETHOOK) && (this.bluetoothTriggerIsDown) )
        {

            this.bluetoothTriggerIsDown     = false;

            Log.d(BluetoothClient.BLUETOOTH_CLIENT_LOG_TAG,  "KeyUp");

            BluetoothMessage  onKeyUp       = new BluetoothMessage();

            onKeyUp.setType("media_key");

            JSONObject  eventData           = new JSONObject();

            try
            {
                eventData.put("event_type", "key_up");
            }
            catch (JSONException e)
            {
                // Error?
            }

            onKeyUp.setData(eventData);


            // Notify sensors service //

            Message notifySensorsService    = new Message();
            notifySensorsService.what       = SensorsService.SET_TRIGGER_STATUS;
            notifySensorsService.obj        = this.bluetoothTriggerIsDown;


            try
            {
                sensorsService.send(notifySensorsService);
            }
            catch (RemoteException e)
            {
                SensorsService.log("Error sending connection status notification");
            }


            // Send message to server //

            Message keyUpMsg  = new Message();
            keyUpMsg.what     = BluetoothClient.SEND_DATA;
            keyUpMsg.obj      = onKeyUp;

            try
            {
                bluetoothClient.send(keyUpMsg);

            }catch(RemoteException e)
            {
                log("Failed to send keyDownMsg");
            }


            return true;

        }
        else
        {
            return super.onKeyDown(keyCode, event);
        }
    }



    // Private inner Classes [END] ========================================== //
    // ====================================================================== //



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

                // IS_ADAPTER_PRESENT_REPLY ================================= //

                case BluetoothClient.IS_ADAPTER_PRESENT_REPLY:

                    bluetoothAdapterPresent = (message.arg1==1) ? true : false;

                break;

                // GET_ADAPTER_STATUS_REPLY ================================= //

                case BluetoothClient.GET_ADAPTER_STATUS_REPLY:

                    bluetoothAdapterStatus = (message.arg1==1) ? true : false;

                break;


                // IS_CONNECTED_REPLY ======================================= //

                case BluetoothClient.IS_CONNECTED_REPLY:

                    bluetoothClientConnected = (message.arg1==1) ? true : false;

                    if(bluetoothClientConnected)
                    {
                        connectionStatus.setText("CONNECTED");
                        connectionStatus.setTextColor(getResources().getColor(R.color.green));

                    }
                    else
                    {
                        connectionStatus.setText("DISCONNECTED");
                        connectionStatus.setTextColor(getResources().getColor(R.color.red));
                    }


                    Message notifySensorsService    = new Message();
                    notifySensorsService.what       = SensorsService.SET_CONNECTION_STATUS;
                    notifySensorsService.obj        = bluetoothClientConnected;


                    try
                    {
                       sensorsService.send(notifySensorsService);
                    }
                    catch (RemoteException e)
                    {
                        SensorsService.log("Error sending connection status notification");
                    }



                break;


                default:
                    //
            }
        }
    }


    // IPC Messages Handler [END] ========================================== //
    // ===================================================================== //


    public void log(String message)
    {
        Log.d(BluetoothClient.BLUETOOTH_CLIENT_LOG_TAG, message);
    }

}
