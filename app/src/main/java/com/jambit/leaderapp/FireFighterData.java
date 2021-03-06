package com.jambit.leaderapp;

/**
 * A data object for fire fighter update data. Used to parse JSON.
 */
@SuppressWarnings("unused")
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
        CONNECTED,
        OK,
        NO_DATA,
        DISCONNECTED
    }
}
