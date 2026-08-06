/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#undef LOG_TAG
#define LOG_TAG "CwbCapture"

#include "CwbCapture.h"

#include <aidl/vendor/qti/hardware/display/config/BnDisplayConfigCallback.h>
#include <aidl/vendor/qti/hardware/display/config/IDisplayConfig.h>
#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <vndk/hardware_buffer.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <unistd.h>

using aidl::android::hardware::common::NativeHandle;
using aidl::vendor::qti::hardware::display::config::Attributes;
using aidl::vendor::qti::hardware::display::config::BnDisplayConfigCallback;
using aidl::vendor::qti::hardware::display::config::CameraSmoothOp;
using aidl::vendor::qti::hardware::display::config::Concurrency;
using aidl::vendor::qti::hardware::display::config::DisplayType;
using aidl::vendor::qti::hardware::display::config::IDisplayConfig;
using aidl::vendor::qti::hardware::display::config::Rect;
using aidl::vendor::qti::hardware::display::config::TUIEventType;

namespace {

constexpr char kServiceName[] = "vendor.qti.hardware.display.config.IDisplayConfig/default";
constexpr int32_t kPrimaryDisplay = static_cast<int32_t>(DisplayType::PRIMARY);
constexpr int kPendingTicks = 5;

const std::array<float, 256>& gammaLut(float gamma) {
    static std::array<float, 256> lut{};
    static float built = 0.f;
    if (built != gamma) {
        for (size_t i = 0; i < lut.size(); i++) {
            lut[i] = std::pow(static_cast<float>(i) / 255.f, gamma);
        }
        built = gamma;
    }
    return lut;
}

float weightFor(const std::vector<LightCalibration::Region>& regions, int32_t dx, int32_t dy) {
    for (const auto& r : regions) {
        if (dx >= -r.left && dx <= r.right && dy >= -r.top && dy <= r.bottom) {
            return r.weight;
        }
    }
    return 0.f;
}

class Callback : public BnDisplayConfigCallback {
  public:
    ::ndk::ScopedAStatus notifyCWBBufferDone(int32_t error, const NativeHandle&) override {
        if (error != 0) {
            CwbCapture::getInstance().onError(error);
        } else {
            CwbCapture::getInstance().onFrame();
        }
        return ::ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus notifyQsyncChange(bool, int32_t, int32_t) override {
        return ::ndk::ScopedAStatus::ok();
    }
    ::ndk::ScopedAStatus notifyIdleStatus(bool) override { return ::ndk::ScopedAStatus::ok(); }
    ::ndk::ScopedAStatus notifyCameraSmoothInfo(CameraSmoothOp, int32_t) override {
        return ::ndk::ScopedAStatus::ok();
    }
    ::ndk::ScopedAStatus notifyResolutionChange(int32_t, const Attributes&) override {
        return ::ndk::ScopedAStatus::ok();
    }
    ::ndk::ScopedAStatus notifyFpsMitigation(int32_t, const Attributes&, Concurrency) override {
        return ::ndk::ScopedAStatus::ok();
    }
    ::ndk::ScopedAStatus notifyTUIEventDone(int32_t, DisplayType, TUIEventType) override {
        return ::ndk::ScopedAStatus::ok();
    }
};

std::shared_ptr<IDisplayConfig> gService;
std::shared_ptr<Callback> gCallback;
AHardwareBuffer* gBuffer = nullptr;

}  // namespace

CwbCapture& CwbCapture::getInstance() {
    static CwbCapture instance;
    return instance;
}

void CwbCapture::configure(const LightCalibration::CwbInfo& info, float gamma) {
    std::lock_guard<std::mutex> lock(mMutex);
    mCentreX = info.centreX;
    mCentreY = info.centreY;
    mRegions = info.regions;
    mGamma = gamma;
    mConfigured = !mRegions.empty();
}

bool CwbCapture::ensureReady() {
    if (!mConfigured) {
        return false;
    }
    if (mReady) {
        return true;
    }

    ::ndk::SpAIBinder binder(AServiceManager_checkService(kServiceName));
    gService = IDisplayConfig::fromBinder(binder);
    if (gService == nullptr) {
        if (!mComplained) {
            mComplained = true;
            LOG(ERROR) << "display config service unreachable, no content compensation";
        }
        return false;
    }
    mComplained = false;

    if (gBuffer == nullptr) {
        const auto& outer = mRegions.back();
        mRoiLeft = std::max(mCentreX - outer.left, 0);
        mRoiTop = std::max(mCentreY - outer.top, 0);
        mRoiW = mCentreX + outer.right - mRoiLeft;
        mRoiH = mCentreY + outer.bottom - mRoiTop;

        AHardwareBuffer_Desc desc = {};
        desc.width = mRoiW;
        desc.height = mRoiH;
        desc.layers = 1;
        desc.format = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;
        desc.usage = AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN |
                     AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE |
                     AHARDWAREBUFFER_USAGE_COMPOSER_OVERLAY;
        if (AHardwareBuffer_allocate(&desc, &gBuffer) != 0 || gBuffer == nullptr) {
            LOG(ERROR) << "failed to allocate writeback buffer";
            return false;
        }
    }

    if (gCallback == nullptr) {
        gCallback = ::ndk::SharedRefBase::make<Callback>();
    }

    mReady = true;
    return true;
}

bool CwbCapture::request() {
    std::lock_guard<std::mutex> lock(mMutex);
    if (!ensureReady()) {
        return false;
    }
    if (mPending) {
        if (++mPendingTicks < kPendingTicks) {
            return true;
        }
        LOG(WARNING) << "no writeback after " << mPendingTicks << " requests, retrying";
        mPending = false;
    }
    mPendingTicks = 0;

    Rect roi;
    roi.left = mRoiLeft;
    roi.top = mRoiTop;
    roi.right = mRoiLeft + mRoiW;
    roi.bottom = mRoiTop + mRoiH;

    const native_handle_t* handle = AHardwareBuffer_getNativeHandle(gBuffer);
    if (handle == nullptr) {
        LOG(ERROR) << "writeback buffer has no handle";
        return false;
    }

    NativeHandle aidlHandle;
    aidlHandle.fds.reserve(handle->numFds);
    for (int i = 0; i < handle->numFds; i++) {
        aidlHandle.fds.emplace_back(::ndk::ScopedFileDescriptor(dup(handle->data[i])));
    }
    aidlHandle.ints = std::vector<int32_t>(handle->data + handle->numFds,
                                           handle->data + handle->numFds + handle->numInts);
    auto status = gService->setCWBOutputBuffer(gCallback, kPrimaryDisplay, roi,
                                               true /* postProcessed */, aidlHandle);
    if (!status.isOk()) {
        LOG(ERROR) << "setCWBOutputBuffer failed: " << status.getDescription();
        mReady = false;
        gService.reset();
        return false;
    }

    if (!mSubmitted) {
        mSubmitted = true;
        LOG(INFO) << "writeback requested for " << mRoiW << "x" << mRoiH << " at " << mRoiLeft
                  << "," << mRoiTop;
    }
    mPending = true;
    return true;
}

void CwbCapture::onError(int error) {
    LOG(WARNING) << "writeback failed: " << error;
    std::lock_guard<std::mutex> lock(mMutex);
    mPending = false;
}

void CwbCapture::onFrame() {
    std::lock_guard<std::mutex> lock(mMutex);
    mPending = false;
    if (gBuffer == nullptr) {
        return;
    }

    AHardwareBuffer_Desc desc = {};
    AHardwareBuffer_describe(gBuffer, &desc);

    void* mapped = nullptr;
    if (AHardwareBuffer_lock(gBuffer, AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN, -1, nullptr, &mapped) !=
                0 ||
        mapped == nullptr) {
        LOG(ERROR) << "failed to map writeback buffer";
        return;
    }

    const auto* pixels = static_cast<const uint8_t*>(mapped);
    const float cx = static_cast<float>(mCentreX - mRoiLeft);
    const float cy = static_cast<float>(mCentreY - mRoiTop);
    double sum = 0.0;
    double weight = 0.0;

    for (uint32_t y = 0; y < desc.height; y++) {
        const uint8_t* row = pixels + static_cast<size_t>(y) * desc.stride * 4;
        for (uint32_t x = 0; x < desc.width; x++) {
            const float w = weightFor(mRegions, static_cast<int32_t>(x) - static_cast<int32_t>(cx),
                                      static_cast<int32_t>(y) - static_cast<int32_t>(cy));
            if (w <= 0.f) {
                continue;
            }
            const auto& lut = gammaLut(mGamma);
            const float luma = 0.2126f * lut[row[x * 4 + 0]] + 0.7152f * lut[row[x * 4 + 1]] +
                               0.0722f * lut[row[x * 4 + 2]];
            sum += w * luma;
            weight += w;
        }
    }

    AHardwareBuffer_unlock(gBuffer, nullptr);

    if (weight <= 0.0) {
        return;
    }
    const float level = static_cast<float>(sum / weight);
    mContentLevel.store(std::min(std::max(level, 0.f), 1.f));
    LOG(VERBOSE) << "content level " << level;
}
