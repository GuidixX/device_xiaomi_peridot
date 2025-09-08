/*
 * Copyright (C) 2024 The LineageOS Project
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

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.touchsampling.TouchSamplingSettingsFragment;
import org.lineageos.settings.utils.FileUtils;

import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public final class TouchSamplingUtils {
    private static final String TAG = "TouchSamplingUtils";
    public static final String HTSR_FILE = "/sys/devices/platform/goodix_ts.0/switch_report_rate";
    private static final String HTSR_FOCALTECH_FILE = "/sys/bus/spi/drivers/focaltech_ts/spi1.0/switch_report_rate";

    /**
     * Writes HTSR value to both Goodix and Focaltech nodes if they exist and are writable
     */
    public static void writeHtsrValue(String value) {
        // Try Goodix first
        if (isFileWritable(HTSR_FILE)) {
            try {
                FileUtils.writeLine(HTSR_FILE, value);
            } catch (Exception e) {
                Log.w(TAG, "Failed to write to Goodix HTSR file: " + e.getMessage());
            }
        }

        // Try Focaltech as fallback
        if (isFileWritable(HTSR_FOCALTECH_FILE)) {
            try {
                FileUtils.writeLine(HTSR_FOCALTECH_FILE, value);
            } catch (Exception e) {
                Log.w(TAG, "Failed to write to Focaltech HTSR file: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if a file exists and is writable
     */
    private static boolean isFileWritable(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists() && file.canWrite();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reads HTSR value from available nodes (Goodix first, then Focaltech)
     */
    public static String readHtsrValue() {
        // Try Goodix first
        try {
            String value = FileUtils.readOneLine(HTSR_FILE);
            if (value != null) {
                return value;
            }
        } catch (Exception e) {
            // Silent fallback
        }

        // Try Focaltech as fallback
        try {
            String value = FileUtils.readOneLine(HTSR_FOCALTECH_FILE);
            if (value != null) {
                return value;
            }
        } catch (Exception e) {
            // Silent fallback
        }

        return null;
    }

    public static void restoreSamplingValue(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        int htsrState = sharedPref.getInt(TouchSamplingSettingsFragment.SHAREDHTSR, 0);
        writeHtsrValue(Integer.toString(htsrState));
    }

    /**
     * Returns the package name of the current foreground app.
     */
    private static String getForegroundApp(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty() && tasks.get(0).topActivity != null) {
                return tasks.get(0).topActivity.getPackageName();
            }
        }
        return null;
    }
}
