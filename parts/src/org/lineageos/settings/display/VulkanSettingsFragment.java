/*
 * Copyright (C) 2025 GuidixX
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.display;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import org.lineageos.settings.R;
import android.widget.Toast;

public class VulkanSettingsFragment extends PreferenceFragmentCompat {

    private static final String KEY_VULKAN_SWITCH = "vulkan_enable";
    private SwitchPreference mVulkanSwitch;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.vulkan_settings);

        mVulkanSwitch = findPreference(KEY_VULKAN_SWITCH);
        if (mVulkanSwitch != null) {
            mVulkanSwitch.setChecked(VulkanUtils.isVulkanEnabled());
            mVulkanSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                VulkanUtils.saveVulkanToggleState(enabled);
                Toast.makeText(getContext(), R.string.vulkan_reboot_required, Toast.LENGTH_LONG).show();
                return true;
            });
        }
    }
}
