
package com.vrem.wifianalyzer.wifi.setdestination;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vrem.wifianalyzer.MainContext;
import com.vrem.wifianalyzer.R;
import com.vrem.wifianalyzer.settings.Settings;
import com.vrem.wifianalyzer.wifi.band.WiFiBand;
import com.vrem.wifianalyzer.wifi.model.SortBy;
import com.vrem.wifianalyzer.wifi.model.WiFiData;
import com.vrem.wifianalyzer.wifi.model.WiFiDetail;
import com.vrem.wifianalyzer.wifi.predicate.WiFiBandPredicate;
import com.vrem.wifianalyzer.wifi.scanner.UpdateNotifier;

import org.apache.commons.collections4.Predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;


public class SetTheDestinationFragment extends Fragment implements UpdateNotifier {


    private List<WiFiDetail> wiFiDetails;
    private static TextView currentLocationTv;
    private static String selectedDestination;
    private static String lastNotified;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void update(@NonNull WiFiData wiFiData) {
        setCurrentStationView();
        destinationHasBeenReached();
        Settings settings = MainContext.INSTANCE.getSettings();
        WiFiBand wiFiBand = settings.getWiFiBand();
        Predicate<WiFiDetail> predicate = new WiFiBandPredicate(wiFiBand);
        wiFiDetails = wiFiData.getWiFiDetails(predicate, SortBy.STRENGTH);
    }

    private boolean destinationHasBeenReached() {
        boolean arrived = false;
        if (WiFiData.location.equals(selectedDestination)&&!WiFiData.location.equals(lastNotified)) {
            headsUpNotification();
            vibrate();
            lastNotified = WiFiData.location;
            arrived = true;
        }
        return arrived;
}

    private void setCurrentStationView() {
        if (currentLocationTv == null) {
            currentLocationTv.setText("Unknown");
        } else {
            currentLocationTv.setText(WiFiData.location);
        }
    }



    private void vibrate() {
        Vibrator v = (Vibrator) MainContext.INSTANCE.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 200};
        v.vibrate(pattern,2);
        Log.d(TAG, "vibrate: it is called");

    }

    private void headsUpNotification(){

        Intent intent = new Intent(MainContext.INSTANCE.getContext(), SetTheDestinationFragment.class);
        PendingIntent pi  = PendingIntent.getActivity(MainContext.INSTANCE.getContext(), 0, intent, 0);
        Notification.Builder builder = new Notification.Builder(MainContext.INSTANCE.getContext());

        builder.setContentTitle(WiFiData.location.toString())
                .setContentText("You have arrived your destinantion")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(Notification.PRIORITY_MAX);

        NotificationManager notificationManager = (NotificationManager) MainContext.INSTANCE.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());

    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.set_destination, container, false);
        MainContext.INSTANCE.getScannerService().register(this::update);

        currentLocationTv = view.findViewById(R.id.collectDatatext);
        selectedDestination = "";

        Spinner spinner = view.findViewById(R.id.random_spinner);



        // Initializing a String Array
        String[] locations = new String[]{
                "终点 - Destination",
                "MERCURY_29E6",
                "***",
                "B8 conference room",
                "B#8 Women\'s restroom",
                "epwj0016",
                "B8 Classroom Door A"
        };


        final List<String> locationList = new ArrayList<>(Arrays.asList(locations));

        // Initializing an ArrayAdapter
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(MainContext.INSTANCE.getContext(),R.layout.spinner_item, locationList){
            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    // Disable the first item from Spinner
                    // First item will be use for hint
                    return false;
                }
                else
                {
                    return true;
                }
            }
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(spinnerArrayAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDestination = (String) parent.getItemAtPosition(position);

                // If user change the default selection
                // First item is disable and it is used for hint
                if(position > 0){
                    // Notify the selected item text
                    Toast.makeText
                            (MainContext.INSTANCE.getContext(), "Selected : " + selectedDestination, Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        swipeRefreshLayout = view.findViewById(R.id.collectDataRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SetTheDestinationFragment.ListViewOnRefreshListener());
        return view;
    }

    private void refresh() {
        swipeRefreshLayout.setRefreshing(true);
        MainContext.INSTANCE.getScannerService().update();
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private class ListViewOnRefreshListener implements SwipeRefreshLayout.OnRefreshListener {
        @Override
        public void onRefresh() {
            refresh();
        }
    }

}