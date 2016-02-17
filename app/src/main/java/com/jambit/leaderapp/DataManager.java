package com.jambit.leaderapp;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Sebastian Stallenberger - jambit GmbH
 */
public class DataManager {

    private static final String TAG = "DataManager";
    private HashMap<Integer, FireFighterData> dataEntries;

    private static final String BASE_URL = "http://192.168.232.112:8080/api/v1";
    private static final String REST_PATH_DATA = "/data";

    /**
     * HTTP client
     */
    private final OkHttpClient client = new OkHttpClient();

    public DataManager() {

        final Request request = new Request.Builder()
                .url(BASE_URL + REST_PATH_DATA)
                .get()
                .build();

        //Declare the timer
        Timer t = new Timer();
        //Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {

                                  @Override
                                  public void run() {
                                      Response response = null;

                                      try {
                                          Log.d(TAG, "GETting json from Host: " + BASE_URL);
                                          response = client.newCall(request).execute();

                                          if (!response.isSuccessful()) {
                                              Log.e(TAG, "REQUEST FAILED!");
                                          } else {
                                              try {
                                                  String jsonString = response.body().string();
                                                  Log.d(TAG, jsonString);

                                                  Gson gson = new Gson();
                                                  FireFighterData[] ffDataSets = gson.fromJson(jsonString, FireFighterData[].class);

                                                  if (ffDataSets == null) {
                                                      Log.e(TAG, "Nothing to parse...");
                                                      return;
                                                  }

                                                  for (FireFighterData data : ffDataSets) {
                                                      dataEntries.put(data.getId(), data);
                                                  }
                                              } catch (IOException e) {
                                                  e.printStackTrace();
                                              }
                                          }
                                      } catch (IOException e) {
                                          e.printStackTrace();
                                      }
                                  }
                              },
                //Set how long before to start calling the TimerTask (in milliseconds)
                0,
                //Set the amount of time between each execution (in milliseconds)
                10000);

//        // Sorting
//        Collections.sort(dataEntries, new Comparator<FFData>() {
//            @Override
//            public int compare(FFData data2, FFData data1) {
//
//                return data1.getHeartRate().compareTo(data2.getHeartRate());
//            }
//        });
    }

    public List<FireFighterData> getDataEntries() {
        List<FireFighterData> list = new ArrayList<>(dataEntries.values());
        return list;
    }

    public void activate() {
        dataEntries = new HashMap<>();
    }

    public void deactivate() {
        dataEntries.clear();

    }

    public CriticalState criticalState(FireFighterData ffData) {
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
