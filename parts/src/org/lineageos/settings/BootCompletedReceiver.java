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
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;
import android.view.Display.HdrCapabilities;

import org.lineageos.settings.display.ColorModeService;
import org.lineageos.settings.doze.PocketService;
import org.lineageos.settings.refreshrate.RefreshUtils;
import org.lineageos.settings.turbocharging.TurboChargingService;
import org.lineageos.settings.touchsampling.TouchSamplingUtils;
import org.lineageos.settings.touchsampling.TouchSamplingService;
import org.lineageos.settings.touchsampling.TouchSamplingTileService;
import org.lineageos.settings.soundcontrol.SoundControlUtils;
import org.lineageos.settings.chargecontrol.ChargeControlService;
import org.lineageos.settings.touch.DoubleTapService;
import org.lineageos.settings.touch.SingleTapService;
import org.lineageos.settings.touch.SoFodTouchService;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "XiaomiParts";
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(final Context context, Intent intent) {
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

            // Override HDR types
            overrideHdrTypes(context);

            // Restore touch sampling rate
            TouchSamplingUtils.restoreSamplingValue(context);

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

        // Start Color Mode Service
        context.startServiceAsUser(new Intent(context, ColorModeService.class), UserHandle.CURRENT);

        // Start Refresh Rate Service
        RefreshUtils.startService(context);

        // Start Pocket Mode Service
        PocketService.startService(context);

        // Start TurboChargingService
        Intent turboChargingIntent = new Intent(context, TurboChargingService.class);
        context.startService(turboChargingIntent);

        // Start Touch Sampling Tile Service
        context.startServiceAsUser(new Intent(context, TouchSamplingTileService.class), UserHandle.CURRENT);

        // Start TouchSamplingService to restore sampling rate
        Intent touchSamplingServiceIntent = new Intent(context, TouchSamplingService.class);
        context.startServiceAsUser(touchSamplingServiceIntent, UserHandle.CURRENT);

        // Start Touch Sampling Service
        context.startServiceAsUser(new Intent(context, TouchSamplingService.class), UserHandle.CURRENT);

        // Start Charge Control Service
        context.startServiceAsUser(new Intent(context, ChargeControlService.class), UserHandle.CURRENT);

        // Start Touchfeatures service
        context.startServiceAsUser(new Intent(context, DoubleTapService.class), UserHandle.CURRENT);
        context.startServiceAsUser(new Intent(context, SoFodTouchService.class), UserHandle.CURRENT);

        // Start Single Tap Service
        context.startServiceAsUser(new Intent(context, SingleTapService.class), UserHandle.CURRENT);
    }

    private void overrideHdrTypes(Context context) {
        try {
            final DisplayManager dm = context.getSystemService(DisplayManager.class);
            if (dm != null) {
                dm.overrideHdrTypes(Display.DEFAULT_DISPLAY, new int[]{
                        HdrCapabilities.HDR_TYPE_HDR10,
                        HdrCapabilities.HDR_TYPE_HLG,
                        HdrCapabilities.HDR_TYPE_HDR10_PLUS
                });
                if (DEBUG) Log.i(TAG, "HDR types overridden successfully.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error overriding HDR types", e);
        }
    }
}
