package com.jambit.leaderapp;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Sebastian Stallenberger - jambit GmbH
 */
public class DataManager {

    private ArrayList<FFData> dataEntries;

    public DataManager() {
        //Declare the timer
        Timer t = new Timer();
        //Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {

                                  @Override
                                  public void run() {
                                      Random r = new Random();
                                      int low = 70;
                                      int high = 190;
                                      for (FFData dataEntry : dataEntries) {
                                          int result = r.nextInt(high - low) + low;
                                          dataEntry.setHeartRate(result);
                                      }
                                  }
                              },
                //Set how long before to start calling the TimerTask (in milliseconds)
                0,
                //Set the amount of time between each execution (in milliseconds)
                1000);

//        // Sorting
//        Collections.sort(dataEntries, new Comparator<FFData>() {
//            @Override
//            public int compare(FFData data2, FFData data1) {
//
//                return data1.getHeartRate().compareTo(data2.getHeartRate());
//            }
//        });
    }

    public ArrayList<FFData> getDataEntries() {
        return dataEntries;
    }

    public void activate() {
        dataEntries = new ArrayList<>();

        FFData data = new FFData();
        data.setFfId("Tobi");
        dataEntries.add(data);
        data = new FFData();
        data.setFfId("Rafał");
        dataEntries.add(data);
        data = new FFData();
        data.setFfId("Stalli");
        dataEntries.add(data);
    }

    public void deactivate() {
        dataEntries.clear();

    }

    public CriticalState criticalState(FFData ffData) {
        if (ffData.getHeartRate() == 0) {
            return CriticalState.CRITICAL_STATE_DEAD;
        }

        if (ffData.getHeartRate() < 90) {
            return CriticalState.CRITICAL_STATE_CRITICAL;
        }

        if (ffData.getHeartRate() > 150) {
            return CriticalState.CRITICAL_STATE_WARNING;
        }

        return CriticalState.CRITICAL_STATE_NOT_CRITICAL;
    }

    enum CriticalState {
        CRITICAL_STATE_DEAD,
        CRITICAL_STATE_CRITICAL,
        CRITICAL_STATE_WARNING,
        CRITICAL_STATE_NOT_CRITICAL
    }
}
