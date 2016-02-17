package com.jambit.leaderapp;

/**
 * A data object for fire fighter data vital signs. Used to parse JSON.
 */
@SuppressWarnings("unused")
public class FireFighterDataVitalsigns {
    private int heartRate;
    private int stepCount;

    public int getHeartRate() {
        return heartRate;
    }

    public int getStepCount() {
        return stepCount;
    }
}
