package com.compaudio.javabinauralservice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.vr.sdk.audio.GvrAudioEngine;

import models.User;
import models.Vector3;

public class MainActivity extends Activity implements View.OnClickListener{
    private static final String TAG = "BinauralMainActivity";

    private Button toggle, destUpdate, marta, sublime, culc, mcd;
    private ToggleButton continuousMode, alertMode;
    private GvrAudioEngine ae;
    //private ArrayList<Integer> musicSourceId;
    private int currentSong = 0, musicSourceId = GvrAudioEngine.INVALID_ID, alertSourceId = GvrAudioEngine.INVALID_ID;
    private Thread musicLoadingThread, uiThread;

    private TextView userLocation, userDestination, angleToDestination, azimuth, userLocationApp, bearing, destinationDistance, alertActive;
    private EditText latIn, lonIn;

    private User user;

    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkRequestPermissions();

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        user = new User(this, this, lm, 10);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkRequestPermissions();
            return;
        }
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, user);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, user);


        //TODO: Move this to actual spot later


        //This is the object that does the spatializing
        //In order to actually move the sound, we have to do ae.Update() every "frame" after initializing
        ae = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        ae.setHeadPosition(0,0,0);
        ae.setRoomProperties(1000, 1000, 1000, 0,0,0);
        //musicSourceId = new ArrayList<>();
        //UI set up
        destUpdate = (Button) findViewById(R.id.buttonUpdate);
        marta = (Button) findViewById(R.id.buttonMarta);
        culc = (Button) findViewById(R.id.buttonCulc);
        sublime = (Button) findViewById(R.id.buttonSublime);
        toggle = (Button) findViewById(R.id.buttonToggle);
        mcd = (Button) findViewById(R.id.buttonMcd);
        continuousMode = (ToggleButton) findViewById(R.id.continuousModeToggle);
        alertMode = (ToggleButton) findViewById(R.id.alertModeToggle);
        continuousMode.setOnClickListener(this);
        alertMode.setOnClickListener(this);
        mcd.setOnClickListener(this);
        destUpdate.setOnClickListener(this);
        marta.setOnClickListener(this);
        culc.setOnClickListener(this);
        sublime.setOnClickListener(this);
        toggle.setOnClickListener(this);

        alertActive = (TextView) findViewById(R.id.isAlertActive);
        userLocation = (TextView) findViewById(R.id.userLocation);
        userDestination = (TextView) findViewById(R.id.userDestination);
        angleToDestination = (TextView) findViewById(R.id.userAngleToDestination);
        userLocationApp = (TextView) findViewById(R.id.userLocationApp);
        azimuth = (TextView) findViewById(R.id.userAngleToNorth);
        bearing = (TextView) findViewById(R.id.userBearing);
        destinationDistance = (TextView) findViewById(R.id.appDistance);

        latIn = (EditText) findViewById(R.id.latInput);
        lonIn = (EditText) findViewById(R.id.longInput);



        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "start of setup");
                        // Start spatial audio playback of OBJECT_SOUND_FILE at the model position. The
                        // returned musicSourceId handle is stored and allows for repositioning the sound object
                        // whenever the cube position changes.
                        //IMPORTANT NOTE: AUDIO TRACKS HAVE TO BE SINGLE CHANNEL (MONO) OR ELSE THEY WONT WORK!!!!
                        ae.preloadSoundFile("music/roots_loop.wav");
                        ae.preloadSoundFile("alerts/chime.wav");
                        ae.preloadSoundFile("alerts/success.wav");


                        musicSourceId = ae.createSoundObject("music/roots_loop.wav");
                        alertSourceId = ae.createSoundObject("alerts/chime.wav");
                        ae.setSoundObjectPosition(alertSourceId, 0, 0, 0);
                        ae.setSoundObjectPosition( //stationary, only the user moves
                                musicSourceId, 0,0,0);
                        ae.pause();

                        AlertManager.getInstance().setSourceAndEngine(alertSourceId, ae);

                        ae.playSound(musicSourceId, true);

                        Log.i(TAG, "End of setup");
                    }
                })
                .start();
        uiThread = uiFrameThread();

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
            if (latIn.getText().toString().length() == 0 || lonIn.getText().toString().length() == 0) {
                return;
            }
            double lat = Double.valueOf(latIn.getText().toString());
            double lon = Double.valueOf(lonIn.getText().toString());
            user.setDestination(new Vector3(lat, lon)); //CULC
            AlertManager.getInstance().reset(user.realDistance);

        } else if (view == marta) {
            latIn.setText("33.7811125");
            lonIn.setText("-84.3864757");
        } else if (view == culc) {
            //33.774675, -84.396376
            latIn.setText("33.774675");
            lonIn.setText("-84.396376");
        } else if (view == sublime) {
            latIn.setText("33.781878");
            lonIn.setText("-84.404919");
        }else if (view == mcd ){
            latIn.setText("33.785088");
            lonIn.setText("-84.406588");

        } else if (view == toggle) {
            if (isPlaying) {
                stopMusic();
                toggle.setText("Start");
            } else {
                playMusic();
                toggle.setText("Stop");

            }
            Log.i(TAG, "in toggle");
        } else if (view == continuousMode) {
            if (continuousMode.isChecked()) {
                Log.i(TAG, "Continuous mode on");
                ae.resumeSound(musicSourceId);
            } else {
                Log.i(TAG, "Continuous mode off");

                ae.pauseSound(musicSourceId);
            }

        } else if (view == alertMode) {
            if (alertMode.isChecked()) {
                Log.i(TAG, "Alert mode on");
                AlertManager.getInstance().enableAlerts();


            } else {
                Log.i(TAG, "Alert mode off");
                AlertManager.getInstance().disableAlerts();

            }
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
        isPlaying = true;
        ae.resume();
        if (musicSourceId != GvrAudioEngine.INVALID_ID && continuousMode.isChecked()) {
            ae.playSound(musicSourceId, true);
        }
        Log.i(TAG, "Playing music");

    }

    private void stopMusic() {
        Log.i(TAG, "Stopping music");
        isPlaying = false;
        ae.pause();
    }

    public Thread uiFrameThread() {
        return new Thread() {
            @Override
            public void run() {
                try {
                    while (!uiThread.isInterrupted()) {
                        Thread.sleep(100);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateAudioSimulation();
                                updateUI();
                                float offAngle = (user.compass.getBearingDegrees() - user.compass.getAzimuth());
                                offAngle = (offAngle < -180) ? offAngle+360 : offAngle;
                                offAngle = (offAngle > 180) ? offAngle-360 : offAngle;
                                alertSourceId = AlertManager.getInstance().checkAlerts(offAngle, user.realDistance);
                                //checkAlerts
                                //off angle, distance,

                                // update TextView here!
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
    }

    private void updateAudioSimulation() {
        //update cameraPosition
        Vector3 pos = user.getPosition();
        float[] q = user.getQuaternion();
        ae.setHeadPosition((float)pos.x, (float)pos.y, -(float)pos.z);
        //head rotation is easier bc we're just changing one axis rn (yaw)
        ae.setHeadRotation(q[0], q[1], q[2], q[3]);

        if (musicSourceId  != ae.INVALID_ID) {
            ae.update();
        }
    }

    private void updateUI() {
        //180 off of current heading
        float offAngle = (user.compass.getBearingDegrees() - user.compass.getAzimuth());
        offAngle = (offAngle < -180) ? offAngle+360 : offAngle;
        offAngle = (offAngle > 180) ? offAngle-360 : offAngle;
        //this should be -180 < angle < 180
        //- means shortest turn is left, + means shortest turn is right
        userLocation.setText(user.getWorldPosition().toString());
        userDestination.setText(user.getDestinationVec().toString());
        destinationDistance.setText(Float.toString((float)user.getPosition().magnitude()));
        angleToDestination.setText(Float.toString(offAngle));
        userLocationApp.setText(user.getPosition().toString());
        azimuth.setText(Float.toString(user.compass.getAzimuth()));
        bearing.setText(Float.toString(user.compass.getBearingDegrees()));
        if (AlertManager.getInstance().alertCurrentlyActive) {
            alertActive.setText("Active");
        } else {
            alertActive.setText("Inactive");
        }
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

