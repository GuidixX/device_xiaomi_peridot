/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <cstdint>
#include <string>
#include <vector>

class LightCalibration {
  public:
    LightCalibration();

    bool valid() const { return mValid; }
    float toLux(float als, float ir, int32_t brightness) const;
    float leakageAls(int32_t brightness) const;
    float leakageIr(int32_t brightness) const;

  private:
    struct LeakageRow {
        float brightness;
        float als;
        float ir;
    };

    float interpolate(int32_t brightness, bool ir) const;
    bool loadLeakageTable(const std::string& path);
    bool loadCoefficients(const std::string& dir);
    std::vector<LeakageRow> mLeakage;
    float mCoef0A = 0.f, mCoef1A = 0.f, mDgfA = 0.f;
    float mCoef0B = 0.f, mCoef1B = 0.f, mDgfB = 0.f;
    float mIrThreshold = 0.f;
    float mScale = 1.f;
    bool mValid = false;
};
