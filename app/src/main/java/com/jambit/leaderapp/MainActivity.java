package com.jambit.leaderapp;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Date;
import java.util.Locale;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int TIMEOUT_SECONDS = 30;
    private RecyclerView.Adapter mAdapter;

    private DataManager dataManager;
    private ImageView disconnectedIcon;
    private Observable<Void> updateObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize data manager
        dataManager = new DataManager();
        updateObservable = dataManager.activate();

        // Configure recycler view
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.ff_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new FFAdapter();
        mRecyclerView.setAdapter(mAdapter);

        disconnectedIcon = (ImageView) findViewById(R.id.disconnected_image_view);

        updateObservable
                // Be notified on the main thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted()");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError()", e);
                    }

                    @Override
                    public void onNext(Void aVoid) {
                        Log.d(TAG, "onNext()");

                        mAdapter.notifyDataSetChanged();

                        if (dataManager.isConnected()) {
                            disconnectedIcon.setVisibility(View.GONE);
                        } else {
                            disconnectedIcon.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dataManager.deactivate();
    }

    private static class FFDataHolder extends RecyclerView.ViewHolder {
        public RelativeLayout containerLayout;
        public TextView nameTextView;
        public TextView heartRateView;
        public TextView stepCountView;
        public ImageView statusIconView;
        public TextView lastUpdateTextView;

        public FFDataHolder(View itemView) {
            super(itemView);
            containerLayout = (RelativeLayout) itemView
                    .findViewById(R.id.firefighter_container_layout);

            nameTextView = (TextView) itemView
                    .findViewById(R.id.name_text_view);

            heartRateView = (TextView) itemView
                    .findViewById(R.id.heart_text_view);

            stepCountView = (TextView) itemView
                    .findViewById(R.id.steps_text_view);

            lastUpdateTextView = (TextView) itemView
                    .findViewById(R.id.lastupdate_text_view);

            statusIconView = (ImageView) itemView
                    .findViewById(R.id.statusImageView);
        }
    }

    private class FFAdapter extends RecyclerView.Adapter<FFDataHolder> {

        public static final String TAG = "FFAdapter";

        @Override
        public FFDataHolder onCreateViewHolder(ViewGroup parent, int pos) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_ffdata, parent, false);
            return new FFDataHolder(view);
        }

        @Override
        public void onBindViewHolder(FFDataHolder holder, int pos) {
            FireFighterData ffData = dataManager.getDataEntries().get(pos);

            holder.nameTextView.setText(ffData.getFfId());

            Long epochMilliSeconds = ffData.getTimestamp().getEpochSecond() * 1000;
            Date lastUpDate = new Date(epochMilliSeconds);
            long lastUpdateSecondsAgo = (new Date().getTime() - lastUpDate.getTime()) / 1000;
            holder.lastUpdateTextView.setText(String.format(Locale.GERMAN, "last update: %d seconds ago", lastUpdateSecondsAgo));

            boolean timedOut = new Date().getTime() - TIMEOUT_SECONDS * 1000 > epochMilliSeconds;
            if (timedOut) {
                // timeout
                holder.containerLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.status_danger));

            } else {
                holder.containerLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
            }

            if (ffData.getStatus() != null) {
                switch (ffData.getStatus()) {
                    case OK: {
                        if (ffData.getVitalSigns() == null) {
                            Log.e(TAG, "Didn't find vitaldata. Can't process update.");
                            return;
                        }

                        holder.heartRateView.setText(String.format(Locale.GERMAN, "%d bpm", ffData.getVitalSigns().getHeartRate()));
                        holder.stepCountView.setText(String.format(Locale.GERMAN, "%d steps", ffData.getVitalSigns().getStepCount()));

                        switch (dataManager.criticalState(ffData)) {
                            case CRITICAL_STATE_DEAD:
                                holder.statusIconView.setImageDrawable(getDrawable(R.drawable.ic_accessibility_24px));
                                holder.statusIconView.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.status_danger), android.graphics.PorterDuff.Mode.MULTIPLY);
                                break;
                            case CRITICAL_STATE_CRITICAL:
                                holder.statusIconView.setImageDrawable(getDrawable(R.drawable.ic_accessibility_24px));
                                holder.statusIconView.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.status_danger), android.graphics.PorterDuff.Mode.MULTIPLY);
                                break;
                            case CRITICAL_STATE_WARNING:
                                holder.statusIconView.setImageDrawable(getDrawable(R.drawable.ic_accessibility_24px));
                                holder.statusIconView.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.status_warning), android.graphics.PorterDuff.Mode.MULTIPLY);
                                break;
                            case CRITICAL_STATE_NOT_CRITICAL:
                                holder.statusIconView.setImageDrawable(getDrawable(R.drawable.ic_accessibility_24px));
                                holder.statusIconView.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.status_ok), android.graphics.PorterDuff.Mode.MULTIPLY);
                                break;
                        }

                        if (ffData.getVitalSigns().getHeartRate() < 50 || ffData.getVitalSigns().getHeartRate() > 200) {
                            if (timedOut) {
                                holder.heartRateView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
                            } else {
                                holder.heartRateView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.warning_font));
                            }
                        } else {
                            holder.heartRateView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.black));
                        }
                        break;
                    }
                    case NO_DATA: {
                        holder.statusIconView.setImageDrawable(getDrawable(R.drawable.ic_assignment_late_24px));
                        holder.statusIconView.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.status_error), android.graphics.PorterDuff.Mode.MULTIPLY);
                        holder.heartRateView.setText("No data");
                        holder.stepCountView.setText("No data");
                        break;
                    }
                }
            } else {
                Log.e(TAG, "Didn't find STATUS. Can't process update.");
            }
        }

        @Override
        public int getItemCount() {
            return dataManager.getDataEntries().size();
        }
    }
}
