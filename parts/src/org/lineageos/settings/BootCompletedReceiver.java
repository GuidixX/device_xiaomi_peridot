/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

package org.lineageos.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;

import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.doze.PocketService;
import org.lineageos.settings.soundcontrol.SoundControlUtils;
import org.lineageos.settings.chargecontrol.ChargeControlService;

import androidx.preference.PreferenceManager;


public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "XiaomiParts";
    private static final boolean DEBUG = true;
    private static final String DC_DIMMING_ENABLE_KEY = "dc_dimming_enable";
    private static final String DC_DIMMING_NODE = "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/disp_param";

    private SharedPreferences sharedPrefs;

    @Override
    public void onReceive(final Context context, Intent intent) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (DEBUG) Log.i(TAG, "Received intent: " + intent.getAction());
        switch (intent.getAction()) {
            case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                handleLockedBootCompleted(context);
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                handleBootCompleted(context);
                break;
        }
    }

    private void handleLockedBootCompleted(Context context) {
        if (DEBUG) Log.i(TAG, "Handling locked boot completed.");
        try {
            // Start necessary services
            startServices(context);

        } catch (Exception e) {
            Log.e(TAG, "Error during locked boot completed", e);
        }
    }

    private void handleBootCompleted(Context context) {
        if (DEBUG) Log.i(TAG, "Handling boot completed.");
        SoundControlUtils.applyAll(context);
        // Add additional boot-completed actions if needed
    }

    private void startServices(Context context) {
        if (DEBUG) Log.i(TAG, "Starting services...");

        // Start Pocket Mode Service
        PocketService.startService(context);

        // DC Dimming
        boolean dcDimmingEnabled = sharedPrefs.getBoolean(DC_DIMMING_ENABLE_KEY, false);
        FileUtils.writeLine(DC_DIMMING_NODE, dcDimmingEnabled ? "08 01" : "08 00");

        // Start Charge Control Service
        context.startServiceAsUser(new Intent(context, ChargeControlService.class), UserHandle.CURRENT);

    }
}
