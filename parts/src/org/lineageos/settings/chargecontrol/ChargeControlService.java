/*
 * Copyright (C) 2025 kenway214
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

package org.lineageos.settings.chargecontrol;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import org.lineageos.settings.Constants;
import org.lineageos.settings.utils.FileUtils;

public class ChargeControlService extends Service {
    private static final String TAG = "ChargeControlService";
    private Handler mHandler;
    private Runnable mMonitorRunnable;
    private BroadcastReceiver mBatteryReceiver;
    private int mLastBatteryLevel = -1;
    private boolean mLastChargingState = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        registerBatteryReceiver();
        startMonitoring();
    }

    private void registerBatteryReceiver() {
        mBatteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                    int percent = (int) ((level / (float) scale) * 100);
                    mLastBatteryLevel = percent;
                    checkAndControlCharging();
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryReceiver, filter);
    }

    private void startMonitoring() {
        mMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndControlCharging();
                mHandler.postDelayed(this, 60000);
            }
        };
        mHandler.post(mMonitorRunnable);
    }

    private void checkAndControlCharging() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = prefs.getBoolean(Constants.KEY_CHARGE_CONTROL, false);
        int stopValue = prefs.getInt(Constants.KEY_STOP_CHARGING, 100);
        int batteryLevel = mLastBatteryLevel;
        if (!enabled) {
            FileUtils.writeLine(Constants.NODE_STOP_CHARGING, "0");
            return;
        }
        if (batteryLevel >= stopValue) {
            if (!mLastChargingState) {
                FileUtils.writeLine(Constants.NODE_STOP_CHARGING, "1");
                mLastChargingState = true;
                Log.d(TAG, "Charging stopped at " + batteryLevel + "%");
            }
        } else {
            if (mLastChargingState) {
                FileUtils.writeLine(Constants.NODE_STOP_CHARGING, "0");
                mLastChargingState = false;
                Log.d(TAG, "Charging allowed at " + batteryLevel + "%");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBatteryReceiver != null) unregisterReceiver(mBatteryReceiver);
        if (mHandler != null && mMonitorRunnable != null) mHandler.removeCallbacks(mMonitorRunnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 