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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.lineageos.settings.R;

public class TouchSamplingTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        SharedPreferences prefs = getSharedPreferences(TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(TouchSamplingSettingsFragment.HTSR_STATE, false);
        prefs.edit().putBoolean(TouchSamplingSettingsFragment.HTSR_STATE, !enabled).apply();
        // Start or stop the service as needed
        Intent serviceIntent = new Intent(this, TouchSamplingService.class);
        if (!enabled) {
            startService(serviceIntent);
        } else {
            stopService(serviceIntent);
        }
        updateTileState();
    }

    private void updateTileState() {
        SharedPreferences prefs = getSharedPreferences(TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(TouchSamplingSettingsFragment.HTSR_STATE, false);
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setLabel(getString(R.string.touch_sampling_mode_title));
            tile.setSubtitle(getString(enabled ? R.string.tile_on : R.string.tile_off));
            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_touch_sampling_tile));
            tile.updateTile();
        }
    }
}
