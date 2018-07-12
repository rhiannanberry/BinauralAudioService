package com.compaudio.javabinauralservice;

import android.content.Intent;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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

public class MainActivity extends GvrActivity implements View.OnClickListener, GvrView.StereoRenderer {
    private static final String TAG = "BinauralMainActivity";

    private static final float MAX_MODEL_DISTANCE = 7.0f;


    private Button start, stop;
    private GvrAudioEngine ae;
    private ArrayList<Integer> musicSourceId;
    private int currentSong = 0, sourceId = GvrAudioEngine.INVALID_ID;
    private Thread musicLoadingThread;

    private User user;

    protected float[] modelPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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


        modelPosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};

        user = new User((SensorManager)getSystemService(SENSOR_SERVICE));

        //This is the object that does the spatializing
        //In order to actually move the sound, we have to do ae.Update() every "frame" after initializing
        ae = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        ae.setHeadPosition(0,0,0);
        musicSourceId = new ArrayList<>();
        //UI set up
        start = (Button) findViewById(R.id.buttonStart);
        stop = (Button) findViewById(R.id.buttonStop);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
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
        if (view == start) {
            playMusic();
        } else if (view == stop) {
            stopMusic();
        }
    }
    @Override
    public void onPause() {
        ae.pause();
        user.unregisterListeners();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        user.registerListeners();
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
        if (sourceId  != ae.INVALID_ID) {
            ae.update();
            Log.i(TAG, "update, is sound playing? " + ae.isSoundPlaying(sourceId));

        }

        Log.i(TAG, "Testing user update; Azimuth: " + user.getAzimuth());

    }

    /**
     * The following overrides are for implementing stereorenderer
     * @param eye
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
                        ae.preloadSoundFile("music/Visager_-_04_-_Village_of_the_Peeping_Frogs_Loop.mp3");
                        sourceId = ae.createSoundObject("music/Visager_-_04_-_Village_of_the_Peeping_Frogs_Loop.mp3");
                        ae.setSoundObjectPosition(
                                sourceId, modelPosition[0], modelPosition[1], modelPosition[2]);
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
}

