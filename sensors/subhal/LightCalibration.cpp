/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include "LightCalibration.h"

#include <android-base/file.h>
#include <android-base/logging.h>
#include <android-base/properties.h>
#include <android-base/strings.h>

#include <dirent.h>
#include <algorithm>
#include <cstdlib>

namespace {

constexpr char kCaliJson[] = "/mnt/vendor/persist/sensors/lightSensorCali.json";
constexpr char kConfigJson[] = "/odm/etc/sensors/config/lightSensorConfig.json";
constexpr char kConfigJsonSec[] = "/odm/etc/sensors/config/lightSensorConfigSec.json";
constexpr char kPanelInfo[] = "/sys/class/mi_display/disp-DSI-0/panel_info";
constexpr int32_t kGammaRampStart = 50;
constexpr char kMaxBrightnessProp[] = "ro.vendor.sensor.maxbrightness";
constexpr int32_t kMaxBrightnessDefault = 2047;
constexpr char kPrimaryPanel[] = "panel_name=mdss_dsi_n16t_42_02_0a_dsc_vid";

const char* configForPanel() {
    std::string info;
    if (!android::base::ReadFileToString(kPanelInfo, &info)) {
        LOG(WARNING) << "no panel info, assuming the primary panel";
        return kConfigJson;
    }
    const bool primary = info.find(kPrimaryPanel) != std::string::npos;
    LOG(INFO) << "panel is " << (primary ? "primary" : "secondary");
    return primary ? kConfigJson : kConfigJsonSec;
}
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

bool LightCalibration::loadConfigCoefficients(const std::string& path) {
    std::string blob;
    if (!android::base::ReadFileToString(path, &blob)) {
        return false;
    }
    size_t start = blob.find("\"channel_coef\"");
    if (start == std::string::npos) {
        return false;
    }
    size_t q1 = blob.find('"', blob.find("\"coef_data\"", start) + 11);
    size_t q2 = q1 == std::string::npos ? q1 : blob.find('"', q1 + 1);
    if (q2 == std::string::npos) {
        return false;
    }

    const auto parts = android::base::Split(blob.substr(q1 + 1, q2 - q1 - 1), ",");
    if (parts.size() < 7) {
        return false;
    }
    mCoef0A = strtof(parts[0].c_str(), nullptr);
    mCoef1A = strtof(parts[1].c_str(), nullptr);
    mDgfA = strtof(parts[2].c_str(), nullptr);
    mCoef0B = strtof(parts[3].c_str(), nullptr);
    mCoef1B = strtof(parts[4].c_str(), nullptr);
    mDgfB = strtof(parts[5].c_str(), nullptr);
    mIrThreshold = strtof(parts[6].c_str(), nullptr);
    return true;
}

LightCalibration::LightCalibration() {
    if (!loadLeakageTable(kCaliJson, "panel_Info_cali", &mLeakage)) {
        LOG(ERROR) << "no panel leakage table, light sensor will be uncompensated";
        return;
    }
    const std::string config = configForPanel();
    if (!loadLeakageTable(config, "panel_Info_cali_ori", &mFullWhite)) {
        LOG(WARNING) << "no full white leakage table, screen content will not be compensated";
    }
    loadCwbInfo(config);
    if (!loadCoefficients(kRegistryDir)) {
        LOG(WARNING) << "no ALS coefficients in the sensor registry, falling back to the "
                        "panel config";
        if (!loadConfigCoefficients(config)) {
            LOG(ERROR) << "no ALS coefficients anywhere, light sensor will not report";
            return;
        }
    }

    mValid = true;
    LOG(INFO) << "calibration loaded: " << mLeakage.size() << " leakage rows, coef_0_a=" << mCoef0A
              << " dgf_a=" << mDgfA << " ir_thr=" << mIrThreshold << " scale=" << mScale;
}

bool LightCalibration::loadLeakageTable(const std::string& path, const std::string& key,
                                        std::vector<LeakageRow>* out) {
    std::string blob;
    if (!android::base::ReadFileToString(path, &blob)) {
        LOG(ERROR) << "failed to read " << path;
        return false;
    }

    size_t start = blob.find("\"" + key + "\"");
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
            out->push_back({strtof(parts[0].c_str(), nullptr),
                            strtof(parts[1].c_str(), nullptr),
                            strtof(parts[2].c_str(), nullptr)});
        }
        pos = q2 + 1;
    }

    std::sort(out->begin(), out->end(),
              [](const LeakageRow& a, const LeakageRow& b) { return a.brightness < b.brightness; });
    return out->size() > 1;
}

