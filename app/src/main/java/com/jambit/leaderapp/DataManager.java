package com.jambit.leaderapp;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
    public static final int MINIMUM_UPDATE_SECONDS = 10;
    public static final int SECONDS_TO_MILLISECONDS_FACTOR = 1000;

    private Date lastDataChangeDate;

    private boolean isConnected = false;

    private HashMap<String, FireFighterData> dataEntries;

    private final OkHttpClient client = new OkHttpClient();
    private final ScheduledExecutorService scheduler;

    public DataManager() {
        //Declare the executer service
        scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Returns a list of FireFighterData sorted by timeout, FireFighterData.Status and CriticalState.
     *
     * @return a list of FireFighterData
     */
    public List<FireFighterData> getDataEntries() {
        List<FireFighterData> list = new ArrayList<>(dataEntries.values());

//        // Sorting
//        Collections.sort(list, new Comparator<FireFighterData>() {
//            @Override
//            public int compare(FireFighterData data2, FireFighterData data1) {
//                return data1.compareTo(data2);
//            }
//        });

        return list;
    }

    /**
     * Starts an publisher which tries to fetch data from backend in a fixed interval. If the fetch
     * fails isConnected will be set to false.
     * On succes the received JSON will be parsed to FireFighterData and modify (create, update, delete)
     * the internal data.
     *
     * @return An Rx Observable on which updates will be sent.
     */
    public Observable<Void> activate() {
        dataEntries = new HashMap<>();

        final PublishSubject<Void> publishSubject = PublishSubject.create();

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
                    Log.d(TAG, "Receive JSON from host: " + request.url());
                    response = client.newCall(request).execute();

                    isConnected = true;

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "REQUEST FAILED!");
                        isConnected = false;
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

                                lastDataChangeDate = new Date();
                                publishSubject.onNext(null);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    isConnected = false;
                    e.printStackTrace();
                }

                Date cleanUpOffsetChange = new Date(new Date().getTime() - MINIMUM_UPDATE_SECONDS * SECONDS_TO_MILLISECONDS_FACTOR);
                if (lastDataChangeDate != null && lastDataChangeDate.before(cleanUpOffsetChange)) {
                    lastDataChangeDate = new Date();
                    publishSubject.onNext(null);
                }
            }
        };

        //Set the schedule function and rate
        scheduler.scheduleAtFixedRate(updateRunnable, 0, UPDATE_INTERVAL, TimeUnit.SECONDS);
        return publishSubject;
    }

    /**
     * Stops the scheduler and cleans data.
     */
    public void deactivate() {
        scheduler.shutdownNow();
        dataEntries.clear();
    }

    /**
     * Calculates vital data contained in FireFighterData to a single CriticalState.
     *
     * @param ffData The input data
     * @return the critical state for FireFighterData
     */
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

    /**
     * Defines critical states.
     */
    enum CriticalState {
        CRITICAL_STATE_DEAD,
        CRITICAL_STATE_CRITICAL,
        CRITICAL_STATE_WARNING,
        CRITICAL_STATE_NOT_CRITICAL
    }

    /**
     * Check if the DataManager has connection to backend.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return isConnected;
    }
}
