/*
 * Copyright (C) 2021 chaldeaprjkt
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
import android.provider.Settings;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.lang.Math;

import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.Constants;

public final class FlashlightUtils {

    final static String PREF_BRIGHTNESS = "flashlight_brightness_pref";
    final static String PATH_BRIGHTNESS = "/sys/class/leds/led:torch_0/max_brightness";

    final static int MIN_BRIGHTNESS = 10;

    public static int getTorchMaxBrightness() {
        if (FileUtils.fileExists(PATH_BRIGHTNESS)) {
            String s = FileUtils.readOneLine(PATH_BRIGHTNESS);
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception e) {
            }
        }
        return 255; // Max Brightness
    }

    public static void applyBrightness(Context context, int percent) {
        if (FileUtils.fileExists(PATH_BRIGHTNESS)) {
            int maxBrightness = getTorchMaxBrightness();
            // (max - MIN_BRIGHTNESS) + MIN_BRIGHTNESS
            int newValue = (int) Math.floor((percent / 100.0) * (maxBrightness - MIN_BRIGHTNESS)) + MIN_BRIGHTNESS;
            newValue = Math.min(newValue, maxBrightness);
            if (newValue > Constants.FLASHLIGHT_MAX_BRIGHTNESS) {
                FileUtils.writeLine(Constants.FLASHLIGHT_BRIGHTNESS_NODE, String.valueOf(Constants.FLASHLIGHT_MAX_BRIGHTNESS));
            } else if (newValue < Constants.FLASHLIGHT_MIN_BRIGHTNESS) {
                FileUtils.writeLine(Constants.FLASHLIGHT_BRIGHTNESS_NODE, String.valueOf(Constants.FLASHLIGHT_MIN_BRIGHTNESS));
            } else {
                FileUtils.writeLine(Constants.FLASHLIGHT_BRIGHTNESS_NODE, String.valueOf(newValue));
            }
        }
    }

    public static void restoreBrightness(Context context) {
        int percent = Settings.Secure.getInt(
            context.getContentResolver(), PREF_BRIGHTNESS, 150);
        applyBrightness(context, percent);
    }
}
