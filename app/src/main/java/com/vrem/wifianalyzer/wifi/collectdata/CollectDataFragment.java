package com.vrem.wifianalyzer.wifi.collectdata;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static android.content.ContentValues.TAG;


public class CollectDataFragment extends Fragment implements UpdateNotifier{

    private Button saveBtn;
    private EditText currentLocationEt;
    private static String objectString = "";
    private static JSONObject jsonObject=new JSONObject();


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.collect_data, container, false);
        MainContext.INSTANCE.getScannerService().register(this::update);

        saveBtn = view.findViewById(R.id.saveCollectedData);
        currentLocationEt = view.findViewById(R.id.collectDataEt);

        // loop through wifiDetails and put() all bssid and current location
        // call writeJSONtoFile with !!jsonObject!! IFF we really added any new values

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (currentLocationEt.getText().toString().isEmpty())
                    try {
                        Log.d(TAG, "onClick: " + WiFiData.wiFiDetails.get(0).getBSSID()+" "+WiFiData.wiFiDetails.get(0).getTitle().split(" ")[0]);
                        jsonObject.put(WiFiData.wiFiDetails.get(0).getBSSID(),WiFiData.wiFiDetails.get(0).getTitle().split(" ")[0]);
                        writeJSONtoFile(MainContext.INSTANCE.getContext(), currentLocationEt.getText().toString(), jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }
        });

       /* readJSON(currentLocationEt.toString());*/
        return view;
    }


    @Override
    public void update(@NonNull WiFiData wiFiData) {

        Settings settings = MainContext.INSTANCE.getSettings();
        WiFiBand wiFiBand = settings.getWiFiBand();
        Predicate<WiFiDetail> predicate = new WiFiBandPredicate(wiFiBand);
        WiFiData.wiFiDetails = wiFiData.getWiFiDetails(predicate, SortBy.STRENGTH);
    }

    private String readJSON(String filename) {
        File directory = Environment.getExternalStorageDirectory();
        File file = new File(directory + "/Notes/", filename);

        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {

                text.append(line);
                text.append('\n');
            }
            JSONObject jsonObject = new JSONObject(String.valueOf(text));
            for (int i = 0; i<jsonObject.length(); i++) {
                if (!objectString.contains(jsonObject.getString((String) jsonObject.names().get(i)))) {
                    objectString = objectString + " " + jsonObject.getString((String) jsonObject.names().get(i)) + "\n";
                }
            }
            /* Toast.makeText(mContext, objectString, Toast.LENGTH_SHORT).show();*/
            br.close();
        } catch (IOException e) {

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    public void writeJSONtoFile(Context context, String sFileName, JSONObject jsonObject) throws JSONException {
        // this function (writeJSONtoFile) is called every 5 seconds and every time it sends values of sKey(MAC) and sValue(NAME) that I want to store in a flattened JSONObject
       //sKey is MAC address of the wifi, sValue is the name of the Wifi
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            Log.d(TAG, "writeJSONtoFile");
            writer.append(jsonObject.toString()+"\n");
            writer.flush();
            writer.close();
            Toast.makeText(MainContext.INSTANCE.getContext(), "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Not saved", Toast.LENGTH_SHORT).show();
        }
    }




}
