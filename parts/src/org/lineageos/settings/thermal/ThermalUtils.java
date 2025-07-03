/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.Log;
import android.telecom.DefaultDialerManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import androidx.preference.PreferenceManager;

import com.android.settingslib.applications.AppUtils;

import org.lineageos.settings.utils.FileUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ThermalUtils {

    private static final String TAG = "ThermalUtils";
    private static final String THERMAL_CONTROL = "thermal_control_v2";
    private static final String THERMAL_ENABLED = "thermal_enabled";

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_BENCHMARK = 1;
    protected static final int STATE_BROWSER = 2;
    protected static final int STATE_CAMERA = 3;
    protected static final int STATE_DIALER = 4;
    protected static final int STATE_GAMING = 5;
    protected static final int STATE_NAVIGATION = 6;
    protected static final int STATE_STREAMING = 7;
    protected static final int STATE_VIDEO = 8;
    protected static final int STATE_NOLIMITS = 9;
    protected static final int STATE_CLASS0 = 10;
    protected static final int STATE_YOUTUBE = 11;
    protected static final int STATE_ARVR = 12;
    protected static final int STATE_VIDEOCHAT = 13;
    protected static final int STATE_4K = 14;
    protected static final int STATE_TGAME = 15;
    protected static final int STATE_MGAME = 16;
    protected static final int STATE_YUANSHEN = 17;
    protected static final int STATE_HIGHFPS = 18;
    protected static final int STATE_CHARGE = 19;
    protected static final int STATE_PER_CLASS0 = 20;

    private static final Map<Integer, String> THERMAL_STATE_MAP;
    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(STATE_DEFAULT, "0");
        map.put(STATE_BENCHMARK, "6");
        map.put(STATE_BROWSER, "11");
        map.put(STATE_CAMERA, "15");
        map.put(STATE_DIALER, "1");
        map.put(STATE_GAMING, "19");
        map.put(STATE_NAVIGATION, "10");
        map.put(STATE_STREAMING, "4");
        map.put(STATE_VIDEO, "11");
        map.put(STATE_NOLIMITS, "6");
        map.put(STATE_CLASS0, "7");
        map.put(STATE_YOUTUBE, "8");
        map.put(STATE_ARVR, "9");
        map.put(STATE_VIDEOCHAT, "14");
        map.put(STATE_4K, "16");
        map.put(STATE_TGAME, "18");
        map.put(STATE_MGAME, "19");
        map.put(STATE_YUANSHEN, "20");
        map.put(STATE_HIGHFPS, "26");
        map.put(STATE_CHARGE, "27");
        map.put(STATE_PER_CLASS0, "57");
        THERMAL_STATE_MAP = Collections.unmodifiableMap(map);
    }

    private static final String THERMAL_BENCHMARK = "thermal.benchmark=";
    private static final String THERMAL_BROWSER = "thermal.browser=";
    private static final String THERMAL_CAMERA = "thermal.camera=";
    private static final String THERMAL_DIALER = "thermal.dialer=";
    private static final String THERMAL_GAMING = "thermal.gaming=";
    private static final String THERMAL_NAVIGATION = "thermal.navigation=";
    private static final String THERMAL_STREAMING = "thermal.streaming=";
    private static final String THERMAL_VIDEO = "thermal.video=";
    private static final String THERMAL_DEFAULT = "thermal.default=";
    private static final String THERMAL_NOLIMITS = "thermal.nolimits=";
    private static final String THERMAL_CLASS0 = "thermal.class0=";
    private static final String THERMAL_YOUTUBE = "thermal.youtube=";
    private static final String THERMAL_ARVR = "thermal.arvr=";
    private static final String THERMAL_VIDEOCHAT = "thermal.videochat=";
    private static final String THERMAL_4K = "thermal.4k=";
    private static final String THERMAL_TGAME = "thermal.tgame=";
    private static final String THERMAL_MGAME = "thermal.mgame=";
    private static final String THERMAL_YUANSHEN = "thermal.yuanshen=";
    private static final String THERMAL_HIGHFPS = "thermal.highfps=";
    private static final String THERMAL_CHARGE = "thermal.charge=";
    private static final String THERMAL_PER_CLASS0 = "thermal.per-class0=";

    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    private static final String GMAPS_PACKAGE = "com.google.android.apps.maps";
    private static final String GMEET_PACKAGE = "com.google.android.apps.tachyon";

    private Context mContext;
    private Display mDisplay;
    private SharedPreferences mSharedPrefs;
    private Boolean mEnabled;
    private String mCurrentState;
    private Intent mServiceIntent;

    private static ThermalUtils sInstance;

    private ThermalUtils(Context context) {
        mContext = context.getApplicationContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        WindowManager mWindowManager = mContext.getSystemService(WindowManager.class);
        mDisplay = mWindowManager.getDefaultDisplay();
        mEnabled = isEnabled();
        mServiceIntent = new Intent(mContext, ThermalService.class);
    }

    public static synchronized ThermalUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ThermalUtils(context);
        }
        return sInstance;
    }

    public void startService() {
        if (mEnabled) {
            dlog("startService");
            mContext.startServiceAsUser(mServiceIntent, UserHandle.CURRENT);
        }
    }

    private void stopService() {
        dlog("stopService");
        mContext.stopService(mServiceIntent);
    }

    protected Boolean isEnabled() {
        return mSharedPrefs.getBoolean(THERMAL_ENABLED, true);
    }

    protected void setEnabled(boolean enabled) {
        if (mEnabled == enabled) return;
        dlog("setEnabled: " + enabled);
        mEnabled = enabled;
        mSharedPrefs.edit().putBoolean(THERMAL_ENABLED, enabled).apply();
        if (enabled) {
            startService();
        } else {
            setDefaultThermalProfile();
            stopService();
        }
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(THERMAL_CONTROL, profiles).commit();
        dlog("writeValue: saved profiles: " + profiles);
    }

    private String getValue() {
        String value = mSharedPrefs.getString(THERMAL_CONTROL, null);

        if (value == null || value.isEmpty()) {
            value = THERMAL_BENCHMARK + ":" + THERMAL_BROWSER + ":" + THERMAL_CAMERA + ":" +
                    THERMAL_DIALER + ":" + THERMAL_GAMING + ":" + THERMAL_NAVIGATION + ":" +
                    THERMAL_STREAMING + ":" + THERMAL_VIDEO + ":" + THERMAL_DEFAULT + ":" +
                    THERMAL_NOLIMITS + ":" + THERMAL_CLASS0 + ":" + THERMAL_YOUTUBE + ":" +
                    THERMAL_ARVR + ":" + THERMAL_VIDEOCHAT + ":" + THERMAL_4K + ":" +
                    THERMAL_TGAME + ":" + THERMAL_MGAME + ":" + THERMAL_YUANSHEN + ":" +
                    THERMAL_HIGHFPS + ":" + THERMAL_CHARGE + ":" + THERMAL_PER_CLASS0;
            writeValue(value);
            dlog("getValue: initialized default value");
        } else {
            dlog("getValue: loaded from prefs: " + value);
        }
        return value;
    }

    protected void writePackage(String packageName, int mode) {
        String value = getValue();
        String[] modes = value.split(":");
        
        if (modes.length != 21) {
            value = getValue();
            modes = value.split(":");
        }

        // First, remove the package from all modes
        for (int i = 0; i < modes.length; i++) {
            modes[i] = modes[i].replace(packageName + ",", "");
        }

        // Then add the package to the selected mode
        switch (mode) {
            case STATE_BENCHMARK:
                if (modes.length > 0) modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_BROWSER:
                if (modes.length > 1) modes[1] = modes[1] + packageName + ",";
                break;
            case STATE_CAMERA:
                if (modes.length > 2) modes[2] = modes[2] + packageName + ",";
                break;
            case STATE_DIALER:
                if (modes.length > 3) modes[3] = modes[3] + packageName + ",";
                break;
            case STATE_GAMING:
                if (modes.length > 4) modes[4] = modes[4] + packageName + ",";
                break;
            case STATE_NAVIGATION:
                if (modes.length > 5) modes[5] = modes[5] + packageName + ",";
                break;
            case STATE_STREAMING:
                if (modes.length > 6) modes[6] = modes[6] + packageName + ",";
                break;
            case STATE_VIDEO:
                if (modes.length > 7) modes[7] = modes[7] + packageName + ",";
                break;
            case STATE_DEFAULT:
                if (modes.length > 8) modes[8] = modes[8] + packageName + ",";
                break;
            case STATE_NOLIMITS:
                if (modes.length > 9) modes[9] = modes[9] + packageName + ",";
                break;
            case STATE_CLASS0:
                if (modes.length > 10) modes[10] = modes[10] + packageName + ",";
                break;
            case STATE_YOUTUBE:
                if (modes.length > 11) modes[11] = modes[11] + packageName + ",";
                break;
            case STATE_ARVR:
                if (modes.length > 12) modes[12] = modes[12] + packageName + ",";
                break;
            case STATE_VIDEOCHAT:
                if (modes.length > 13) modes[13] = modes[13] + packageName + ",";
                break;
            case STATE_4K:
                if (modes.length > 14) modes[14] = modes[14] + packageName + ",";
                break;
            case STATE_TGAME:
                if (modes.length > 15) modes[15] = modes[15] + packageName + ",";
                break;
            case STATE_MGAME:
                if (modes.length > 16) modes[16] = modes[16] + packageName + ",";
                break;
            case STATE_YUANSHEN:
                if (modes.length > 17) modes[17] = modes[17] + packageName + ",";
                break;
            case STATE_HIGHFPS:
                if (modes.length > 18) modes[18] = modes[18] + packageName + ",";
                break;
            case STATE_CHARGE:
                if (modes.length > 19) modes[19] = modes[19] + packageName + ",";
                break;
            case STATE_PER_CLASS0:
                if (modes.length > 20) modes[20] = modes[20] + packageName + ",";
                break;
        }

        String finalString = String.join(":", modes);
        writeValue(finalString);
        
        // Log for debugging
        dlog("writePackage: " + packageName + " -> mode " + mode);
    }

    protected int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        int state = STATE_DEFAULT;

        if (modes.length != 21) {
            value = getValue();
            modes = value.split(":");
        }

        if (modes.length > 0 && modes[0].contains(packageName + ",")) {
            state = STATE_BENCHMARK;
        } else if (modes.length > 1 && modes[1].contains(packageName + ",")) {
            state = STATE_BROWSER;
        } else if (modes.length > 2 && modes[2].contains(packageName + ",")) {
            state = STATE_CAMERA;
        } else if (modes.length > 3 && modes[3].contains(packageName + ",")) {
            state = STATE_DIALER;
        } else if (modes.length > 4 && modes[4].contains(packageName + ",")) {
            state = STATE_GAMING;
        } else if (modes.length > 5 && modes[5].contains(packageName + ",")) {
            state = STATE_NAVIGATION;
        } else if (modes.length > 6 && modes[6].contains(packageName + ",")) {
            state = STATE_STREAMING;
        } else if (modes.length > 7 && modes[7].contains(packageName + ",")) {
            state = STATE_VIDEO;
        } else if (modes.length > 8 && modes[8].contains(packageName + ",")) {
            state = STATE_DEFAULT;
        } else if (modes.length > 9 && modes[9].contains(packageName + ",")) {
            state = STATE_NOLIMITS;
        } else if (modes.length > 10 && modes[10].contains(packageName + ",")) {
            state = STATE_CLASS0;
        } else if (modes.length > 11 && modes[11].contains(packageName + ",")) {
            state = STATE_YOUTUBE;
        } else if (modes.length > 12 && modes[12].contains(packageName + ",")) {
            state = STATE_ARVR;
        } else if (modes.length > 13 && modes[13].contains(packageName + ",")) {
            state = STATE_VIDEOCHAT;
        } else if (modes.length > 14 && modes[14].contains(packageName + ",")) {
            state = STATE_4K;
        } else if (modes.length > 15 && modes[15].contains(packageName + ",")) {
            state = STATE_TGAME;
        } else if (modes.length > 16 && modes[16].contains(packageName + ",")) {
            state = STATE_MGAME;
        } else if (modes.length > 17 && modes[17].contains(packageName + ",")) {
            state = STATE_YUANSHEN;
        } else if (modes.length > 18 && modes[18].contains(packageName + ",")) {
            state = STATE_HIGHFPS;
        } else if (modes.length > 19 && modes[19].contains(packageName + ",")) {
            state = STATE_CHARGE;
        } else if (modes.length > 20 && modes[20].contains(packageName + ",")) {
            state = STATE_PER_CLASS0;
        } else {
            // derive a default state based on package name
            state = getDefaultStateForPackage(packageName);
        }

        dlog("getStateForPackage: " + packageName + " -> state " + state);
        return state;
    }

    protected void setDefaultThermalProfile() {
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATE_MAP.get(STATE_DEFAULT));
    }

    protected void setThermalProfile(String packageName) {
        final int state = getStateForPackage(packageName);
        FileUtils.writeLine(THERMAL_SCONFIG, THERMAL_STATE_MAP.get(state));
    }

    private int getDefaultStateForPackage(String packageName) {
        switch (packageName) {
            case GMAPS_PACKAGE:
                return STATE_NAVIGATION;
            case GMEET_PACKAGE:
                return STATE_VIDEOCHAT;
            case "com.google.android.youtube":
            case "app.revanced.android.youtube":
                return STATE_YOUTUBE;
            case "com.miHoYo.Yuanshen":
                return STATE_YUANSHEN;
            case "com.tencent.ig":
            case "com.pubg.krmobile":
            case "com.pubg.imobile":
                return STATE_TGAME;
            case "com.miHoYo.GenshinImpact":
            case "com.activision.callofduty.shooter":
            case "com.gametion.codm":
                return STATE_MGAME;
        }

        final PackageManager pm = mContext.getPackageManager();
        final ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, /* flags */ 0);
        } catch (PackageManager.NameNotFoundException e) {
            return STATE_DEFAULT;
        }

        switch (appInfo.category) {
            case ApplicationInfo.CATEGORY_GAME:
                return STATE_GAMING;
            case ApplicationInfo.CATEGORY_VIDEO:
                return STATE_VIDEO;
            case ApplicationInfo.CATEGORY_MAPS:
                return STATE_NAVIGATION;
        }

        if (AppUtils.isBrowserApp(mContext, packageName, UserHandle.myUserId())) {
            return STATE_BROWSER;
        } else if (DefaultDialerManager.getDefaultDialerApplication(mContext).equals(packageName)) {
            return STATE_DIALER;
        } else if (isCameraApp(packageName)) {
            return STATE_CAMERA;
        } else {
            return STATE_DEFAULT;
        }
    }

    private boolean isCameraApp(String packageName) {
        final Intent cameraIntent =
                new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                .setPackage(packageName);

        final List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivitiesAsUser(
                cameraIntent, PackageManager.MATCH_ALL, UserHandle.myUserId());
        for (ResolveInfo info : list) {
            if (info.activityInfo != null) {
                return true;
            }
        }
        return false;
    }

    private static void dlog(String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, msg);
        }
    }
}
