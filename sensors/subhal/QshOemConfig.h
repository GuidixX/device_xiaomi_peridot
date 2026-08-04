/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <cstdint>
#include <mutex>
#include <string>

class QshOemConfig {
  public:
    enum ConfigType : int32_t {
        TYPE_COLOR = 0,
        LUX_SCALE = 1,
        PS_THRESHOLD = 2,
        PS_FORCE_CALI = 3,
        BIAS = 4,
        BACKLIGHT = 5,
        DC_STATE = 6,
        BRIGHTNESS_PARSE_REULT_RGB = 7,
        DISPLAY_FREQ = 8,
        CHANNEL_CALI = 9,
        HIGHLCD = 10,
        LOWLCD = 11,
        PRX2LUX = 12,
        CCT_SCREEN_CALI = 13,
        HWC = 14,
        BOLED_DATA = 15,
        MBIST = 16,
    };

    static QshOemConfig& getInstance();
    bool notifyBacklight(int32_t brightness);
    bool notifyDcState(int32_t state);
    bool notifyDisplayFreq(uint32_t freq);
    bool reportValue(float value, float aux);

  private:
    QshOemConfig();
    ~QshOemConfig();
    QshOemConfig(const QshOemConfig&) = delete;
    QshOemConfig& operator=(const QshOemConfig&) = delete;
    bool ensureReady();
    bool lookupSuid();
    bool send(int32_t type, const std::string& oemConfig);
    std::mutex mMutex;
    bool mReady = false;
    void* mConnection = nullptr;
    void* mLookup = nullptr;
    uint64_t mSuidLow = 0;
    uint64_t mSuidHigh = 0;
};
