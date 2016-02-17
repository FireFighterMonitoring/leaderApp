package com.jambit.leaderapp;

/**
 * @author Sebastian Stallenberger - jambit GmbH
 */
public class FFData {
    private String ffId = "[NONAME]";
    private Long lastUpdate = 0l;
    private Integer heartRate = 0;

    public String getFfId() {
        return ffId;
    }

    public void setFfId(String ffId) {
        this.ffId = ffId;
    }

    public Long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }
}
