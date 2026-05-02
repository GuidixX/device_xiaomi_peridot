/*
 * Copyright (C) 2021 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <libinit_dalvik_heap.h>
#include <libinit_variant.h>

#include "vendor_init.h"

static const variant_info_t global_variant = {
    .hwc_value = "GL",

    .mod_device = "peridot_global",
    .brand = "POCO",
    .device = "peridot",
    .name = "peridot_global",
    .marketname = "POCO F6",
    .model = "24069PC21G",
    .build_fingerprint = "POCO/peridot_global/peridot:16/BP2A.250605.031.A3/OS3.0.6.0.WNPMIXM:user/release-keys",
};

static const variant_info_t indian_variant = {
    .hwc_value = "IN",

    .mod_device = "peridot_global",
    .brand = "POCO",
    .device = "peridot",
    .name = "peridot_global",
    .marketname = "POCO F6",
    .model = "24069PC21I",
    .build_fingerprint = "POCO/peridot_global/peridot:16/BP2A.250605.031.A3/OS3.0.6.0.WNPMIXM:user/release-keys",
};

static const variant_info_t chinese_variant = {
    .hwc_value = "CN",

    .mod_device = "peridot_global",
    .brand = "Redmi",
    .device = "peridot",
    .name = "peridot_global",
    .marketname = "Redmi Turbo 3",
    .model = "24069RA21C",
    .build_fingerprint = "Redmi/peridot/peridot:14/UKQ1.240116.001/V816.0.18.0.UNPCNXM:user/release-keys",
};

static const std::vector<variant_info_t> variants = {
    global_variant,
    indian_variant,
    chinese_variant,
};

void vendor_load_properties() {
    search_variant(variants);
    set_dalvik_heap();
}
