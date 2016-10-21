package me.sunyfusion.bdsp.state;

import android.content.Context;

import me.sunyfusion.bdsp.db.BdspDB;

/**
 * Created by jesse on 8/1/16.
 */
public class Global {
    private Context context;
    private static BdspDB dbHelper;
    private static Global ourInstance = new Global();

    private Global() {

    }

    public static Global getInstance() {
        return ourInstance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = new BdspDB(context);
    }

    public static BdspDB getDb() {
        if (dbHelper == null) {
            dbHelper = new BdspDB(getContext());
        }
        return getInstance().dbHelper;
    }

    public static Context getContext() {
        return getInstance().context;
    }
}
