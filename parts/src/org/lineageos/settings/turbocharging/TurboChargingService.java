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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.UEventObserver;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.lineageos.settings.utils.FileUtils;

public class TurboChargingService extends Service {
    private static final String TAG = "TurboCharging";
    private static final String CHARGE_CURRENT_FILE = "/sys/devices/platform/soc/soc:qcom,pmic_glink/soc:qcom,pmic_glink:qcom,battery_charger/power_supply/battery/constant_charge_current";
    private static final String USB_ONLINE_FILE = "/sys/class/power_supply/usb/online";

    private UEventObserver mObserver;

    @Override
    public void onCreate() {
        Log.d(TAG, "Starting");

        if (!FileUtils.isFileWritable(CHARGE_CURRENT_FILE)) {
            Log.e(TAG, "No write access to " + CHARGE_CURRENT_FILE);
            stopSelf();
            return;
        }

        try {
            mObserver = new UEventObserver() {
                @Override
                public void onUEvent(UEvent event) {
                    try {
                        String chargerStatus = event.get("POWER_SUPPLY_ONLINE");
                        if (chargerStatus != null && chargerStatus.equals("1")) {
                            updateChargeCurrent();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in onUEvent", e);
                    }
                }
            };
            mObserver.startObserving("DEVPATH=/sys/class/power_supply/usb/online");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start UEventObserver", e);
            stopSelf();
            return;
        }

        updateChargeCurrent();
    }

    private void updateChargeCurrent() {
        try {
            boolean turboEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("turbo_enable", false);
            Log.i(TAG, "isTurbo=" + turboEnabled);
            String defaultValue = "2000000";
            if (turboEnabled) {
                String currentValue = PreferenceManager.getDefaultSharedPreferences(this).getString("turbo_current", "9750000");
                Log.i(TAG, "currentValue=" + currentValue);
                writeChargeCurrent(currentValue);
            } else {
                writeChargeCurrent(defaultValue);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateChargeCurrent", e);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
