package me.sunyfusion.bdsp.state;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import cz.msebera.android.httpclient.Header;
import me.sunyfusion.bdsp.activity.MainActivity;
import me.sunyfusion.bdsp.column.Column;
import me.sunyfusion.bdsp.column.Datestamp;
import me.sunyfusion.bdsp.column.Latitude;
import me.sunyfusion.bdsp.column.Longitude;
import me.sunyfusion.bdsp.column.Photo;
import me.sunyfusion.bdsp.column.Run;
import me.sunyfusion.bdsp.db.BdspDB;
import me.sunyfusion.bdsp.io.ReadFromInput;
import me.sunyfusion.bdsp.service.GpsService;

/**
 * Created by deisingj1 on 8/4/2016.
 */
public class Config {

    public static String SUBMIT_URL = "update.php";

    private Column latColumn, lonColumn, id;
    String url;
    private String id_key, id_value, email, table;
    private Run run;
    private Photo photo;
    private Datestamp date;
    private boolean LocInUse;
    private BdspDB db;

    public boolean isGpsTrackerEnabled() {
        return gpsTrackerEnabled;
    }

    boolean gpsTrackerEnabled = false;
    private String project;
    ArrayList<Column> uniques = new ArrayList<>();
    Context c;

    public Config(Context context) {
        c = context;   // Ties config to main activity
        init();
        db = new BdspDB(c);
    }
    //Currently not used, written to support updating configurations remotely, not finished
    public void getNewConfig() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get("https://example.com/file.png", new FileAsyncHttpResponseHandler(c) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                // Do something with the file `response`
            }
            @Override
            public void onFailure(int statusCode, Header[] header, Throwable t, File f) {


            }
        });
    }
    public BdspDB getDb() {
        if (db == null) {
            db = new BdspDB(c);
        }
        return db;
    }


    private void init() {
        String Type;
        Scanner infile = null;
        try {
            infile = new Scanner(c.getAssets().open("buildApp.txt"));   // scans File
        } catch (Exception e) {
            e.printStackTrace();
        }
        ReadFromInput readFile = new ReadFromInput(infile);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(c);

        do {
            try {
                readFile.getNextLine();
                readFile.ReadLineCollectInfo();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Type = readFile.getType();

            switch (Type) {
                case "url":
                    if(url == null) {
                        url = readFile.getArg(1);
                        if(!SUBMIT_URL.contains(readFile.getArg(1))) {
                            SUBMIT_URL = readFile.getArg(1) + SUBMIT_URL;
                        }
                    }
                    break;
                case "project":
                    project = readFile.getArg(1);
                    break;
                case "locOnSub":
                    latColumn = new Column(lbm, Column.ColumnType.LATITUDE, readFile.getArg(2), getDb());
                    lonColumn = new Column(lbm, Column.ColumnType.LONGITUDE, readFile.getArg(3),getDb());
                    break;
                case "email":
                    email = readFile.getArg(1);
                    break;
                case "id":
                    id_key = readFile.getArg(1);
                    id = new Column(lbm, Column.ColumnType.ID, id_key, getDb());
                    break;
                case "photo":
                    if (readFile.enabled()) {
                        photo = new Photo(c,readFile.getArg(2),getDb());
                    }
                    break;
                case "gpsLoc":
                    if (readFile.enabled()) {
                        checkGPSPermission();
                        LocInUse = true;
                        latColumn = new Latitude(c, readFile.getArg(2),getDb());
                        lonColumn = new Longitude(c, readFile.getArg(3),getDb());
                    }
                    break;
                case "gpsTracker":
                    gpsTrackerEnabled = readFile.enabled();
                    if (readFile.enabled())
                        checkGPSPermission();
                    break;
                case "unique":
                    uniques.add(new Column(lbm, Column.ColumnType.UNIQUE, readFile.getArg(1),getDb()));
                    break;
                case "datetime":
                    date = new Datestamp(c,readFile.getArg(1),getDb());
                    break;
                case "table":
                    table = readFile.getArg(1);
                    break;
                case "run":
                    run = new Run(c, readFile.getArg(2),getDb());
                    break;
                default:
                    break;
            }
        }
        while (!Type.equals("endFile"));

    }

    public void updateUrl() {
        if(!SUBMIT_URL.contains("?idk=") && !SUBMIT_URL.contains("&email=")) {
            SUBMIT_URL += "?idk=" + id_key + "&idv=" + id_value + "&email=" + email + "&table=" + table;
        }
        System.out.println(SUBMIT_URL);
    }

    public void setIdValue(String idValue) {
        id.setValue(idValue);
        id_value = idValue;
    }
    public String getProjectUrl() {
        return url + "projects/" + project;
    }

    public Run getRun() {
        return run;
    }
    public ArrayList<Column> getUniques() {
        return uniques;
    }
    public String getIdKey() {
        return id_key;
    }
    public String getIdValue() {
        return id_value;
    }
    public Column getLatitude() { return latColumn; };
    public Column getLongitude() { return lonColumn; };
    public Column getId() { return id; }
    public boolean isPhotoEnabled() {
        return photo != null;
    }
    public boolean isLocationEnabled() {
        return LocInUse;
    }

    public void checkGPSPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(c,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {


                ActivityCompat.requestPermissions((Activity) c,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MainActivity.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            }
        }
        else {
            if(!isServiceRunning(GpsService.class)) {
                Global.getContext().startService(new Intent(c, GpsService.class));
            }
        }
    }
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
