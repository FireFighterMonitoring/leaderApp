package com.jambit.leaderapp;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * The data manager for handling, fetching and managing fire fighter update data.
 */
public class DataManager {

    private static final String TAG = "DataManager";
    private static final int UPDATE_INTERVAL = 2; // in seconds
    private static final String BASE_URL = "http://192.168.232.112:8080/api/v1";
    private static final String REST_PATH_DATA = "/data";

    private boolean isConnected = false;

    private HashMap<String, FireFighterData> dataEntries;

    private final OkHttpClient client = new OkHttpClient();
    private final ScheduledExecutorService scheduler;

    public DataManager() {
        //Declare the executer service
        scheduler = Executors.newScheduledThreadPool(1);
    }

    public List<FireFighterData> getDataEntries() {
        List<FireFighterData> list = new ArrayList<>(dataEntries.values());

//        // Sorting
//        Collections.sort(dataEntries, new Comparator<FFData>() {
//            @Override
//            public int compare(FFData data2, FFData data1) {
//
//                return data1.getHeartRate().compareTo(data2.getHeartRate());
//            }
//        });

        return list;
    }

    public Observable<Void> activate() {
        dataEntries = new HashMap<>();

        final PublishSubject<Void> stringPublishSubject = PublishSubject.create();

        final Request request = new Request.Builder()
                .url(BASE_URL + REST_PATH_DATA)
                .get()
                .build();

        // Update runnable
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                Response response;

                try {
                    Log.d(TAG, "GETting json from Host: " + BASE_URL);
                    response = client.newCall(request).execute();

                    isConnected = true;

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
                                if (data.getStatus() == FireFighterData.Status.DISCONNECTED) {
                                    dataEntries.remove(data.getFfId());
                                } else {
                                    dataEntries.put(data.getFfId(), data);
                                }

                                stringPublishSubject.onNext(null);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    isConnected = false;
                    e.printStackTrace();
                }
            }
        };

        //Set the schedule function and rate
        scheduler.scheduleAtFixedRate(updateRunnable, 0, UPDATE_INTERVAL, TimeUnit.SECONDS);
        return stringPublishSubject;
    }

    public void deactivate() {
        scheduler.shutdownNow();
        dataEntries.clear();

    }

    public CriticalState criticalState(FireFighterData ffData) {
        if (ffData.getVitalSigns().getHeartRate() == 0) {
            return CriticalState.CRITICAL_STATE_DEAD;
        }

        if (ffData.getVitalSigns().getHeartRate() < 50) {
            return CriticalState.CRITICAL_STATE_CRITICAL;
        }

        if (ffData.getVitalSigns().getHeartRate() > 200) {
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

    public boolean isConnected() {
        return isConnected;
    }
}
