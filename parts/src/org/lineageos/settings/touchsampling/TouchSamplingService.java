/*
 * Copyright (C) 2025 The LineageOS Project
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

package org.lineageos.settings.touchsampling;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class TouchSamplingService extends Service {
    private static final String TAG = "TouchSamplingService";

    private BroadcastReceiver mScreenUnlockReceiver;
    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener;
    private FileObserver mSconfigObserver;
    private Handler mAutoAppsHandler;
    private Runnable mAutoAppsRunnable;
    private NotificationManager mNotificationManager;
    private static final int NOTIFICATION_ID = 3;
    private static final String NOTIFICATION_CHANNEL_ID = "touch_sampling_tile_service_channel";
    private boolean mLastEffectiveState = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TouchSamplingService started");

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        setupNotificationChannel();

        // Initialize and register the broadcast receiver
        registerScreenUnlockReceiver();

        // Initialize and register the SharedPreferences listener
        registerPreferenceChangeListener();

        // Apply the touch sampling rate initially
        updateEffectiveStateAndApply();

        // Start a FileObserver to watch the sconfig file changes
        mSconfigObserver = new FileObserver(TouchSamplingUtils.SCONFIG_FILE, FileObserver.MODIFY) {
            @Override
            public void onEvent(int event, String path) {
                if ((event & FileObserver.MODIFY) != 0) {
                    Log.d(TAG, "sconfig file modified. Reapplying touch sampling rate.");
                    updateEffectiveStateAndApply();
                }
            }
        };
        mSconfigObserver.startWatching();

        // Periodically check for auto-enabled apps every 2 seconds
        mAutoAppsHandler = new Handler();
        mAutoAppsRunnable = new Runnable() {
            @Override
            public void run() {
                updateEffectiveStateAndApply();
                mAutoAppsHandler.postDelayed(this, 2000);
            }
        };
        mAutoAppsHandler.post(mAutoAppsRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "TouchSamplingService stopped");

        // Unregister the broadcast receiver
        if (mScreenUnlockReceiver != null) {
            unregisterReceiver(mScreenUnlockReceiver);
        }

        // Unregister the SharedPreferences change listener
        SharedPreferences sharedPref = getSharedPreferences(TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        sharedPref.unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);

        // Stop watching sconfig file changes
        if (mSconfigObserver != null) {
            mSconfigObserver.stopWatching();
        }
        if (mAutoAppsHandler != null) {
            mAutoAppsHandler.removeCallbacks(mAutoAppsRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Registers a BroadcastReceiver to handle screen unlock and screen on events.
     */
    private void registerScreenUnlockReceiver() {
        mScreenUnlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_USER_PRESENT.equals(intent.getAction()) ||
                        Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    Log.d(TAG, "Screen turned on or device unlocked. Reapplying touch sampling rate.");
                    updateEffectiveStateAndApply();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenUnlockReceiver, filter);
    }

    /**
     * Registers a SharedPreferences listener to monitor changes in the touch sampling settings.
     */
    private void registerPreferenceChangeListener() {
        SharedPreferences sharedPref = getSharedPreferences(TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        mPreferenceChangeListener = (sharedPreferences, key) -> {
            if (TouchSamplingSettingsFragment.HTSR_STATE.equals(key)
                    || "htsr_auto_enable_selected_apps".equals(key)) {
                Log.d(TAG, "Preference changed (" + key + "). Reapplying touch sampling rate.");
                updateEffectiveStateAndApply();
            }
        };
        sharedPref.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    /**
     * Reads the touch sampling rate preferences and applies the effective state.
     */
    private void updateEffectiveStateAndApply() {
        boolean effectiveState = getEffectiveTouchSamplingEnabled(this);
        if (effectiveState != mLastEffectiveState) {
            // State changed: update node and notification
            applyTouchSamplingRate(effectiveState ? 1 : 0);
            updateNotification(effectiveState);
            mLastEffectiveState = effectiveState;
        } else if (effectiveState) {
            // State is ON, but may need to reapply node (e.g., after unlock/boot)
            applyTouchSamplingRate(1);
        } else {
            // State is OFF, but may need to reapply node (e.g., after unlock/boot)
            applyTouchSamplingRate(0);
        }
    }

    /**
     * Applies the given touch sampling rate state directly to the hardware file.
     *
     * @param state 1 to enable high touch sampling rate, 0 to disable it.
     */
    private void applyTouchSamplingRate(int state) {
        String currentState = FileUtils.readOneLine(TouchSamplingUtils.HTSR_FILE);
        if (currentState == null || !currentState.equals(Integer.toString(state))) {
            Log.d(TAG, "Applying touch sampling rate: " + state);
            FileUtils.writeLine(TouchSamplingUtils.HTSR_FILE, Integer.toString(state));
        }
    }

    /**
     * Checks if the foreground app is in the auto-enable list and applies HTSR accordingly.
     */
    private void applyTouchSamplingRateForAutoApps() {
        SharedPreferences sharedPref = getSharedPreferences(TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        boolean mainEnabled = sharedPref.getBoolean(TouchSamplingSettingsFragment.HTSR_STATE, false);
        boolean autoEnableSelectedApps = sharedPref.getBoolean("htsr_auto_enable_selected_apps", true);
        if (mainEnabled || !autoEnableSelectedApps) {
            // Already handled by other logic or not enabled
            return;
        }
        // Check auto apps
        java.util.Set<String> autoApps = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .getStringSet(org.lineageos.settings.touchsampling.TouchSamplingPerAppConfigFragment.PREF_AUTO_APPS, new java.util.HashSet<>());
        String foreground = getForegroundApp(this);
        int effectiveState = (autoApps.contains(foreground)) ? 1 : 0;
        applyTouchSamplingRate(effectiveState);
    }

    /**
     * Returns the package name of the current foreground app.
     */
    private static String getForegroundApp(Context context) {
        android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            java.util.List<android.app.ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty() && tasks.get(0).topActivity != null) {
                return tasks.get(0).topActivity.getPackageName();
            }
        }
        return null;
    }

    private void updateNotification(boolean effectiveState) {
        if (effectiveState) {
            showTouchSamplingNotification();
        } else {
            cancelTouchSamplingNotification();
        }
    }

    private void setupNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.touch_sampling_mode_title),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setBlockable(true);
        mNotificationManager.createNotificationChannel(channel);
    }

    private void showTouchSamplingNotification() {
        Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.touch_sampling_mode_title))
                .setContentText(getString(R.string.touch_sampling_mode_notification))
                .setSmallIcon(R.drawable.ic_touch_sampling_tile)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setFlag(Notification.FLAG_NO_CLEAR, true)
                .build();
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void cancelTouchSamplingNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    public static boolean getEffectiveTouchSamplingEnabled(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        boolean mainEnabled = sharedPref.getBoolean(TouchSamplingSettingsFragment.HTSR_STATE, false);
        boolean autoEnableSelectedApps = sharedPref.getBoolean("htsr_auto_enable_selected_apps", true);
        if (mainEnabled) return true;
        if (autoEnableSelectedApps) {
            // Check auto apps
            java.util.Set<String> autoApps = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
                    .getStringSet(org.lineageos.settings.touchsampling.TouchSamplingPerAppConfigFragment.PREF_AUTO_APPS, new java.util.HashSet<>());
            String foreground = getForegroundApp(context);
            return autoApps.contains(foreground);
        }
        return false;
    }
}
