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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import cz.msebera.android.httpclient.Header;
import me.sunyfusion.bdsp.BdspRow;
import me.sunyfusion.bdsp.activity.MainActivity;
import me.sunyfusion.bdsp.db.BdspDB;
import me.sunyfusion.bdsp.io.ReadFromInput;
import me.sunyfusion.bdsp.service.GpsService;

/**
 * Created by deisingj1 on 8/4/2016.
 */
public class Config {

    public ArrayList<String> uniques = new ArrayList<>();
    public static String SUBMIT_URL = "update.php";
    private String url;
    private String id_key, email, table;
    private BdspDB db;
    private String project;

    Context c;

    public Config(Context context) {
        c = context;   // Ties config to main activity
        init();
        db = new BdspDB(c);
    }

    //TODO Currently not used, written to support updating configurations remotely, not finished
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

    private void addColumn(BdspRow.ColumnType type, String name) {
        if (db == null) {
            db = new BdspDB(c);
        }
        db.addColumn(name,"TEXT");
        BdspRow.ColumnNames.put(type,name);
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
                    addColumn(BdspRow.ColumnType.LATITUDE, readFile.getArg(2));
                    addColumn(BdspRow.ColumnType.LONGITUDE, readFile.getArg(3));
                    break;
                case "email":
                    email = readFile.getArg(1);
                    break;
                case "id":
                    addColumn(BdspRow.ColumnType.ID,readFile.getArg(1));
                    id_key = readFile.getArg(1);
                    break;
                case "photo":
                    if (readFile.enabled()) {
                        //photo = new Photo(c,readFile.getArg(2),getDb());
                    }
                    break;
                case "gpsLoc":
                    if (readFile.enabled()) {
                        checkGPSPermission();
                        addColumn(BdspRow.ColumnType.LATITUDE,readFile.getArg(2));
                        addColumn(BdspRow.ColumnType.LONGITUDE,readFile.getArg(3));
                    }
                    break;
                case "gpsTracker":
                    if (readFile.enabled()) {
                        addColumn(BdspRow.ColumnType.GEOMETRY, readFile.getArg(2));
                        addColumn(BdspRow.ColumnType.START,readFile.getArg(4));
                        addColumn(BdspRow.ColumnType.END,readFile.getArg(5));
                        BdspRow.getInstance().markStart();
                        checkGPSPermission();
                    }
                    break;
                case "unique":
                    addColumn(BdspRow.ColumnType.UNIQUE, readFile.getArg(1));
                    uniques.add(readFile.getArg(1));
                    break;
                case "datetime":
                    addColumn(BdspRow.ColumnType.DATE,readFile.getArg(1));
                    BdspRow.ColumnNames.put(BdspRow.ColumnType.DATE,readFile.getArg(1));
                    break;
                case "table":
                    table = readFile.getArg(1);
                    break;
                case "run":
                    addColumn(BdspRow.ColumnType.RUN,readFile.getArg(2));
                    BdspRow.ColumnNames.put(BdspRow.ColumnType.RUN,readFile.getArg(2));
                    break;
                default:
                    break;
            }
        }
        while (!Type.equals("endFile"));

    }

    public void updateUrl() {
        if(!SUBMIT_URL.contains("?email=") && !SUBMIT_URL.contains("&table=")) {
            SUBMIT_URL += "?email=" + email + "&table=" + table;
        }
        System.out.println(SUBMIT_URL);
    }

    public String getProjectUrl() {
        return url + "projects/" + project;
    }

    public ArrayList<String> getUniques() {
        return uniques;
    }
    public String getIdKey() {
        return id_key;
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
            else {
                if(!isServiceRunning(GpsService.class)) {
                    Global.getContext().startService(new Intent(c, GpsService.class));
                }
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
