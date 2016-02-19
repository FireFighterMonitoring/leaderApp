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

/**
 * The main activity
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int TIMEOUT_SECONDS = 30;
    public static final int DANGEROUS_HEARTRATE_OFFSET_MINIMUM = 50;
    public static final int DANGEROUS_HEARTRATE_OFFSET_MAXIMUM = 200;
    private RecyclerView.Adapter mAdapter;

    private DataManager dataManager;
    private ImageView disconnectedIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize data manager
        dataManager = new DataManager();
        Observable<Void> updateObservable = dataManager.activate();

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

    /**
     * A data holder structure for a single recycler view cell.
     */
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

    /**
     * The adapter for filling the recycler view.
     */
    private class FFAdapter extends RecyclerView.Adapter<FFDataHolder> {

        public static final String TAG = "FFAdapter";
        public static final int SECONDS_TO_MILLISECONDS_FACTOR = 1000;

        @Override
        public FFDataHolder onCreateViewHolder(ViewGroup parent, int pos) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_ffdata, parent, false);
            return new FFDataHolder(view);
        }

        @Override
        public void onBindViewHolder(FFDataHolder holder, int pos) {
            FireFighterData ffData = dataManager.getDataEntries().get(pos);

            // Update name (ffId)
            holder.nameTextView.setText(ffData.getFfId());

            // Update last updated
            Long epochMilliSeconds = ffData.getTimestamp().getEpochSecond() * SECONDS_TO_MILLISECONDS_FACTOR;
            Date lastUpDate = new Date(epochMilliSeconds);
            long lastUpdateSecondsAgo = (new Date().getTime() - lastUpDate.getTime()) / SECONDS_TO_MILLISECONDS_FACTOR;
            holder.lastUpdateTextView.setText(String.format(Locale.GERMAN, "last update: %d seconds ago", lastUpdateSecondsAgo));

            // Check if connection timed out.
            boolean timedOut = new Date().getTime() - TIMEOUT_SECONDS * SECONDS_TO_MILLISECONDS_FACTOR > epochMilliSeconds;

            // Set container background
            holder.statusIconView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, timedOut ? R.color.status_danger : R.color.status_neutral));

            if (ffData.getStatus() != null) {
                switch (ffData.getStatus()) {
                    case OK: {
                        // Everything is ok with the smartphone/wearable connection. We can use the vital data.
                        if (ffData.getVitalSigns() == null) {
                            Log.e(TAG, "Didn't find vital data. This should never happen! (Never, Tobi!)");
                            return;
                        }

                        // Update vital data content
                        String heartRateText = ffData.getVitalSigns().getHeartRate() != -1
                                ? String.format(Locale.GERMAN, "%d %s", ffData.getVitalSigns().getHeartRate(), getString(R.string.heartrate_suffix))
                                : getString(R.string.no_data);
                        holder.heartRateView.setText(heartRateText);
                        String stepCountText = ffData.getVitalSigns().getStepCount() != -1
                                ? String.format(Locale.GERMAN, "%d %s", ffData.getVitalSigns().getStepCount(), getString(R.string.steps_suffix))
                                : getString(R.string.no_data);
                        holder.stepCountView.setText(stepCountText);

                        // Update status icon
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

                        // If heartrate is critical, change the color of the label
                        if (ffData.getVitalSigns().getHeartRate() < DANGEROUS_HEARTRATE_OFFSET_MINIMUM || ffData.getVitalSigns().getHeartRate() > DANGEROUS_HEARTRATE_OFFSET_MAXIMUM) {
                            holder.heartRateView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.warning_font));
                        } else {
                            holder.heartRateView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.black));
                        }
                        break;
                    }
                    case NO_DATA: {
                        holder.statusIconView.setImageDrawable(getDrawable(R.drawable.ic_assignment_late_24px));
                        holder.statusIconView.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.status_error), android.graphics.PorterDuff.Mode.MULTIPLY);
                        holder.heartRateView.setText(R.string.no_data);
                        holder.stepCountView.setText(R.string.no_data);
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
