package models;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import static android.support.v4.content.ContextCompat.getSystemService;

//data needed to place the user in the GVR 3d space
public class User implements SensorEventListener, LocationListener {
    private static final String TAG = "User";
    private float latitude, longitude;
    private SensorManager sm;
    private Location loc;
    private Sensor gyroscope, magnetometer, accelerometer;

    private float[] orientationData = new float[3], ac = new float[3], ma = new float[3], rm = new float[9];
    public User(SensorManager sm) {
        orientationData = new float[3];
        this.sm = sm;
        gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE); //not using rn
        magnetometer = sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Log.i(TAG, "Initialized");
    }


    public void registerListeners() {
        //high accuracy makes it slower
        sm.registerListener(this, accelerometer,SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
        sm.registerListener(this, magnetometer,SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
    }

    public void unregisterListeners() {
        sm.unregisterListener(this, accelerometer);
        sm.unregisterListener(this, magnetometer);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor == magnetometer) {
            ma = sensorEvent.values;
        }

        if (sensorEvent.sensor == accelerometer) {
            ac = sensorEvent.values;
        }
        sm.getRotationMatrix(rm, null, ac, ma);
        orientationData = sm.getOrientation(rm, orientationData);

        adjustDeviceHeading();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * X is across the phone, y is up the length of the phone, and z is coming out of the phone
     */
    private void adjustDeviceHeading() {
        //for when our other sensors get introduced
        //we might be able to guestimate the phone orientation and adjust accordingly
    }

    /**
     * Part of Magnetometer sensor result
     * Will be the most likely candidate for direction we're facing wrt north
     * @return  Azimuth, angle of rotation about the -z axis.
     *          This value represents the angle between the device's
     *          y axis and the magnetic north pole. Returns 0 when facing north.
     *          -pi <= return <= pi
     */
    public float getAzimuth() {
        return orientationData[0];
    }

    /**
     * Part of Magnetometer sensor result
     * @return  Pitch, angle of rotation about the x axis. This value represents
     *          the angle between a plane parallel to the device's screen and a
     *          plane parallel to the ground.
     *          -pi <= return <= pi
     */
    public float getPitch() {
        return orientationData[1];
    }

    /**
     * Part of Magnetometer sensor result
     * @return  Roll, angle of rotation about the y axis. This value represents
     *          the angle between a plane perpendicular to the device's screen
     *          and a plane perpendicular to the ground.
     *          -pi/2 <= return <= pi/2
     */
    public float getRoll() {
        return orientationData[2];
    }

    @Override
    public void onLocationChanged(Location location) {
        loc = location;
        Log.i(TAG, "Location: (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");
        //loc.getBearing will return non-zero when 2 consequent points are far enough
        //apart (so you're moving fast enough in a "direction"
        //TODO: Include loc.getBearing() in the average azimuth if != 0 and bearing accuracy is relatively high
        //TODO: loc.BearingTo() would be good for placing an earcon in the dir. of the destination if the user is too off course
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
