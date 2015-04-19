package in.spacehack.android.sensors;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

import in.spacehack.android.bluetooth.BluetoothClient;
import in.spacehack.android.bluetooth.BluetoothMessage;
import in.spacehack.android.sensors.filters.FusedGyroscopeSensor;
import in.spacehack.android.sensors.filters.MeanFilter;
import static in.spacehack.android.sensors.Common.matrixMultiplication;
import in.spacehack.android.sensors.observer.FusedGyroscopeSensorListener;

/**
 *
 * SensorsService
 *
 * Service to handle updates on the sensors.
 *
 *
 * @package     Android
 * @subpackage  Sensors
 * @author      Otoniel Ortega <ortega_x2@hotmail.com>
 * @copyright   2015 Otoniel Ortega (c)
 * @version     1.0
 * @license     CC BY-NC 4.0 (https://creativecommons.org/licenses/by-nc/4.0/)
 *
 */

public class SensorsService extends Service
{

    /* Private Variables ==================================================== */

    /* Service Communication */

    private final Messenger serviceMessenger = new Messenger(new IncomingMessageHandler());

    /* Updates handling */

    private boolean     triggerDown         = false;
    private boolean     connectionActive    = false;


    /* Sensors Updates */

    private SensorHandler sensorHandler;


    /* Public Variables ===================================================== */

    /* Constants */

    public  static final String         SENSOR_SERVICE_LOG_TAG        = "SensorsService";


    /* IPC Method Calls ===================================================== */

    public static final int SET_TRIGGER_STATUS  	= 300;
    public static final int SET_CONNECTION_STATUS   = 301;


    /* Bluetooth Client Service Related Variables [START] =================== */

