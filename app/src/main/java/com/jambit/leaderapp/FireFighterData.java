package com.jambit.leaderapp;

public class FireFighterData {
    private String ffId;
    private int heartRate;
    private int stepCount;
    private FireFighterDataTimestamp timestamp;

    public String getFfId() {
        return ffId;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public int getStepCount() {
        return stepCount;
    }

    public FireFighterDataTimestamp getTimestamp() {
        return timestamp;
    }
}
