package com.vrem.wifianalyzer.wifi.model;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.vrem.util.FileUtils;
import com.vrem.wifianalyzer.MainContext;
import com.vrem.wifianalyzer.R;
import com.vrem.wifianalyzer.vendor.model.VendorService;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import static android.content.ContentValues.TAG;

public class WiFiData {

    public static final WiFiData EMPTY = new WiFiData(Collections.emptyList(), WiFiConnection.EMPTY, Collections.emptyList());
    public static String location = "";
    public static List<WiFiDetail> wiFiDetails;
    private final List<String> wiFiConfigurations;
    private Context mContext = MainContext.INSTANCE.getContext();
    private final WiFiConnection wiFiConnection;
    private static JSONObject wiFiLocations = new JSONObject();


    static

    {
        try {

            String content = FileUtils.readFile(MainContext.INSTANCE.getResources(), R.raw.shanghai);
            wiFiLocations = new JSONObject(content);
            Toast.makeText(MainContext.INSTANCE.getContext(), "Loaded locations with " + wiFiLocations.length() + " MAC -> location mappings", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(MainContext.INSTANCE.getContext(), "Failed to load locations", Toast.LENGTH_LONG).show();
        }
    }

    private static String lastNotified = "";

    public WiFiData(@NonNull List<WiFiDetail> wiFiDetails, @NonNull WiFiConnection wiFiConnection, @NonNull List<String> wiFiConfigurations) {
        this.wiFiDetails = wiFiDetails;
        this.wiFiConnection = wiFiConnection;
        this.wiFiConfigurations = wiFiConfigurations;
    }

    @NonNull
    public WiFiDetail getConnection() {
        WiFiDetail wiFiDetail = IterableUtils.find(wiFiDetails, new ConnectionPredicate());
        return wiFiDetail == null ? WiFiDetail.EMPTY : copyWiFiDetail(wiFiDetail);
    }

    @NonNull
    public List<WiFiDetail> getWiFiDetails(@NonNull Predicate<WiFiDetail> predicate, @NonNull SortBy sortBy) {
        return getWiFiDetails(predicate, sortBy, GroupBy.NONE);
    }

    @NonNull
    public List<WiFiDetail> getWiFiDetails(@NonNull Predicate<WiFiDetail> predicate, @NonNull SortBy sortBy, @NonNull GroupBy groupBy) {
        List<WiFiDetail> results = getWiFiDetails(predicate);
        if (!results.isEmpty() && !GroupBy.NONE.equals(groupBy)) {
            results = sortAndGroup(results, sortBy, groupBy);
        }
        Collections.sort(results, sortBy.comparator());
        notifyLocation(results);
        return results;
    }

    @NonNull
    List<WiFiDetail> sortAndGroup(@NonNull List<WiFiDetail> wiFiDetails, @NonNull SortBy sortBy, @NonNull GroupBy groupBy) {
        List<WiFiDetail> results = new ArrayList<>();
        Collections.sort(wiFiDetails, groupBy.sortOrderComparator());
        WiFiDetail parent = null;
        for (WiFiDetail wiFiDetail : wiFiDetails) {
            if (parent == null || groupBy.groupByComparator().compare(parent, wiFiDetail) != 0) {
                if (parent != null) {
                    Collections.sort(parent.getChildren(), sortBy.comparator());
                }
                parent = wiFiDetail;
                results.add(parent);
            } else {
                parent.addChild(wiFiDetail);
            }
        }
        if (parent != null) {
            Collections.sort(parent.getChildren(), sortBy.comparator());
        }
        Collections.sort(results, sortBy.comparator());
        return results;
    }


    private void notifyLocation(List<WiFiDetail> wifiDetails) {


        if (wifiDetails.size() < 3) {
            return;
        }
        try {
            String lookupKey = (wifiDetails.get(0).getBSSID().toUpperCase()/*+" " + wifiDetails.get(0).getWiFiSignal().getStrength()+"\n"+wifiDetails.get(1).getBSSID().toUpperCase()+" " + wifiDetails.get(1).getWiFiSignal().getStrength()+"\n"+wifiDetails.get(2).getBSSID().toUpperCase()+" " + wifiDetails.get(2).getWiFiSignal().getStrength()*/);
            Log.d(TAG, "Wifidata: " + lookupKey);
            location = wiFiLocations.getString(lookupKey);
            if (location == null) {
                location = wiFiLocations.getString((wifiDetails.get(1).getBSSID() + " " + wifiDetails.get(0).getBSSID() + " " + wifiDetails.get(2).getBSSID()).toUpperCase());
            }
            if (location == null) {
                location = wiFiLocations.getString((wifiDetails.get(0).getBSSID() + " " + wifiDetails.get(1).getBSSID()).toUpperCase());
            }
            if (location == null) {
                location = wiFiLocations.getString((wifiDetails.get(1).getBSSID() + " " + wifiDetails.get(0).getBSSID()).toUpperCase());
            }
            if (location == null) {
                location = wiFiLocations.getString(wifiDetails.get(0).getBSSID().toUpperCase());
            }
            if (location == null) {
                location = wiFiLocations.getString(wifiDetails.get(1).getBSSID().toUpperCase());
            }
            if (location != null) {
                if (!lastNotified.equals(location)) {
                    Log.d(TAG, "notifyLocation: "+location);
                    Log.d(TAG, "notifyLocation: "+lastNotified);
                    Toast.makeText(MainContext.INSTANCE.getContext(), "You are in " + location, Toast.LENGTH_LONG).show();
                    lastNotified = location;

                }
            } else {
                //Toast.makeText(MainContext.INSTANCE.getContext(), "Nothing found " + lookupKey, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
        }
    }

    @NonNull
    private List<WiFiDetail> getWiFiDetails(@NonNull Predicate<WiFiDetail> predicate) {
        Collection<WiFiDetail> selected = CollectionUtils.select(wiFiDetails, predicate);
        Collection<WiFiDetail> collected = CollectionUtils.collect(selected, new Transform());
        return new ArrayList<>(collected);
    }

    @NonNull
    public List<WiFiDetail> getWiFiDetails() {
        return Collections.unmodifiableList(wiFiDetails);
    }

    @NonNull
    public List<String> getWiFiConfigurations() {
        return Collections.unmodifiableList(wiFiConfigurations);
    }

    @NonNull
    public WiFiConnection getWiFiConnection() {
        return wiFiConnection;
    }

    @NonNull
    private WiFiDetail copyWiFiDetail(WiFiDetail wiFiDetail) {
        VendorService vendorService = MainContext.INSTANCE.getVendorService();
        String vendorName = vendorService.findVendorName(wiFiDetail.getBSSID());
        WiFiAdditional wiFiAdditional = new WiFiAdditional(vendorName, wiFiConnection);
        return new WiFiDetail(wiFiDetail, wiFiAdditional);
    }

    private class ConnectionPredicate implements Predicate<WiFiDetail> {
        @Override
        public boolean evaluate(WiFiDetail wiFiDetail) {
            return new EqualsBuilder()
                    .append(wiFiConnection.getSSID(), wiFiDetail.getSSID())
                    .append(wiFiConnection.getBSSID(), wiFiDetail.getBSSID())
                    .isEquals();
        }
    }

    private class Transform implements Transformer<WiFiDetail, WiFiDetail> {
        private final WiFiDetail connection;
        private final VendorService vendorService;

        private Transform() {
            this.connection = getConnection();
            this.vendorService = MainContext.INSTANCE.getVendorService();
        }

        @Override
        public WiFiDetail transform(WiFiDetail input) {
            if (input.equals(connection)) {
                return connection;
            }
            String vendorName = vendorService.findVendorName(input.getBSSID());
            boolean contains = wiFiConfigurations.contains(input.getSSID());
            WiFiAdditional wiFiAdditional = new WiFiAdditional(vendorName, contains);
            return new WiFiDetail(input, wiFiAdditional);
        }
    }

}