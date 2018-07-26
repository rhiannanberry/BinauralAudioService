package com.compaudio.javabinauralservice;

import android.util.Log;

import com.google.vr.sdk.audio.GvrAudioEngine;

import java.util.Timer;
import java.util.TimerTask;

public class AlertManager {
    private static final String TAG = "Alerts";

    private static final AlertManager ourInstance = new AlertManager();
    //these use real distances
    private double originalDistance;
    private double shortestDistance;
    private double threshDistance;
    private double endTime, timeRemaining, timeTimer;
    private boolean distTripped, angleTripped, alertActive,  alertsEnabled;
    public boolean alertCurrentlyActive;
    private int sourceId;
    GvrAudioEngine ae;
    private Timer timer;
    private TimerTask task;
    public static AlertManager getInstance() {
        return ourInstance;
    }

    private AlertManager() {
        distTripped = false;
        angleTripped = false;
        threshDistance = 3; //meters
    }

    public void setSourceAndEngine(int sourceId, GvrAudioEngine ae) {
        this.ae = ae;
        this.sourceId = sourceId;
    }

    public void reset(double originalDistance) {
        distTripped = false;
        angleTripped = false;
        this.originalDistance = originalDistance;
        shortestDistance = originalDistance;
    }

    public int checkAlerts(double degreesOff, double newDistance) {
        if (alertsEnabled) {

            timeRemaining = endTime - System.currentTimeMillis();
            Log.i(TAG, "is active? " + alertCurrentlyActive);
            if (alertCurrentlyActive) {
                checkToReleaseAlerts(degreesOff, newDistance);

            } else {
                angleTripped = (Math.abs(degreesOff) > 60);
                if (newDistance <= shortestDistance) {
                    shortestDistance = newDistance;
                } else if ((newDistance - shortestDistance) > threshDistance) {
                    distTripped = true;
                } else {
                    distTripped = false;
                }
                //&& distTripped
                Log.i(TAG, "dist Remaining: " + (newDistance-shortestDistance));
                Log.i(TAG, "time Remaining: " + (timeRemaining));

                if (angleTripped && distTripped && (timeRemaining <= 0)) {
                    activateAlert();
                }
            }
            Log.i(TAG, "distTripped " + distTripped + ", angleTripped " + angleTripped );
        }
        return sourceId;
    }

    private void checkToReleaseAlerts(double degreesOff, double newDistance) {
        //not using newDistance for now
        Log.i(TAG, "Checking");

        if (Math.abs(degreesOff) <= 40) {
            shortestDistance = newDistance;
            alertCurrentlyActive = false;
            angleTripped = false;
            endTime = System.currentTimeMillis()+5*1000; // 5 second hold
            int success = ae.createSoundObject("alerts/success.wav");
            ae.playSound(success, false);
//            task.cancel();
            Log.i(TAG, "Released");
        } else {
            double time = System.currentTimeMillis();
            Log.i(TAG, "timer: " + timeTimer + " sys time: " + time);
            ae.setSoundVolume(sourceId, (float)(1.3 - (Math.abs(degreesOff))/180));


            if (timeTimer <= time) {
                Log.i(TAG, "PLAYING ALERT");
                ae.playSound(sourceId, false);
                timeTimer = System.currentTimeMillis() + 5000;
                sourceId = ae.createSoundObject("alerts/chime.wav");
            }
        }
    }

    private void activateAlert() {
        /*timer = new Timer();
        //Set the schedule function
        Log.i(TAG, "Alert Activated");

        task = new TimerTask() {

            @Override
            public void run() {
                ae.playSound(sourceId, false);
            }
        };
        timer.scheduleAtFixedRate(task,
                0, 5000);*/

        alertCurrentlyActive = true;

    }

    public void disableAlerts() {
        alertsEnabled = false;

        Log.i(TAG, "Alert Cancelled");

    }

    public void enableAlerts() {
        alertsEnabled = true;
    }


}
