/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include "LightCalibration.h"

#include <atomic>
#include <cstdint>
#include <mutex>
#include <vector>

class CwbCapture {
  public:
    static CwbCapture& getInstance();
    bool request();
    void configure(const LightCalibration::CwbInfo& info, float gamma);
    float contentLevel() const { return mContentLevel.load(); }
    void onFrame();
    void onError(int error);

  private:
    CwbCapture() = default;
    ~CwbCapture() = default;

    CwbCapture(const CwbCapture&) = delete;
    CwbCapture& operator=(const CwbCapture&) = delete;
    bool ensureReady();
    std::mutex mMutex;
    bool mConfigured = false;
    int32_t mCentreX = 0;
    int32_t mCentreY = 0;
    float mGamma = 2.2f;
    std::vector<LightCalibration::Region> mRegions;
    int32_t mRoiLeft = 0, mRoiTop = 0, mRoiW = 0, mRoiH = 0;
    bool mReady = false;
    bool mPending = false;
    bool mComplained = false;
    bool mSubmitted = false;
    int mPendingTicks = 0;
    std::atomic<float> mContentLevel{-1.f};
};
