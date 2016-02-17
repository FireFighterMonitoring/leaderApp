package com.jambit.leaderapp;

public class FireFighterData {
    private int id;
    private String ffId;
    private int heartRate;
    private FireFighterDataTimestamp timestamp;

    public int getId() {
        return id;
    }

    public String getFfId() {
        return ffId;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public FireFighterDataTimestamp getTimestamp() {
        return timestamp;
    }
}
