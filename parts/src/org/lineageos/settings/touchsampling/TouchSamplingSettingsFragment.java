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
import androidx.preference.SwitchPreferenceCompat;

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

    private SwitchPreferenceCompat mHTSRPreference;
    private SharedPreferences mPrefs;
    private VideoPreference videoPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.htsr_settings);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        mPrefs = getActivity().getSharedPreferences(SHAREDHTSR, Context.MODE_PRIVATE);

        // Set the initial state of the main toggle
        mHTSRPreference = (SwitchPreferenceCompat) findPreference(HTSR_ENABLE_KEY);
        boolean htsrEnabled = mPrefs.getBoolean(HTSR_STATE, false);
        mHTSRPreference.setChecked(htsrEnabled);
        mHTSRPreference.setOnPreferenceChangeListener(this);

        // Setup the new auto-enable for selected apps toggle
        SwitchPreferenceCompat autoEnableSelectedAppsPref = (SwitchPreferenceCompat) findPreference("htsr_auto_enable_selected_apps");
        boolean autoEnableSelectedApps = mPrefs.getBoolean("htsr_auto_enable_selected_apps", true);
        autoEnableSelectedAppsPref.setChecked(autoEnableSelectedApps);
        autoEnableSelectedAppsPref.setOnPreferenceChangeListener(this);

        // Find the VideoPreference (if any)
        videoPreference = (VideoPreference) findPreference("htsr_media");

        // Wire up app selector/remover
        Preference perAppConfigPref = findPreference("htsr_per_app_config");
        if (perAppConfigPref != null) {
            perAppConfigPref.setOnPreferenceClickListener(pref -> {
                startActivity(new android.content.Intent(getContext(), TouchSamplingPerAppConfigActivity.class));
                return true;
            });
        }

        // Start service if main toggle or auto-enable for selected apps is ON
        if (htsrEnabled || autoEnableSelectedApps) {
            startTouchSamplingService(true);
        }
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
        } else if ("htsr_auto_enable_selected_apps".equals(preference.getKey())) {
            boolean isAutoEnableSelectedApps = (Boolean) newValue;
            mPrefs.edit().putBoolean("htsr_auto_enable_selected_apps", isAutoEnableSelectedApps).apply();
            // Reapply the service state
            boolean mainEnabled = mPrefs.getBoolean(HTSR_STATE, false);
            startTouchSamplingService(mainEnabled || isAutoEnableSelectedApps);
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
}
