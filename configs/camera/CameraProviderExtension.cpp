/*
 * Copyright (C) 2024 LibreMobileOS Foundation
 *
 * SPDX-License-Identifier: Apache-2.0
 */
#include "CameraProviderExtension.h"
#include <fstream>
#include <cmath>
#include <algorithm>
#define TORCH_BRIGHTNESS "brightness"
#define TORCH_MAX_BRIGHTNESS "max_brightness"
#define TOGGLE_SWITCH "/sys/devices/platform/soc/c42d000.qcom,spmi/spmi-0/0-01/c42d000.qcom,spmi:qcom,pmxr2230@1:qcom,flash_led@ee00/leds/led:switch_2/brightness"
static std::string kTorchLedPaths[] = {
        "/sys/devices/platform/soc/c42d000.qcom,spmi/spmi-0/0-01/c42d000.qcom,spmi:qcom,pmxr2230@1:qcom,flash_led@ee00/leds/led:torch_0",
        "/sys/devices/platform/soc/c42d000.qcom,spmi/spmi-0/0-01/c42d000.qcom,spmi:qcom,pmxr2230@1:qcom,flash_led@ee00/leds/led:torch_1",
        "/sys/devices/platform/soc/c42d000.qcom,spmi/spmi-0/0-01/c42d000.qcom,spmi:qcom,pmxr2230@1:qcom,flash_led@ee00/leds/led:torch_2",
        "/sys/devices/platform/soc/c42d000.qcom,spmi/spmi-0/0-01/c42d000.qcom,spmi:qcom,pmxr2230@1:qcom,flash_led@ee00/leds/led:torch_3",
};
/**
 * Write value to path and close file.
 */
template <typename T>
static void set(const std::string& path, const T& value) {
    std::ofstream file(path);
    file << value;
}
/**
 * Read value from the path and close file.
 */
template <typename T>
static T get(const std::string& path, const T& def) {
    std::ifstream file(path);
    T result;
    file >> result;
    return file.fail() ? def : result;
}
bool supportsTorchStrengthControlExt() {
    return true;
}

bool supportsSetTorchModeExt() {
    return true;
}

int32_t getTorchDefaultStrengthLevelExt() {
    return 40; // Adjusted to represent 100% of default max brightness
}
int32_t getTorchMaxStrengthLevelExt() {
    auto node = kTorchLedPaths[0] + "/" + TORCH_MAX_BRIGHTNESS;
    return get(node, 40); // Fallback to 40 if read fails
}
int32_t getTorchStrengthLevelExt() {
    auto node = kTorchLedPaths[0] + "/" + TORCH_BRIGHTNESS;
    return get(node, 0);
}
void setTorchStrengthLevelExt(int32_t torchStrength) {
    int32_t maxBrightness = getTorchMaxStrengthLevelExt();
    float percentageValue = static_cast<float>(torchStrength);
    int32_t realValue = static_cast<int32_t>(std::floor((percentageValue / 100.0f) * maxBrightness));
    realValue = std::max(realValue, 1);

    ALOGD("FlashStrengthValue: %d", realValue);

    set(TOGGLE_SWITCH, 0);
    for (auto& path : kTorchLedPaths) {
        auto node = path + "/" + TORCH_BRIGHTNESS;
        std::ofstream file(node);
        if (file) {
            file << realValue;
            if (file.fail()) {
                ALOGE("TorchBrightnessSetter: Failed to write to %s", node.c_str());
            } else {
                ALOGD("TorchBrightnessSetter: Successfully wrote '%d' to %s", realValue, node.c_str());
            }
        } else {
            ALOGE("TorchBrightnessSetter: Failed to open %s for writing", node.c_str());
        }
    }
    if (realValue > 0)
        set(TOGGLE_SWITCH, 255);
}
