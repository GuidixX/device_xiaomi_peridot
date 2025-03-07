/*
 * Copyright (C) 2025 The LineageOS Project
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import org.lineageos.settings.R;
import org.lineageos.settings.touchsampling.TouchSamplingUtils;
import org.lineageos.settings.utils.FileUtils;

public class TouchSamplingSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String HTSR_ENABLE_KEY = "htsr_enable";
    public static final String SHAREDHTSR = "SHAREDHTSR";
    public static final String HTSR_STATE = "htsr_state";
    
    // Added constants for notification
    private static final int NOTIFICATION_ID = 3;
    private static final String NOTIFICATION_CHANNEL_ID = "touch_sampling_tile_service_channel";

    private SwitchPreference mHTSRPreference;
    private SharedPreferences mPrefs;
    private VideoPreference videoPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.htsr_settings);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        mPrefs = getActivity().getSharedPreferences(SHAREDHTSR, Context.MODE_PRIVATE);

        // Set the initial state of the main toggle
        mHTSRPreference = (SwitchPreference) findPreference(HTSR_ENABLE_KEY);
        boolean htsrEnabled = mPrefs.getBoolean(HTSR_STATE, false);
        mHTSRPreference.setChecked(htsrEnabled);
        mHTSRPreference.setOnPreferenceChangeListener(this);

        // Setup the "Automatically enable HTSR when game mode active" toggle
        SwitchPreference gameModePreference = (SwitchPreference) findPreference("htsr_game_mode_auto");
        boolean gameModeEnabled = mPrefs.getBoolean("htsr_game_mode_auto", false);
        gameModePreference.setChecked(gameModeEnabled);
        gameModePreference.setOnPreferenceChangeListener(this);

        // Setup "Automatically enable HTSR when games added in gamespace app" toggle
        SwitchPreference gamespacePreference = (SwitchPreference) findPreference("htsr_game_gamespace_auto");
        boolean gamespaceEnabled = mPrefs.getBoolean("htsr_game_gamespace_auto", false);
        gamespacePreference.setChecked(gamespaceEnabled);
        gamespacePreference.setOnPreferenceChangeListener(this);

        // Start service if any auto condition might enable HTSR
        if (htsrEnabled || gameModeEnabled || gamespaceEnabled) {
            startTouchSamplingService(true);
        }

        // Find the VideoPreference (if any)
        videoPreference = (VideoPreference) findPreference("htsr_media");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (videoPreference != null) {
            videoPreference.restartVideo();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (HTSR_ENABLE_KEY.equals(preference.getKey())) {
            boolean isEnabled = (Boolean) newValue;
            mPrefs.edit().putBoolean(HTSR_STATE, isEnabled).apply();
            startTouchSamplingService(isEnabled);
            if (isEnabled) {
                showTouchSamplingNotification();
            } else {
                cancelTouchSamplingNotification();
            }
        } else if ("htsr_game_mode_auto".equals(preference.getKey())) {
            boolean isGameModeAuto = (Boolean) newValue;
            mPrefs.edit().putBoolean("htsr_game_mode_auto", isGameModeAuto).apply();
            // Reapply the service state
            boolean mainEnabled = mPrefs.getBoolean(HTSR_STATE, false);
            startTouchSamplingService(mainEnabled || isGameModeAuto || mPrefs.getBoolean("htsr_game_gamespace_auto", false));
        } else if ("htsr_game_gamespace_auto".equals(preference.getKey())) {
            boolean isGamespaceAuto = (Boolean) newValue;
            mPrefs.edit().putBoolean("htsr_game_gamespace_auto", isGamespaceAuto).apply();
            // Reapply the service state
            boolean mainEnabled = mPrefs.getBoolean(HTSR_STATE, false);
            startTouchSamplingService(mainEnabled || isGamespaceAuto || mPrefs.getBoolean("htsr_game_mode_auto", false));
        }
        return true;
    }

    private void startTouchSamplingService(boolean enable) {
        Intent serviceIntent = new Intent(getActivity(), TouchSamplingService.class);
        if (enable) {
            getActivity().startService(serviceIntent);
        } else {
            getActivity().stopService(serviceIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }

    private void showTouchSamplingNotification() {
        NotificationManager notificationManager = (NotificationManager)
                getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(getActivity(), NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.touch_sampling_mode_title))
                .setContentText(getString(R.string.touch_sampling_mode_notification))
                .setSmallIcon(R.drawable.ic_touch_sampling_tile)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setFlag(Notification.FLAG_NO_CLEAR, true)
                .build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void cancelTouchSamplingNotification() {
        NotificationManager notificationManager = (NotificationManager)
                getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
