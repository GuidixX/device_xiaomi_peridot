/*
 * Copyright (C) 2025 GuidixX
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

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;

public class TurboChargingTileService extends TileService {
    private static final String TAG = "TurboChargingTile";
    private static final String PREF_TURBO_ENABLED = "turbo_enable";
    private static final String PREF_TURBO_CURRENT = "turbo_current";

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean isEnabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_TURBO_ENABLED, false);
        
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(PREF_TURBO_ENABLED, !isEnabled)
                .apply();

        updateTileState();
        
        // Restart Service After Applying Changes
        Intent serviceIntent = new Intent(this, TurboChargingService.class);
        stopService(serviceIntent);
        startService(serviceIntent);
    }

    private void updateTileState() {
        boolean isEnabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_TURBO_ENABLED, false);
        String currentValue = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PREF_TURBO_CURRENT, "9750000");

        Tile tile = getQsTile();
        if (tile == null) return;

        tile.setState(isEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.turbo_charge_tile_label));
        tile.setContentDescription(getString(R.string.turbo_charge_tile_content_description));

        String subtitle = getPowerString(currentValue);
        tile.setSubtitle(subtitle);
        
        tile.updateTile();
    }

    private String getPowerString(String value) {
        switch (value) {
            case "3500000":
                return getString(R.string.turbo_mode_one);
            case "6000000":
                return getString(R.string.turbo_mode_two);
            case "9750000":
                return getString(R.string.turbo_mode_three);
            default:
                return getString(R.string.tile_off);
        }
    }
}
