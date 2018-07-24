package com.compaudio.javabinauralservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.GvrViewerParams;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;

import models.User;
import models.Vector3;

public class MainActivity extends GvrActivity implements View.OnClickListener, GvrView.StereoRenderer {
    private static final String TAG = "BinauralMainActivity";

    private Button start, stop, destUpdate, marta, sublime, culc;
    private GvrAudioEngine ae;
    private ArrayList<Integer> musicSourceId;
    private int currentSong = 0, sourceId = GvrAudioEngine.INVALID_ID;
    private Thread musicLoadingThread, uiThread;

    private TextView userLocation, userDestination, angleToDestination, azimuth, userLocationApp, bearing;
    private EditText latIn, lonIn;

    private User user;

    protected float[] modelPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkRequestPermissions();
        GvrView gv = (GvrView) findViewById(R.id.gvr_view);

        gv.setEGLConfigChooser(8, 8, 8, 8, 16, 8);
        gv.setRenderer(this);
        gv.setStereoModeEnabled(false);
        gv.setTransitionViewEnabled(false);
        gv.enableCardboardTriggerEmulation();

        if (gv.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }
        setGvrView(gv);

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        user = new User(this, this, lm, 10);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkRequestPermissions();
            return;
        }
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, user);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, user);
        //lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, user);


        //TODO: Move this to actual spot later


        //This is the object that does the spatializing
        //In order to actually move the sound, we have to do ae.Update() every "frame" after initializing
        ae = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        ae.setHeadPosition(0,0,0);
        ae.setRoomProperties(1000, 1000, 1000, 0,0,0);
        musicSourceId = new ArrayList<>();
        //UI set up
        destUpdate = (Button) findViewById(R.id.buttonUpdate);
        marta = (Button) findViewById(R.id.buttonMarta);
        culc = (Button) findViewById(R.id.buttonCulc);
        sublime = (Button) findViewById(R.id.buttonSublime);
        start = (Button) findViewById(R.id.buttonStart);
        stop = (Button) findViewById(R.id.buttonStop);
        destUpdate.setOnClickListener(this);
        marta.setOnClickListener(this);
        culc.setOnClickListener(this);
        sublime.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);

        userLocation = (TextView) findViewById(R.id.userLocation);
        userDestination = (TextView) findViewById(R.id.userDestination);
        angleToDestination = (TextView) findViewById(R.id.userAngleToDestination);
        userLocationApp = (TextView) findViewById(R.id.userLocationApp);
        azimuth = (TextView) findViewById(R.id.userAngleToNorth);
        bearing = (TextView) findViewById(R.id.userBearing);

        latIn = (EditText) findViewById(R.id.latInput);
        lonIn = (EditText) findViewById(R.id.longInput);

        uiThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!uiThread.isInterrupted()) {
                        Thread.sleep(100);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                       userLocation.setText("Current Location: " + user.getXy().toString());
                                    userDestination.setText("Destination:      " + user.getDestinationVec().toString());
                                 angleToDestination.setText("Bearing azimuth: " + user.compass.getBearingAzimuth());
                                    userLocationApp.setText("App position:    " + user.getPosition().toString());
                                            azimuth.setText("Angle to North:  " + user.compass.getAzimuth());
                                            bearing.setText("Bearing:               " + user.compass.getBearingDegrees());
                                // update TextView here!
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        uiThread.start();
    }

    private static void checkGLError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == destUpdate) {
            double lat = Double.valueOf(latIn.getText().toString());
            double lon = Double.valueOf(lonIn.getText().toString());
            user.setDestination(new Vector3(lat, lon)); //CULC

        } else if (view == marta) {
            latIn.setText("33.7811125");
            lonIn.setText("-84.3864757");
        } else if (view == culc) {
            latIn.setText("33.774719");
            lonIn.setText("-84.396287");
        } else if (view == sublime) {
            latIn.setText("33.781878");
            lonIn.setText("-84.404919");
        } else if (view == start) {
            playMusic();
        } else if (view == stop) {
            stopMusic();
        }
    }
    @Override
    public void onPause() {
        ae.pause();
        user.compass.stopBearing();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        user.compass.startBearing();
        ae.resume();
    }

    private void loadSounds() {
        //for if/when we want to set up earcons or audio icons
    }

    private void playMusic() {
        ae.resume();
        if (sourceId != GvrAudioEngine.INVALID_ID) {
            ae.playSound(sourceId, true);
        }
        Log.i(TAG, "Playing music");

    }

    private void stopMusic() {
        Log.i(TAG, "Stopping music");
        ae.pause();
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        //update cameraPosition
        Vector3 pos = user.getPosition();
        float[] q = user.getQuaternion();
        ae.setHeadPosition((float)pos.x, (float)pos.y, -(float)pos.z);
        //head rotation is easier bc we're just changing one axis rn (yaw)
        ae.setHeadRotation(q[0], q[1], q[2], q[3]);

        if (sourceId  != ae.INVALID_ID) {
            ae.update();
        }


    }

    /**
     * The following overrides are for implementing stereorenderer
     */

    @Override
    public void onDrawEye(Eye eye) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {


    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        //loadMusic();
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "start of setup");
                        // Start spatial audio playback of OBJECT_SOUND_FILE at the model position. The
                        // returned sourceId handle is stored and allows for repositioning the sound object
                        // whenever the cube position changes.
                        //IMPORTANT NOTE: AUDIO TRACKS HAVE TO BE SINGLE CHANNEL (MONO) OR ELSE THEY WONT WORK!!!!
                        ae.preloadSoundFile("music/roots_loop.wav");
                        sourceId = ae.createSoundObject("music/roots_loop.wav");
                        ae.setSoundObjectPosition( //stationary, only the user moves
                                sourceId, 0,0,0);
                        ae.pause();

                        ae.playSound(sourceId, true /* looped playback */);

                        Log.i(TAG, "End of setup");
                    }
                })
                .start();


        checkGLError("onSurfaceCreated");

    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    /**
     * TODO: Clean up this awesome copy/paste job for checking and requesting permissions
     */
    public void checkRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            // Permission is not granted
        } else {
            //permission granted
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.INTERNET)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET},
                        2);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            // Permission is not granted
        } else {
            //permission granted
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case 2: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
}