    private Messenger           bluetoothClient;
    private boolean             bluetoothClientBound;
    private ServiceConnection   bluetoothClientConnection   = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {

            bluetoothClient        = new Messenger(service);
            bluetoothClientBound   = true;;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            bluetoothClient        = null;
            bluetoothClientBound   = false;

        }

    };

    /* Bluetooth Client Service Related Variables [END] ===================== */



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
     * onCreate()
     * ----------------------------------------------------
     *
     * Initialize values attach listener.
     *
     * @return void
     */


    @Override
    public void onCreate()
    {
        super.onCreate();

        this.sensorHandler  = new SensorHandler();
        this.sensorHandler.restart();

        // Bluetooth Client: Bind to Service //

        Intent intent = new Intent(this, BluetoothClient.class);
        bindService(intent, bluetoothClientConnection, Context.BIND_AUTO_CREATE);

    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();

        this.sensorHandler.reset();

        // Bluetooth Client: Unbind from Service //

        if (bluetoothClientBound)
        {
            unbindService(bluetoothClientConnection);
            bluetoothClientBound = false;
        }

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
        Log.d(SENSOR_SERVICE_LOG_TAG, message);
    }


    // Private inner Classes [START] ======================================== //
    // ====================================================================== //

    /**
     *
     * SensorHandler
     *
     * Handles sensor data updates and applies filters + sensor fusion.
     *
     *
     */

    public class SensorHandler implements SensorEventListener, FusedGyroscopeSensorListener
    {

        // Filter Related Variables ========================================= //

        // Constants //

        public static final float   EPSILON                     = 0.000000001f;
        private static final float  NS2S                        = 1.0f / 1000000000.0f;
        private static final int    MEAN_FILTER_WINDOW          = 10;
        private static final int    MIN_SAMPLE_COUNT            = 30;


        // Fusion related variables //

        private int                 accelerationSampleCount     = 0;
        private int                 magneticSampleCount         = 0;
        private boolean             hasInitialOrientation       = false;
        private boolean             stateInitializedCalibrated  = false;
        private boolean             stateInitializedRaw         = false;
        private boolean             useFusedEstimation          = false;
        private DecimalFormat       df;


        // Structures to hold sensor data =================================== //

        // Calibrated maths //
        private float[] currentRotationMatrixCalibrated;
        private float[] deltaRotationMatrixCalibrated;
        private float[] deltaRotationVectorCalibrated;
        private float[] gyroscopeOrientationCalibrated;
        private float[] currentGyroscopeOrientationCalibrated;

        // Uncalibrated maths //
        private float[] currentRotationMatrixRaw;
        private float[] deltaRotationMatrixRaw;
        private float[] deltaRotationVectorRaw;
        private float[] gyroscopeOrientationRaw;

        // accelerometer and magnetometer based rotation matrix //
        private float[] initialRotationMatrix;
        private float[] acceleration;
        private float[] magnetic;


        // Structures to hold sensor data =================================== //

        private FusedGyroscopeSensor    fusedGyroscopeSensor;
        private long                    timestampOldCalibrated  = 0;
        private long                    timestampOldRaw         = 0;
        private MeanFilter              accelerationFilter;
        private MeanFilter              magneticFilter;


        // Get instance of the sensor manager //

        private SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);



        // Define sensors to monitor ======================================== //

        private float[] gyroscope       = new float[]{0, 0, 0};
        private float[] accelerometer   = new float[]{0, 0, 0};
        private float[] orientation     = new float[]{0, 0, 0};
        private float[] gravity         = new float[]{0, 0, 0};



        // Define if sensor data is available =============================== //

        private boolean gyroscopeReady      = false;
        private boolean accelerometerReady  = false;
        private boolean orientationReady    = false;
        private boolean gravityReady        = false;



        public SensorHandler()
        {

            // Initialize filters & fusion related data //

            initMaths();
            initSensors();
            initFilters();

        }


        /**
         * ----------------------------------------------------
         * onSensorChanged(SensorEvent event)
         * ----------------------------------------------------
         *
         * Listen to sensor updates
         *
         * @return void
         *
         */

        @Override
        public void onSensorChanged(SensorEvent event)
        {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                onAccelerationSensorChanged(event.values, event.timestamp);
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            {
                onMagneticSensorChanged(event.values, event.timestamp);
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            {
                onGyroscopeSensorChanged(event.values, event.timestamp);
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED)
            {
                onGyroscopeSensorUncalibratedChanged(event.values, event.timestamp);
            }
        }



        /**
         * ----------------------------------------------------
         * onAccelerationSensorChanged(float[] acceleration, long timeStamp)
         * ----------------------------------------------------
         *
         * Listen to acceleration sensor updates
         *
         * @return void
         *
         */

        public void onAccelerationSensorChanged(float[] acceleration, long timeStamp)
        {

            // Get raw data from sensor     //
            System.arraycopy(acceleration, 0, this.acceleration, 0, acceleration.length);

            // Apply mean filter to input   //
            this.acceleration = accelerationFilter.filterFloat(this.acceleration);

            // Count the samples we have    //
            accelerationSampleCount++;

            // Only determine the initial orientation after the acceleration sensor
            // and magnetic sensor have had enough time to be smoothed by the mean
            // filters. Also, only do this if the orientation hasn't already been
            // determined since we only need it once.
            if
            (
                accelerationSampleCount > MIN_SAMPLE_COUNT
                && magneticSampleCount  > MIN_SAMPLE_COUNT
                && !hasInitialOrientation
            )
            {
                if(!hasInitialOrientation)
                {
                    calculateOrientation();
                    Log.d("BluetoothClient","CalculatingOrientation");
                }

            }

        }


        /**
         * ----------------------------------------------------
         * onMagneticSensorChanged(float[] magnetic, long timeStamp)
         * ----------------------------------------------------
         *
         * Listen to magnetic sensor updates
         *
         * @return void
         *
         */

        public void onMagneticSensorChanged(float[] magnetic, long timeStamp)
        {

            // Get raw data from sensor     //
            System.arraycopy(magnetic, 0, this.magnetic, 0, magnetic.length);

            // Apply mean filter to input   //
            this.magnetic = magneticFilter.filterFloat(this.magnetic);

            // Count the samples we have    //
            magneticSampleCount++;

        }



        /**
         * ----------------------------------------------------
         * calculateOrientation()
         * ----------------------------------------------------
         *
         * Get device orientation
         *
         * @return void
         *
         */

        private void calculateOrientation()
        {
            hasInitialOrientation = SensorManager.getRotationMatrix(initialRotationMatrix, null, acceleration, magnetic);

            // Remove the sensor observers since they are no longer required.
            if (hasInitialOrientation)
            {
                //sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
                sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
            }

        }



        /**
         * ----------------------------------------------------
         * onGyroscopeSensorChanged(float[] gyroscope, long timestamp)
         * ----------------------------------------------------
         *
         * Manage gyroscope updates
         *
         * @return void
         *
         */

        public void onGyroscopeSensorChanged(float[] gyroscope, long timestamp)
        {
            // Don't start until initial orientation matrix
            if (!hasInitialOrientation)
            {
                return;
            }

            // Initialization of the gyroscope based rotation matrix
            if (!stateInitializedCalibrated)
            {
                currentRotationMatrixCalibrated = matrixMultiplication(currentRotationMatrixCalibrated, initialRotationMatrix);
                stateInitializedCalibrated      = true;
            }

            // This timestep's delta rotation to be multiplied by the current
            // rotation after computing it from the gyro sample data.
            if (timestampOldCalibrated != 0 && stateInitializedCalibrated)
            {
                final float dT = (timestamp - timestampOldCalibrated) * NS2S;

                // Axis of the rotation sample, not normalized yet.
                float axisX = gyroscope[0];
                float axisY = gyroscope[1];
                float axisZ = gyroscope[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                if (omegaMagnitude > EPSILON)
                {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the
                // timestep. We will convert this axis-angle representation of the
                // delta rotation into a quaternion before turning it into the
                // rotation matrix.
                float thetaOverTwo      = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo   = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo   = (float) Math.cos(thetaOverTwo);

                deltaRotationVectorCalibrated[0] = sinThetaOverTwo * axisX;
                deltaRotationVectorCalibrated[1] = sinThetaOverTwo * axisY;
                deltaRotationVectorCalibrated[2] = sinThetaOverTwo * axisZ;
                deltaRotationVectorCalibrated[3] = cosThetaOverTwo;

                SensorManager.getRotationMatrixFromVector( deltaRotationMatrixCalibrated, deltaRotationVectorCalibrated);

                currentRotationMatrixCalibrated  = matrixMultiplication( currentRotationMatrixCalibrated, deltaRotationMatrixCalibrated);

                SensorManager.getOrientation(currentRotationMatrixCalibrated, gyroscopeOrientationCalibrated);

            }

            timestampOldCalibrated = timestamp;

            processOrientation
            (
                (int) Math.toDegrees(gyroscopeOrientationCalibrated[0]),
                (int) Math.toDegrees(gyroscopeOrientationCalibrated[1]),
                (int) Math.toDegrees(gyroscopeOrientationCalibrated[2])
            );

        }


        @Override
        public void onAngularVelocitySensorChanged(float[] angularVelocity, long timeStamp)
        {

        }


        /**
         * ----------------------------------------------------
         * processOrientation(int x, int y, int z)
         * ----------------------------------------------------
         *
         * Process orientation changes
         *
         * @return void
         *
         */

        public void processOrientation(int x, int y, int z)
        {

            int X = (int) currentGyroscopeOrientationCalibrated[0];
            int Y = (int) currentGyroscopeOrientationCalibrated[1];
            int Z = (int) currentGyroscopeOrientationCalibrated[2];

            int Xacc = (int) this.acceleration[0];
            int Yacc = (int) this.acceleration[1];
            int Zacc = (int) this.acceleration[2];



            // set this to true if the new point is different from the current point
            Boolean shouldSave = false;

            // if one is greater than 360 that means that it is the first time
            // so set the current value but don't save it to the file
            if (X > 360)
            {
                currentGyroscopeOrientationCalibrated[0] = x;
                currentGyroscopeOrientationCalibrated[1] = y;
                currentGyroscopeOrientationCalibrated[2] = z;
                return;
            }

            if ((Math.abs(X - x) >= Common.THRESHOLD_ANGLE) || (((360 - (Math.abs(X) - Math.abs(x)))%360) >= Common.THRESHOLD_ANGLE))
            {
                shouldSave = true;
                currentGyroscopeOrientationCalibrated[0] = x;
            }

            if ((Math.abs(Y - y) >= Common.THRESHOLD_ANGLE) || (((360 - (Math.abs(Y) - Math.abs(y)))%360) >= Common.THRESHOLD_ANGLE))
            {
                shouldSave = true;
                currentGyroscopeOrientationCalibrated[1] = y;
            }

            if ((Math.abs(Z - z) >= Common.THRESHOLD_ANGLE) || (((360 - (Math.abs(Z) - Math.abs(z)))%360) >= Common.THRESHOLD_ANGLE))
            {
                shouldSave = true;
                currentGyroscopeOrientationCalibrated[2] = z;
            }


            int Xactive = (Math.abs(acceleration[0])>=7) ? 1 : 0;
            int Yactive = (Math.abs(acceleration[1])>=7) ? 1 : 0;
            int Zactive = (Math.abs(acceleration[2])>=7) ? 1 : 0;


            if( (Xactive + Yactive + Zactive) >= 3 )
            {
                shouldSave = true;
            }
            else
            {
                shouldSave = false;
            }


            // if it is a new gesture then save it to the file
            if( shouldSave )
            {

                // Create new Bluetooth package ============================= //

                JSONObject coordinatesData  = new JSONObject();
                JSONObject orientationData  = new JSONObject();
                JSONObject accelerationData = new JSONObject();

                try
                {
                    orientationData.put("x", x);
                    orientationData.put("y", y);
                    orientationData.put("z", z);
                    accelerationData.put("x", Xacc);
                    accelerationData.put("y", Yacc);
                    accelerationData.put("z", Zacc);

                }
                catch (JSONException e)
                {
                    // Error ?
                }

                try {
                    coordinatesData.put("orientation"   , orientationData);
                    coordinatesData.put("accelerometer" , accelerationData);

                } catch (JSONException e) {
                    // Error ?
                }

                // Create BluetoothMessage ================================= //

                BluetoothMessage newCoordinates = new BluetoothMessage();

                newCoordinates.setType("coordinates");
                newCoordinates.setData(coordinatesData);

                Message coordinatesMsg  = new Message();
                coordinatesMsg.what     = BluetoothClient.SEND_DATA;
                coordinatesMsg.obj      = newCoordinates;


                // log(">> Gyro["+z+", "+y+", "+z+"]");
                log(">> Acce["+Xacc+", "+Yacc+", "+Zacc+"]");


                if(connectionActive)
                {

                    try
                    {
                        bluetoothClient.send(coordinatesMsg);
                    }
                    catch (RemoteException e)
                    {
                        e.printStackTrace();
                    }

                }


            }

        }


        /**
         * ----------------------------------------------------
         * onGyroscopeSensorUncalibratedChanged(float[] gyroscope, long timestamp)
         * ----------------------------------------------------
         *
         * Handle uncalibrated gyroscope changes.
         *
         * @return void
         *
         */

        public void onGyroscopeSensorUncalibratedChanged(float[] gyroscope, long timestamp)
        {
            // don't start until first accelerometer/magnetometer orientation has
            // been acquired
            if (!hasInitialOrientation)
            {
                return;
            }

            // Initialization of the gyroscope based rotation matrix

            if (!stateInitializedRaw)
            {
                currentRotationMatrixRaw    = matrixMultiplication( currentRotationMatrixRaw, initialRotationMatrix);
                stateInitializedRaw         = true;
            }

            // This timestep's delta rotation to be multiplied by the current
            // rotation after computing it from the gyro sample data.
            if (timestampOldRaw != 0 && stateInitializedRaw)
            {
                final float dT = (timestamp - timestampOldRaw) * NS2S;

                // Axis of the rotation sample, not normalized yet.
                float axisX = gyroscope[0];
                float axisY = gyroscope[1];
                float axisZ = gyroscope[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                if (omegaMagnitude > EPSILON)
                {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the
                // timestep. We will convert this axis-angle representation of the
                // delta rotation into a quaternion before turning it into the
                // rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;

                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

                deltaRotationVectorRaw[0] = sinThetaOverTwo * axisX;
                deltaRotationVectorRaw[1] = sinThetaOverTwo * axisY;
                deltaRotationVectorRaw[2] = sinThetaOverTwo * axisZ;
                deltaRotationVectorRaw[3] = cosThetaOverTwo;

                SensorManager.getRotationMatrixFromVector(deltaRotationMatrixRaw, deltaRotationVectorRaw);

                currentRotationMatrixRaw  = matrixMultiplication( currentRotationMatrixRaw, deltaRotationMatrixRaw );

                SensorManager.getOrientation(currentRotationMatrixRaw, gyroscopeOrientationRaw);

            }

            timestampOldRaw = timestamp;

        }


        // Initialization methos ====================================================== //
        // ============================================================================ //

        /**
         * Initialize the mean filters.
         */
        private void initFilters()
        {
            accelerationFilter  = new MeanFilter();
            accelerationFilter.setWindowSize(MEAN_FILTER_WINDOW);

            magneticFilter      = new MeanFilter();
            magneticFilter.setWindowSize(MEAN_FILTER_WINDOW);
        }

        /**
         * Initialize the data structures required for the maths.
         */
        private void initMaths()
        {
            initialRotationMatrix                   = new float[9];
            acceleration                            = new float[3];
            magnetic                                = new float[3];

            deltaRotationVectorCalibrated           = new float[4];
            deltaRotationMatrixCalibrated           = new float[9];
            currentRotationMatrixCalibrated         = new float[9];
            gyroscopeOrientationCalibrated          = new float[3];
            currentGyroscopeOrientationCalibrated   = new float[3];

            // Initialize the current rotation matrix as an identity matrix...
            currentRotationMatrixCalibrated[0]      = 1.0f;
            currentRotationMatrixCalibrated[4]      = 1.0f;
            currentRotationMatrixCalibrated[8]      = 1.0f;

            //Initialize the current rotation angles to 361 which is improbable
            currentGyroscopeOrientationCalibrated[0] = 361;
            currentGyroscopeOrientationCalibrated[1] = 361;
            currentGyroscopeOrientationCalibrated[2] = 361;

            deltaRotationVectorRaw                   = new float[4];
            deltaRotationMatrixRaw                   = new float[9];
            currentRotationMatrixRaw                 = new float[9];
            gyroscopeOrientationRaw                  = new float[3];

            // Initialize the current rotation matrix as an identity matrix...
            currentRotationMatrixRaw[0] = 1.0f;
            currentRotationMatrixRaw[4] = 1.0f;
            currentRotationMatrixRaw[8] = 1.0f;

        }



        /**
         * Initialize the sensors.
         */
        private void initSensors()
        {
            fusedGyroscopeSensor = new FusedGyroscopeSensor();
        }


        /**
         * Restarts all of the sensor observers and resets the activity to the
         * initial state. This should only be called *after* a call to reset().
         */
        private void restart()
        {

            int updateSpeed         = SensorManager.SENSOR_DELAY_FASTEST;

            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) , SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);

            // Do not register for gyroscope updates if we are going to use the
            // fused version of the sensor...
            if (!useFusedEstimation)
            {
                boolean enabled     = sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
            }

            // If we want to use the fused version of the gyroscope sensor.
            if (useFusedEstimation)
            {
                boolean hasGravity  = sensorManager.registerListener( fusedGyroscopeSensor, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);

                // If for some reason the gravity sensor does not exist, fall back
                // onto the acceleration sensor.
                if (!hasGravity)
                {
                    sensorManager.registerListener(fusedGyroscopeSensor, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
                }

                sensorManager.registerListener(fusedGyroscopeSensor, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);

                boolean enabled     = sensorManager.registerListener( fusedGyroscopeSensor, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);

            }
        }

        /**
         * Removes all of the sensor observers and resets the activity to the
         * initial state.
         */
        private void reset()
        {

            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));

            if (!useFusedEstimation)
            {
                sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            }

            if (useFusedEstimation)
            {

                sensorManager.unregisterListener(fusedGyroscopeSensor, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
                sensorManager.unregisterListener(fusedGyroscopeSensor, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
                sensorManager.unregisterListener(fusedGyroscopeSensor, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
                sensorManager.unregisterListener(fusedGyroscopeSensor, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

                fusedGyroscopeSensor.removeObserver(this);

            }

            initMaths();

            hasInitialOrientation       = false;
            stateInitializedCalibrated  = false;
            stateInitializedRaw         = false;

        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {

        }



    }

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

                // TRIGGER_DOWNSET_TRIGGER_STATUS =========================== //

                case SET_TRIGGER_STATUS:

                    triggerDown    = (boolean) message.obj;

                break;



                // SET_CONNECTION_STATUS ======================================= //

                case SET_CONNECTION_STATUS:

                    connectionActive    = (boolean) message.obj;

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
