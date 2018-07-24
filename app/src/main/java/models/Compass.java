package models;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class Compass implements SensorEventListener {
    private static final String TAG = "Compass";

    private SensorManager sensorManager;
    private Sensor gsensor;
    private Sensor msensor;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth = 0f;
    private float bearingAzimuth = 0f;

    private boolean bearing = false;
    private float bearingDegrees = -1;

    Activity activity;

    public Compass(Activity activity) {

        sensorManager = (SensorManager) activity.getApplicationContext()
                .getSystemService(Context.SENSOR_SERVICE);
        gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void start() {

        boolean deviceSensorCompatible = true;

        if(!sensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_FASTEST))
            deviceSensorCompatible = false;

        if(!sensorManager.registerListener(this, msensor, SensorManager.SENSOR_DELAY_FASTEST))
            deviceSensorCompatible = false;

    }

    public void startBearing()
    {
        bearing = true;
        start();
    }

    public void setBearingDegrees(float bearingDegrees)
    {
        this.bearingDegrees = bearingDegrees;
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void stopBearing()
    {
        bearing = false;
        stop();
    }

    public float getAzimuth() {
        return azimuth%360;
    }

    public float getBearingAzimuth() {
        return bearingAzimuth%360;
    }

    public float getBearingDegrees() {
        return (360+bearingDegrees)%360;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;

        synchronized (this) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                        * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                        * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                        * sensorEvent.values[2];

                // mGravity = sensorEvent.values;

                // Log.e(TAG, Float.toString(mGravity[0]));
            }

            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // mGeomagnetic = sensorEvent.values;

                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                        * sensorEvent.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                        * sensorEvent.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                        * sensorEvent.values[2];
                // Log.e(TAG, Float.toString(sensorEvent.values[0]));

            }

            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
                    mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                Log.d(TAG, "Bearing degrees: " + bearingDegrees);
                azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                azimuth = (azimuth + 360) % 360;
                Log.d(TAG, "azimuth (rad): " + azimuth);


                if (bearing) {
                    if (bearingDegrees != -1) {
                        bearingAzimuth = azimuth - bearingDegrees;
                        Log.d(TAG, "azimuth with bearing (rad): " + bearingAzimuth);

                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
