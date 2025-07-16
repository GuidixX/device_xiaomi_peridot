/*
 * Copyright (C) 2025 GuidixX
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.display;

import android.os.Bundle;
import org.lineageos.settings.R;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class VulkanSettingsActivity extends CollapsingToolbarBaseActivity {
    private static final String TAG_VULKAN = "vulkan";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(
            com.android.settingslib.collapsingtoolbar.R.id.content_frame,
            new VulkanSettingsFragment(), TAG_VULKAN).commit();
    }
}
