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
 * limitations under the License
 */

package org.lineageos.settings.flashlight;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class FlashlightTileService extends TileService {

    private static final String TAG = "FlashlightTileService";
    private static final int[] BRIGHTNESS_LEVELS = {0, 25, 50, 75, 100};
    private static final String PREF_KEY_FLASHLIGHT_STATE = "flashlight_tile_state";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        if (tile == null) return;

        int currentLevel = getCurrentLevel();
        int nextLevel = getNextLevel(currentLevel);

        FlashlightUtils.applyBrightness(getApplicationContext(), nextLevel);

        setCurrentLevel(nextLevel);
        updateTile();
    }

    private int getCurrentLevel() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getInt(PREF_KEY_FLASHLIGHT_STATE, 0);
    }

    private void setCurrentLevel(int level) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(PREF_KEY_FLASHLIGHT_STATE, level).apply();
    }

    private int getNextLevel(int currentLevel) {
        int index = 0;
        for (int i = 0; i < BRIGHTNESS_LEVELS.length; i++) {
            if (BRIGHTNESS_LEVELS[i] == currentLevel) {
                index = i;
                break;
            }
        }
        index = (index + 1) % BRIGHTNESS_LEVELS.length;
        return BRIGHTNESS_LEVELS[index];
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        int currentLevel = getCurrentLevel();
        tile.setLabel("Flashlight " + currentLevel + "%");
        tile.setState(currentLevel == 0 ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
