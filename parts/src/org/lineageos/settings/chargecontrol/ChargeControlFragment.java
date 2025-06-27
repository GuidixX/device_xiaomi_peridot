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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;

import org.lineageos.settings.Constants;
import org.lineageos.settings.CustomSeekBarPreference;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class ChargeControlFragment extends PreferenceFragmentCompat
        implements OnCheckedChangeListener, Preference.OnPreferenceChangeListener {

    private MainSwitchPreference mChargeControlSwitch;

    private CustomSeekBarPreference mStopChargingPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.charge_control, rootKey);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        mChargeControlSwitch = findPreference(Constants.KEY_CHARGE_CONTROL);
        mChargeControlSwitch.setChecked(sharedPrefs.getBoolean(Constants.KEY_CHARGE_CONTROL, false));
        mChargeControlSwitch.addOnSwitchChangeListener(this);

        mStopChargingPreference = findPreference(Constants.KEY_STOP_CHARGING);
        if (FileUtils.isFileWritable(Constants.NODE_STOP_CHARGING)) {
            mStopChargingPreference.setValue(sharedPrefs.getInt(Constants.KEY_STOP_CHARGING,
                    Integer.parseInt(FileUtils.readOneLine(Constants.NODE_STOP_CHARGING) != null ? FileUtils.readOneLine(Constants.NODE_STOP_CHARGING) : Constants.DEFAULT_STOP_CHARGING)));
            mStopChargingPreference.setOnPreferenceChangeListener(this);
        } else {
            mStopChargingPreference.setSummary(getString(R.string.kernel_node_access_error));
            mStopChargingPreference.setEnabled(false);
        }
        mStopChargingPreference.setVisible(mChargeControlSwitch.isChecked());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor prefChange = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();

        prefChange.putBoolean(Constants.KEY_CHARGE_CONTROL, isChecked).apply();

        mStopChargingPreference.setVisible(isChecked);

        if (!isChecked) {
            FileUtils.writeLine(Constants.NODE_STOP_CHARGING, "0");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mStopChargingPreference) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int value = Integer.parseInt(newValue.toString());
            sharedPrefs.edit().putInt(Constants.KEY_STOP_CHARGING, value).apply();
            mStopChargingPreference.refresh(value);
            Toast.makeText(getContext(), getString(R.string.stop_charging_set_to, value), Toast.LENGTH_SHORT).show();

            boolean enabled = sharedPrefs.getBoolean(Constants.KEY_CHARGE_CONTROL, false);
            if (enabled) {
                android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
                android.content.Intent batteryStatus = getContext().registerReceiver(null, ifilter);
                if (batteryStatus != null) {
                    int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
                    int percent = (int) ((level / (float) scale) * 100);
                    if (percent >= value) {
                        FileUtils.writeLine(Constants.NODE_STOP_CHARGING, "1");
                    } else {
                        FileUtils.writeLine(Constants.NODE_STOP_CHARGING, "0");
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static void restoreStopChargingSetting(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean chargeControlEnabled = sharedPrefs.getBoolean(Constants.KEY_CHARGE_CONTROL, true);

        if (chargeControlEnabled && FileUtils.isFileWritable(Constants.NODE_STOP_CHARGING)) {
            int value = sharedPrefs.getInt(Constants.KEY_STOP_CHARGING,
                    Integer.parseInt(FileUtils.readOneLine(Constants.NODE_STOP_CHARGING) != null ? FileUtils.readOneLine(Constants.NODE_STOP_CHARGING) : Constants.DEFAULT_STOP_CHARGING));
            FileUtils.writeLine(Constants.NODE_STOP_CHARGING, "0");
        }
    }
}
