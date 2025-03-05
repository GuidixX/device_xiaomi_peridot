/*
 * Copyright (C) 2025 Android Open Source Project
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

package org.lineageos.settings.turbocharging;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.MainSwitchPreference;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class TurboChargingFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "TurboChargingFragment";
    private static final String CHARGE_CURRENT_FILE = "/sys/devices/platform/soc/soc:qcom,pmic_glink/soc:qcom,pmic_glink:qcom,battery_charger/power_supply/battery/constant_charge_current";
    private static final String USB_ONLINE_FILE = "/sys/class/power_supply/usb/online";
    private static final String PREF_TURBO_ENABLED = "turbo_enable";
    private static final String PREF_TURBO_CURRENT = "turbo_current";

    private SwitchPreferenceCompat mTurboEnabled;
    private ListPreference mTurboCurrent;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.charging_panel, rootKey);

        mTurboEnabled = (SwitchPreferenceCompat) findPreference(PREF_TURBO_ENABLED);
        mTurboEnabled.setOnPreferenceChangeListener(this);

        mTurboCurrent = (ListPreference) findPreference(PREF_TURBO_CURRENT);
        mTurboCurrent.setOnPreferenceChangeListener(this);
        mTurboCurrent.setEnabled(mTurboEnabled.isChecked());

        if (!FileUtils.isFileWritable(CHARGE_CURRENT_FILE)) {
            mTurboEnabled.setEnabled(false);
            mTurboCurrent.setEnabled(false);
            Log.e(TAG, "No write access to " + CHARGE_CURRENT_FILE);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTurboEnabled) {
            boolean enabled = (boolean) newValue;
            mTurboCurrent.setEnabled(enabled);
            updateChargeCurrent();
            return true;
        } else if (preference == mTurboCurrent) {
            String value = (String) newValue;
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putString(PREF_TURBO_CURRENT, value)
                    .apply();
            updateChargeCurrent();
            return true;
        }
        return false;
    }

    private void updateChargeCurrent() {
        boolean turboEnabled = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREF_TURBO_ENABLED, false);
        Log.i(TAG, "isTurbo=" + turboEnabled);
        String defaultValue = "6000000";
        if (turboEnabled) {
            String currentValue = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(PREF_TURBO_CURRENT, "9750000");
            Log.i(TAG, "currentValue=" + currentValue);
            writeChargeCurrent(currentValue);
        } else {
            writeChargeCurrent(defaultValue);
        }
    }

    private void writeChargeCurrent(String value) {
        try {
            Integer.parseInt(value);
            if (!FileUtils.isFileWritable(CHARGE_CURRENT_FILE)) {
                Log.e(TAG, "File is not writable: " + CHARGE_CURRENT_FILE);
                return;
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(CHARGE_CURRENT_FILE))) {
                writer.write(value);
                writer.flush();
                Log.i(TAG, "Updated Charging current to " + value);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid charge current value: " + value, e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to update charge current", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!FileUtils.isFileWritable(CHARGE_CURRENT_FILE)) {
            mTurboEnabled.setEnabled(false);
            mTurboCurrent.setEnabled(false);
            Log.e(TAG, "No write access to " + CHARGE_CURRENT_FILE);
        } else {
            updateChargeCurrent();
        }
    }
}
