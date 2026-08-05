/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include "LightCalibration.h"

#include <android-base/file.h>
#include <android-base/logging.h>
#include <android-base/strings.h>

#include <dirent.h>
#include <algorithm>
#include <cstdlib>

namespace {

constexpr char kCaliJson[] = "/mnt/vendor/persist/sensors/lightSensorCali.json";
constexpr char kRegistryDir[] = "/mnt/vendor/persist/sensors/registry/registry";
constexpr float kLuxDivisor = 25600.0f;

bool findFloat(const std::string& blob, const std::string& key, float* out) {
    size_t k = blob.find("\"" + key + "\"");
    if (k == std::string::npos) {
        return false;
    }
    size_t d = blob.find("\"data\"", k);
    if (d == std::string::npos) {
        return false;
    }
    size_t q1 = blob.find('"', d + 6);
    if (q1 == std::string::npos) {
        return false;
    }
    size_t q2 = blob.find('"', q1 + 1);
    if (q2 == std::string::npos) {
        return false;
    }
    *out = strtof(blob.substr(q1 + 1, q2 - q1 - 1).c_str(), nullptr);
    return true;
}

}  // namespace

LightCalibration::LightCalibration() {
    if (!loadLeakageTable(kCaliJson)) {
        LOG(ERROR) << "no panel leakage table, light sensor will be uncompensated";
        return;
    }
    if (!loadCoefficients(kRegistryDir)) {
        LOG(ERROR) << "no ALS coefficients in the sensor registry";
        return;
    }

    mValid = true;
    LOG(INFO) << "calibration loaded: " << mLeakage.size() << " leakage rows, coef_0_a=" << mCoef0A
              << " dgf_a=" << mDgfA << " ir_thr=" << mIrThreshold << " scale=" << mScale;
}

bool LightCalibration::loadLeakageTable(const std::string& path) {
    std::string blob;
    if (!android::base::ReadFileToString(path, &blob)) {
        LOG(ERROR) << "failed to read " << path;
        return false;
    }

    size_t start = blob.find("\"panel_Info_cali\"");
    if (start == std::string::npos) {
        return false;
    }
    size_t arr = blob.find('[', start);
    size_t end = blob.find(']', arr);
    if (arr == std::string::npos || end == std::string::npos) {
        return false;
    }

    for (size_t pos = arr; pos < end;) {
        size_t q1 = blob.find('"', pos);
        if (q1 == std::string::npos || q1 > end) {
            break;
        }
        size_t q2 = blob.find('"', q1 + 1);
        if (q2 == std::string::npos || q2 > end) {
            break;
        }
        auto parts = android::base::Split(blob.substr(q1 + 1, q2 - q1 - 1), ",");
        if (parts.size() >= 3) {
            mLeakage.push_back({strtof(parts[0].c_str(), nullptr),
                                strtof(parts[1].c_str(), nullptr),
                                strtof(parts[2].c_str(), nullptr)});
        }
        pos = q2 + 1;
    }

    std::sort(mLeakage.begin(), mLeakage.end(),
              [](const LeakageRow& a, const LeakageRow& b) { return a.brightness < b.brightness; });
    return mLeakage.size() > 1;
}

bool LightCalibration::loadCoefficients(const std::string& dir) {
    std::unique_ptr<DIR, decltype(&closedir)> d(opendir(dir.c_str()), closedir);
    if (!d) {
        LOG(ERROR) << "failed to open " << dir;
        return false;
    }

    std::string customCal, facCal;
    while (struct dirent* e = readdir(d.get())) {
        std::string name(e->d_name);
        if (name.find("als.custom_cal") != std::string::npos) {
            customCal = dir + "/" + name;
        } else if (name.find("als.fac_cal") != std::string::npos) {
            facCal = dir + "/" + name;
        }
    }

    std::string blob;
    if (customCal.empty() || !android::base::ReadFileToString(customCal, &blob)) {
        return false;
    }

    bool ok = findFloat(blob, "coef_0_a", &mCoef0A) && findFloat(blob, "coef_1_a", &mCoef1A) &&
              findFloat(blob, "dgf_a", &mDgfA) && findFloat(blob, "coef_0_b", &mCoef0B) &&
              findFloat(blob, "coef_1_b", &mCoef1B) && findFloat(blob, "dgf_b", &mDgfB) &&
              findFloat(blob, "ir_thr", &mIrThreshold);
    if (!ok) {
        LOG(ERROR) << "custom_cal is missing coefficients";
        return false;
    }

    if (!facCal.empty() && android::base::ReadFileToString(facCal, &blob)) {
        findFloat(blob, "scale", &mScale);
    }
    return true;
}

float LightCalibration::interpolate(int32_t brightness, bool ir) const {
    if (mLeakage.empty()) {
        return 0.f;
    }

    const float dbv = static_cast<float>(brightness);
    const LeakageRow& first = mLeakage.front();
    const LeakageRow& last = mLeakage.back();
    if (dbv <= first.brightness) {
        return ir ? first.ir : first.als;
    }
    if (dbv >= last.brightness) {
        return ir ? last.ir : last.als;
    }

    for (size_t i = 1; i < mLeakage.size(); i++) {
        const LeakageRow& hi = mLeakage[i];
        if (dbv > hi.brightness) {
            continue;
        }
        const LeakageRow& lo = mLeakage[i - 1];
        const float span = hi.brightness - lo.brightness;
        if (span <= 0.f) {
            return ir ? lo.ir : lo.als;
        }
        const float t = (dbv - lo.brightness) / span;
        return ir ? lo.ir + (hi.ir - lo.ir) * t : lo.als + (hi.als - lo.als) * t;
    }
    return ir ? last.ir : last.als;
}

float LightCalibration::leakageAls(int32_t brightness) const {
    return interpolate(brightness, false);
}

float LightCalibration::leakageIr(int32_t brightness) const {
    return interpolate(brightness, true);
}

float LightCalibration::toLux(float als, float ir, int32_t brightness) const {
    if (!mValid) {
        return -1.f;
    }
    float alsNet = std::max(als - leakageAls(brightness), 0.f);
    float irNet = std::max(ir - leakageIr(brightness), 0.f);
    float ratio = als > 0.f ? (ir / als) : 0.f;
    bool useB = ratio > mIrThreshold;
    float coef0 = useB ? mCoef0B : mCoef0A;
    float coef1 = useB ? mCoef1B : mCoef1A;
    float dgf = useB ? mDgfB : mDgfA;
    float lux = (coef0 * alsNet + coef1 * irNet) / kLuxDivisor * dgf * mScale;
    return std::max(lux, 0.f);
}
