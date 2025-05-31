/*
 * Copyright (C) 2021 crDroid Android Project
 * Copyright (C) 2021 Chaldeaprjkt
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

package org.lineageos.settings.refreshrate;

import android.content.Context;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RefreshTileService extends TileService {
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";
    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";

    private Context context;
    private Tile tile;

    private static final int[][] REFRESH_RATES = {
        {60, 60},    // 60 Hz
        {90, 90},    // 90 Hz
        {120, 120},  // 120 Hz
        {60, 90},    // 60-90 Hz
        {90, 120},   // 90-120 Hz
        {60, 120}    // 60-120 Hz
    };
    private int currentRateIndex = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        syncFromSettings();
    }

    private void syncFromSettings() {
        float minRate = Settings.System.getFloat(context.getContentResolver(), KEY_MIN_REFRESH_RATE, 60);
        float maxRate = Settings.System.getFloat(context.getContentResolver(), KEY_PEAK_REFRESH_RATE, 120);
        
        for (int i = 0; i < REFRESH_RATES.length; i++) {
            if (REFRESH_RATES[i][0] == minRate && REFRESH_RATES[i][1] == maxRate) {
                currentRateIndex = i;
                break;
            }
        }
    }

    private void cycleRefreshRate() {
        currentRateIndex = (currentRateIndex + 1) % REFRESH_RATES.length;
        
        float minRate = REFRESH_RATES[currentRateIndex][0];
        float maxRate = REFRESH_RATES[currentRateIndex][1];
        
        Settings.System.putFloat(context.getContentResolver(), KEY_MIN_REFRESH_RATE, minRate);
        Settings.System.putFloat(context.getContentResolver(), KEY_PEAK_REFRESH_RATE, maxRate);
    }

    private void updateTileView() {
        String displayText;
        int min = REFRESH_RATES[currentRateIndex][0];
        int max = REFRESH_RATES[currentRateIndex][1];

        if (min == max) {
            displayText = String.format(Locale.US, "%d Hz", min);
        } else {
            displayText = String.format(Locale.US, "%d - %d Hz", min, max);
        }

        tile.setContentDescription(displayText);
        tile.setSubtitle(displayText);
        tile.setState(min == max ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        tile = getQsTile();
        syncFromSettings();
        updateTileView();
    }

    @Override
    public void onClick() {
        super.onClick();
        cycleRefreshRate();
        syncFromSettings();
        updateTileView();
    }
}
