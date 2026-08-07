/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <atomic>
#include <cstdint>
#include <string>
#include <vector>

class LightCalibration {
  public:
    LightCalibration();
    struct Region {
        int32_t left = 0;
        int32_t top = 0;
        int32_t right = 0;
        int32_t bottom = 0;
        float weight = 0.f;
    };

    struct CwbInfo {
        bool valid = false;
        int32_t centreX = 0;
        int32_t centreY = 0;
        float powLow = 0.f;
        float powHigh = 0.f;
        std::vector<Region> regions;
    };

    bool valid() const { return mValid; }
    float toLux(float als, float ir, int32_t brightness) const;
    void setContentLevel(float level) { mContentLevel.store(level); }
    const CwbInfo& cwbInfo() const { return mCwb; }
    float panelGamma(int32_t brightness) const;
    float leakageAls(int32_t brightness) const;
    float leakageIr(int32_t brightness) const;

  private:
    struct LeakageRow {
        float brightness;
        float als;
        float ir;
    };

    float interpolate(const std::vector<LeakageRow>& table, int32_t brightness, bool ir) const;
    float leakage(int32_t brightness, bool ir) const;
    bool loadLeakageTable(const std::string& path, const std::string& key,
                          std::vector<LeakageRow>* out);
    bool loadCoefficients(const std::string& dir);
    bool loadConfigCoefficients(const std::string& path);
    void loadCwbInfo(const std::string& path);
    std::vector<LeakageRow> mLeakage;
    std::vector<LeakageRow> mFullWhite;
    CwbInfo mCwb;
    std::atomic<float> mContentLevel{-1.f};
    float mCoef0A = 0.f, mCoef1A = 0.f, mDgfA = 0.f;
    float mCoef0B = 0.f, mCoef1B = 0.f, mDgfB = 0.f;
    float mIrThreshold = 0.f;
    float mScale = 1.f;
    bool mValid = false;
};
