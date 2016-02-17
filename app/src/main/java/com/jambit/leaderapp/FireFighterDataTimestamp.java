package com.jambit.leaderapp;

/**
 * A data object for fire fighter data timestamp. Used to parse JSON.
 */
@SuppressWarnings("unused")
public class FireFighterDataTimestamp {
    private long nano;
    private long epochSecond;

    public long getNano() {
        return nano;
    }

    public long getEpochSecond() {
        return epochSecond;
    }
}
