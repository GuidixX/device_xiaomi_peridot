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

package org.lineageos.settings;

public class Constants {

    // Saturation
    public static final String KEY_SATURATION = "saturation";
    public static final String KEY_SATURATION_PREVIEW = "saturation_preview";

    /* Flashlight Brightness Settings */
    public static final String KEY_FLASHLIGHT_BRIGHTNESS = "flashlight_brightness_pref";
    public static final String FLASHLIGHT_BRIGHTNESS_NODE = "/sys/class/leds/led:torch_0/max_brightness";

    public static final int FLASHLIGHT_MIN_BRIGHTNESS = 10; // Min(10), Max(200)
    public static final int FLASHLIGHT_MAX_BRIGHTNESS = 200; // Min(10), Max(200)

}
