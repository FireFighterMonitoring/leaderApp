package com.jambit.leaderapp;

public class FireFighterData {
    private String ffId;
    private Status status;
    private FireFighterDataTimestamp timestamp;
    private FireFighterDataVitalsigns vitalSigns;

    public String getFfId() {
        return ffId;
    }

    public FireFighterDataTimestamp getTimestamp() {
        return timestamp;
    }

    public Status getStatus() {
        return status;
    }

    public FireFighterDataVitalsigns getVitalSigns() {
        return vitalSigns;
    }

    public enum Status {
        OK,
        NO_DATA,
        DISCONNECTED
    }
}
