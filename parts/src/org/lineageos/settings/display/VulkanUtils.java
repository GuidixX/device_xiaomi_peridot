/*
 * Copyright (C) 2025 GuidixX
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.display;

import android.os.SystemProperties;

public class VulkanUtils {
    private static final String PROP_PERSIST = "persist.sys.vulkan";

    public static boolean isVulkanEnabled() {
        return "true".equals(SystemProperties.get(PROP_PERSIST, "true"));
    }

    public static void saveVulkanToggleState(boolean enabled) {
        SystemProperties.set(PROP_PERSIST, enabled ? "true" : "false");
    }
}
