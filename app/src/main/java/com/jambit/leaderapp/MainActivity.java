package com.jambit.leaderapp;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final int TIMEOUT_SECONDS = 30;
    private RecyclerView.Adapter mAdapter;

    private DataManager dataManager;
    private ImageView disconnectedIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize data manager
        dataManager = new DataManager();
        dataManager.activate();

        // Configure recycler view
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.ff_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new FFAdapter();
        mRecyclerView.setAdapter(mAdapter);

        disconnectedIcon = (ImageView) findViewById(R.id.disconnected_image_view);

        //Declare the timer
        Timer t = new Timer();
        //Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {

                                  @Override
                                  public void run() {
                                      runOnUiThread(new Runnable() {
                                          @Override
                                          public void run() {
                                              mAdapter.notifyDataSetChanged();

                                              runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      if (dataManager.isConnected()) {
                                                          disconnectedIcon.setVisibility(View.GONE);
                                                      } else {
                                                          disconnectedIcon.setVisibility(View.VISIBLE);
                                                      }
                                                  }
                                              });
                                          }
                                      });

                                  }
                              },
                //Set how long before to start calling the TimerTask (in milliseconds)
                0,
                //Set the amount of time between each execution (in milliseconds)
                1000);
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
            holder.heartRateView.setText(String.format(Locale.GERMAN, "%d bpm", ffData.getHeartRate()));
            holder.stepCountView.setText(String.format(Locale.GERMAN, "%d steps", ffData.getStepCount()));

            Long epochMilliSeconds = ffData.getTimestamp().getEpochSecond() * 1000;
            Date lastUpDate = new Date(epochMilliSeconds);
            long lastUpdateSecondsAgo = (new Date().getTime() - lastUpDate.getTime()) / 1000;
            holder.lastUpdateTextView.setText(String.format(Locale.GERMAN, "last update: %d seconds ago", lastUpdateSecondsAgo));

            if (ffData.getHeartRate() < 50 || ffData.getHeartRate() > 200) {
                holder.heartRateView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.warning_font));
            } else {
                holder.heartRateView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.black));
            }

            switch (dataManager.criticalState(ffData)) {
                case CRITICAL_STATE_DEAD:
                    holder.statusIconView.setImageDrawable(getDrawable(android.R.drawable.presence_offline));
                    break;
                case CRITICAL_STATE_CRITICAL:
                    holder.statusIconView.setImageDrawable(getDrawable(android.R.drawable.presence_busy));
                    break;
                case CRITICAL_STATE_WARNING:
                    holder.statusIconView.setImageDrawable(getDrawable(android.R.drawable.presence_away));
                    break;
                case CRITICAL_STATE_NOT_CRITICAL:
                    holder.statusIconView.setImageDrawable(getDrawable(android.R.drawable.presence_online));
                    break;
            }

            if (new Date().getTime() - TIMEOUT_SECONDS * 1000 > epochMilliSeconds) {
                holder.containerLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.warning));
                holder.heartRateView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
            } else {
                holder.containerLayout.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));
            }
        }

        @Override
        public int getItemCount() {
            return dataManager.getDataEntries().size();
        }
    }
}


