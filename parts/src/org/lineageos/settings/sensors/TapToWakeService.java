/*
 * Copyright (C) 2025 Yet Another AOSP Extended Project
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

package org.lineageos.settings.sensors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

public class TapToWakeService extends Service {
    private static final String TAG = "TapToWakeService";
    private static final String DOUBLE_TAP_PROP = "vendor.sensors.xiaomi.double_tap";
    private static final String SINGLE_TAP_PROP = "vendor.sensors.xiaomi.single_tap";
    private static final String DOUBLE_TAP_SECURE = Settings.Secure.DOZE_DOUBLE_TAP_GESTURE;
    private static final String SINGLE_TAP_SECURE = Settings.Secure.DOZE_TAP_SCREEN_GESTURE;

    private Handler mHandler;
    private Context mContext;
    private boolean mIsRunning = false;

    private final ContentObserver mSettingsObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                updateTapToWakeState();
            } catch (Exception e) {
                Log.e(TAG, "Error updating tap to wake state", e);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mContext = getApplicationContext();
            mHandler = new Handler();
            
            // Register settings observer
            getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(DOUBLE_TAP_SECURE), false, mSettingsObserver, UserHandle.USER_CURRENT);
            getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(SINGLE_TAP_SECURE), false, mSettingsObserver, UserHandle.USER_CURRENT);
            
            // Initial update
            updateTapToWakeState();
            Log.d(TAG, "Service created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIsRunning) {
            Log.d(TAG, "Service already running");
            return START_STICKY;
        }
        
        try {
            mIsRunning = true;
            updateTapToWakeState();
            Log.d(TAG, "Service started");
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand", e);
            mIsRunning = false;
        }
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
            }
            getContentResolver().unregisterContentObserver(mSettingsObserver);
            mIsRunning = false;
            Log.d(TAG, "Service destroyed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateTapToWakeState() {
        try {
            boolean isSingleTapEnabled = Settings.Secure.getIntForUser(getContentResolver(), 
                    SINGLE_TAP_SECURE, 0, UserHandle.USER_CURRENT) == 1;
            boolean isDoubleTapEnabled = Settings.Secure.getIntForUser(getContentResolver(), 
                    DOUBLE_TAP_SECURE, 0, UserHandle.USER_CURRENT) == 1;
            
            SystemProperties.set(SINGLE_TAP_PROP, isSingleTapEnabled ? "true" : "false");
            SystemProperties.set(DOUBLE_TAP_PROP, isDoubleTapEnabled ? "true" : "false");
            
            Log.d(TAG, "Tap to wake state updated - Single: " + isSingleTapEnabled + 
                    ", Double: " + isDoubleTapEnabled);
        } catch (Exception e) {
            Log.e(TAG, "Error updating tap to wake state", e);
        }
    }
}
