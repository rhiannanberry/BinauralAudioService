package models;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.math.BigDecimal;

import static android.support.v4.content.ContextCompat.getSystemService;

//data needed to place the user in the GVR 3d space
public class User implements LocationListener {
    private static final String TAG = "User";

    private Activity activity;
    private Context ctx;

    private LocationManager lm;
    private Location loc, destination;
    public Compass compass;

    Vector3 xy = new Vector3(), position = new Vector3(), destinationVec = new Vector3();

    private double startDistance = 10.0;
    private double adjustedScale = -1;
    boolean locationSet = false, destinationSet = false, scaleSet = false;

    public User(Activity fa, Context ctx, LocationManager lm,  double startDistance) {
        activity = fa;
        this.ctx = ctx;
        this.startDistance = startDistance;
        compass = new Compass(activity);
        this.lm = lm;
        Log.i(TAG, "Initialized");
    }

    /**
     * X is across the phone, y is up the length of the phone, and z is coming out of the phone
     */
    private void adjustDeviceHeading() {
        //for when our other sensors get introduced
        //we might be able to guestimate the phone orientation and adjust accordingly
    }

    public void setDestination(Location destination) {
        destinationSet = true;
        this.destination = destination;
        destinationVec = new Vector3(destination.getLatitude(), destination.getLongitude());
        if (locationSet) {
            xy = new Vector3(loc.getLatitude(), loc.getLongitude());
            //Log.d(TAG, "real location: (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");

            Log.d(TAG, "location: " + xy.toString());
            if (!destinationSet)
                return;
            float bearing = loc.bearingTo(destination);
            Log.i(TAG, "Bearing: " + bearing);
            float distance = loc.distanceTo(destination);
            Log.d(TAG, "Location bearing: " + bearing);

            compass.setBearingDegrees(bearing);
            Log.d(TAG, "Calculated azimuth: " + compass.getAzimuth());
            Log.d(TAG, "Location difference: " + xy.subtract(destinationVec));
            scaleSet = false;
            appSpacePosition(distance, xy.subtract(destinationVec));
        }
    }

    public void setDestination(Vector3 vec) {
        destinationSet = true;
        this.destinationVec = vec; //in real world coord
        destination = new Location("");
        destination.setLatitude(vec.x);
        destination.setLongitude(vec.y);
        if (locationSet) {
            xy = new Vector3(loc.getLatitude(), loc.getLongitude());
            //Log.d(TAG, "real location: (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");

            Log.d(TAG, "location: " + xy.toString());
            if (!destinationSet)
                return;
            float bearing = loc.bearingTo(destination);
            Log.i(TAG, "Bearing: " + bearing);
            float distance = loc.distanceTo(destination);
            Log.d(TAG, "Location bearing: " + bearing);

            compass.setBearingDegrees(bearing);
            Log.d(TAG, "Calculated azimuth: " + compass.getAzimuth());
            Log.d(TAG, "Location difference: " + xy.subtract(destinationVec));
            scaleSet = false;
            appSpacePosition(distance, xy.subtract(destinationVec));
        }
    }

    public Vector3 getDestinationVec() {
        return destinationVec;
    }

    public Vector3 getXy() {
        return xy;
    }

    public Vector3 getPosition() {
        return position;
    }

    public float[] getQuaternion() {
        //360- because we flipped this for a right hand coordinated space
        Vector3 euler = new Vector3(0, ((-compass.getBearingAzimuth()) * Math.PI / 180), 0);
        return euler.toQuaternion();
    }

    @Override
    public void onLocationChanged(Location location) {

        locationSet = true;
        loc = location;
        location.setAltitude(0);

        xy = new Vector3(loc.getLatitude(), loc.getLongitude());
        //Log.d(TAG, "real location: (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");

        Log.d(TAG, "location: " + xy.toString());
        if (!destinationSet)
            return;
        float bearing = location.bearingTo(destination);
        Log.i(TAG, "Bearing: " + bearing);
        float distance = location.distanceTo(destination);
        Log.d(TAG, "Location bearing: " + bearing);

        compass.setBearingDegrees(bearing);
        Log.d(TAG, "Calculated azimuth: " + compass.getAzimuth());
        Log.d(TAG, "Location difference: " + xy.subtract(destinationVec));
        appSpacePosition(distance, xy.subtract(destinationVec));

        //Log.i(TAG, "Location: (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");
        //loc.getBearing will return non-zero when 2 consequent points are far enough
        //apart (so you're moving fast enough in a "direction"
        //TODO: Include loc.getBearing() in the average azimuth if != 0 and bearing accuracy is relatively high
        //TODO: loc.BearingTo() would be good for placing an earcon in the dir. of the destination if the user is too off course
    }

    private void appSpacePosition(double distance, Vector3 currentPath) {
        if (!scaleSet && destinationSet && locationSet) {
            //startDistance = adjustedScale * distance;
            adjustedScale = startDistance / distance;
            Log.i(TAG, "Adjusted Scale Set: " + adjustedScale);
            scaleSet = true;
        }

        Log.d(TAG, "updated current path check: " + currentPath.toString());

        position = (currentPath.direction()).scalarMultiply(distance * adjustedScale);
        Log.i(TAG, "Adjusted position: " + position.toString());
        Log.i(TAG, "Azimuth: " + compass.getAzimuth());
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d(TAG, "TEST");
    }

    @Override
    public void onProviderEnabled(String s) {

        Log.d(TAG, "TEST11");

    }

    @Override
    public void onProviderDisabled(String s) {
        //lm.removeUpdates(this);
        Log.d(TAG, "TESTwwww");

    }
}
