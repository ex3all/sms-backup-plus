package org.thialfihar.android.apg.utils;

import java.util.ArrayList;

import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.thialfihar.android.apg.IApgService;

/**
 * This class can be used by other projects to simplify connecting to the
 * APG-Service. Kind of wrapper of for AIDL.
 * 
 * It is not used in this project.
 */
public class ApgCon {

    private final static String TAG = "ApgCon";

    private final Context mContext;

    private final Bundle result = new Bundle();
    private final Bundle args = new Bundle();
    private final ArrayList<String> error_list = new ArrayList<String>();
    private final ArrayList<String> warning_list = new ArrayList<String>();

    /** Remote service for decrypting and encrypting data */
    private IApgService apgService = null;

    /** Set apgService accordingly to connection status */
    private ServiceConnection apgConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "IApgService bound to apgService");
            apgService = IApgService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "IApgService disconnected");
            apgService = null;
        }
    };

    public static enum error {
        GENERIC, // no special type
        CANNOT_BIND_TO_APG, // connection to apg service not possible
        CALL_MISSING, // function to call not provided
        CALL_NOT_KNOWN, // apg service does not know what to do
    }

    public ApgCon(Context ctx) {
        Log.d(TAG, "EncryptionService created");
        mContext = ctx;
    }

    /** try to connect to the apg service */
    private boolean connect() {
        Log.d(TAG, "trying to bind the apgService to context");

        if (apgService != null) {
            Log.d(TAG, "allready connected");
            return true;
        }

        try {
            mContext.bindService(new Intent(IApgService.class.getName()), apgConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.d(TAG, "could not bind APG service");
            return false;
        }

        int wait_count = 0;
        while (apgService == null && wait_count++ < 15) {
            Log.d(TAG, "sleeping 1 second to wait for apg");
            android.os.SystemClock.sleep(1000);
        }
        ;

        if (wait_count >= 15) {
            Log.d(TAG, "slept waiting for nothing!");
            return false;
        }

        return true;
    }

    private boolean initialize() {
        if (apgService == null) {
            if (!connect()) {
                Log.d(TAG, "connection to apg service failed");
                return false;
            }
        }
        return true;
    }

    public boolean call(String function) {
        return this.call(function, args, result);
    }

    public boolean call(String function, Bundle pArgs) {
        return this.call(function, pArgs, result);
    }

    public boolean call(String function, Bundle pArgs, Bundle pReturn) {

        error_list.clear();
        warning_list.clear();

        if (!initialize()) {
            error_list.add("CLASS: Cannot bind to ApgService");
            pReturn.putInt("CLASS_ERROR", error.CANNOT_BIND_TO_APG.ordinal());
            return false;
        }

        if (function == null || function.length() == 0) {
            error_list.add("CLASS: Function to call missing");
            pReturn.putInt("CLASS_ERROR", error.CALL_MISSING.ordinal());
            return false;
        }

        try {
            Boolean ret = (Boolean) IApgService.class.getMethod(function, Bundle.class, Bundle.class).invoke(apgService, pArgs, pReturn);
            error_list.addAll(pReturn.getStringArrayList("ERRORS"));
            warning_list.addAll(pReturn.getStringArrayList("WARNINGS"));
            return ret;
        } catch (NoSuchMethodException e) {
            Log.d(TAG, e.getMessage());
            error_list.add("CLASS: " + e.getMessage());
            pReturn.putInt("CLASS_ERROR", error.CALL_NOT_KNOWN.ordinal());
            return false;
        } catch (Exception e) {
            Log.d(TAG, "" + e.getMessage());
            error_list.add("CLASS: " + e.getMessage());
            pReturn.putInt("CLASS_ERROR", error.GENERIC.ordinal());
            return false;
        }

    }

    public void set_arg(String key, String val) {
        args.putString(key, val);
    }

    public void set_arg(String key, String vals[]) {
        ArrayList<String> list = new ArrayList<String>();
        for (String val : vals) {
            list.add(val);
        }
        args.putStringArrayList(key, list);
    }

    public void set_arg(String key, boolean val) {
        args.putBoolean(key, val);
    }

    public void set_arg(String key, int val) {
        args.putInt(key, val);
    }

    public void set_arg(String key, int vals[]) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int val : vals) {
            list.add(val);
        }
        args.putIntegerArrayList(key, list);
    }

    public void clear_args() {
        args.clear();
    }

    public Object get_arg(String key) {
        return args.get(key);
    }

    public String get_next_error() {
        if (error_list.size() != 0)
            return error_list.remove(0);
        else
            return null;
    }

    public boolean has_next_error() {
        return error_list.size() != 0;
    }

    public String get_next_warning() {
        if (warning_list.size() != 0)
            return warning_list.remove(0);
        else
            return null;
    }

    public boolean has_next_warning() {
        return warning_list.size() != 0;
    }

    public String get_result() {
        return result.getString("RESULT");
    }
    
    public void clear_errors() {
        error_list.clear();
    }
    
    public void clear_warnings() {
        warning_list.clear();
    }
    
    public void reset() {
        clear_errors();
        clear_warnings();
        clear_args();
    }

    public void disconnect() {
        Log.v(TAG, "disconnecting apgService");
        if (apgService != null) {
            mContext.unbindService(apgConnection);
            apgService = null;
        }
    }

}