void LightCalibration::loadCwbInfo(const std::string& path) {
    std::string blob;
    if (!android::base::ReadFileToString(path, &blob)) {
        return;
    }
    size_t start = blob.find("\"cwbInfo\"");
    if (start == std::string::npos) {
        return;
    }

    auto field = [&](const char* key) {
        std::vector<float> out;
        size_t k = blob.find(std::string("\"") + key + "\"", start);
        if (k == std::string::npos) {
            return out;
        }
        size_t q1 = blob.find('"', blob.find(':', k));
        size_t q2 = q1 == std::string::npos ? q1 : blob.find('"', q1 + 1);
        if (q2 == std::string::npos) {
            return out;
        }
        for (const auto& part : android::base::Split(blob.substr(q1 + 1, q2 - q1 - 1), ",")) {
            out.push_back(strtof(part.c_str(), nullptr));
        }
        return out;
    };

    const auto centre = field("Center");
    const auto pow = field("PowPara");
    const auto radius = field("Radius");
    const size_t count = radius.size() >= 7 ? (radius.size() - 3) / 4 : 0;
    if (centre.size() < 2 || pow.size() < 2 || count == 0 ||
        radius.size() != count * 4 + count) {
        LOG(WARNING) << "cwbInfo is incomplete, screen content will not be compensated";
        return;
    }

    for (size_t i = 0; i < count; i++) {
        const float stride = radius[count * 4 + i];
        if (stride <= 0.f) {
            LOG(WARNING) << "cwbInfo region " << i << " has no stride";
            return;
        }
        mCwb.regions.push_back({static_cast<int32_t>(radius[i * 4 + 0]),
                                static_cast<int32_t>(radius[i * 4 + 1]),
                                static_cast<int32_t>(radius[i * 4 + 2]),
                                static_cast<int32_t>(radius[i * 4 + 3]),
                                1.f / (stride * stride * stride)});
    }

    mCwb.centreX = static_cast<int32_t>(centre[0]);
    mCwb.centreY = static_cast<int32_t>(centre[1]);
    mCwb.powLow = pow[0];
    mCwb.powHigh = pow[1];
    mCwb.valid = true;

    for (size_t i = 0; i < mCwb.regions.size(); i++) {
        const auto& r = mCwb.regions[i];
        LOG(INFO) << "cwbInfo region " << i << ": l" << r.left << " t" << r.top << " r" << r.right
                  << " b" << r.bottom << " weight " << r.weight;
    }
    LOG(INFO) << "cwbInfo: centre " << mCwb.centreX << "," << mCwb.centreY << " pow "
              << mCwb.powLow << ".." << mCwb.powHigh;
}

float LightCalibration::panelGamma(int32_t brightness) const {
    if (!mCwb.valid) {
        return 2.2f;
    }
    const int32_t max = android::base::GetIntProperty(kMaxBrightnessProp, kMaxBrightnessDefault);
    if (brightness >= max || max <= kGammaRampStart) {
        return mCwb.powHigh;
    }
    if (brightness <= kGammaRampStart) {
        return mCwb.powLow;
    }
    const float t = static_cast<float>(brightness - kGammaRampStart) /
                    static_cast<float>(max - kGammaRampStart);
    return mCwb.powLow + (mCwb.powHigh - mCwb.powLow) * t;
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

float LightCalibration::interpolate(const std::vector<LeakageRow>& table, int32_t brightness,
                                    bool ir) const {
    if (table.empty()) {
        return 0.f;
    }

    const float dbv = static_cast<float>(brightness);
    const LeakageRow& first = table.front();
    const LeakageRow& last = table.back();
    if (dbv <= first.brightness) {
        return ir ? first.ir : first.als;
    }
    if (dbv >= last.brightness) {
        return ir ? last.ir : last.als;
    }

    for (size_t i = 1; i < table.size(); i++) {
        const LeakageRow& hi = table[i];
        if (dbv > hi.brightness) {
            continue;
        }
        const LeakageRow& lo = table[i - 1];
        const float span = hi.brightness - lo.brightness;
        if (span <= 0.f) {
            return ir ? lo.ir : lo.als;
        }
        const float t = (dbv - lo.brightness) / span;
        return ir ? lo.ir + (hi.ir - lo.ir) * t : lo.als + (hi.als - lo.als) * t;
    }
    return ir ? last.ir : last.als;
}

float LightCalibration::leakage(int32_t brightness, bool ir) const {
    const float content = mContentLevel.load();
    if (content >= 0.f && mFullWhite.size() > 1) {
        return interpolate(mFullWhite, brightness, ir) * content;
    }
    return interpolate(mLeakage, brightness, ir);
}

float LightCalibration::leakageAls(int32_t brightness) const {
    return leakage(brightness, false);
}

float LightCalibration::leakageIr(int32_t brightness) const {
    return leakage(brightness, true);
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
