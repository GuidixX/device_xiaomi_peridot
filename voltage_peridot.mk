#
# Copyright (C) 2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common VoltageOS stuff.
$(call inherit-product, vendor/voltage/config/common_full_phone.mk)

# Inherit from peridot device
$(call inherit-product, device/xiaomi/peridot/device.mk)

PRODUCT_NAME := voltage_peridot
PRODUCT_DEVICE := peridot
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := POCO
PRODUCT_MODEL := 24069PC21G
PRODUCT_SYSTEM_NAME := peridot_global
PRODUCT_SYSTEM_DEVICE := peridot

# Fingerprint
PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="peridot_global-user 15 AQ3A.240912.001 OS2.0.207.0.VNPMIXM release-keys" \
    BuildFingerprint=POCO/peridot_global/peridot:15/AQ3A.240912.001/OS2.0.207.0.VNPMIXM:user/release-keys \
    DeviceName=$(PRODUCT_SYSTEM_DEVICE) \
    DeviceProduct=$(PRODUCT_SYSTEM_NAME)

# GMS
PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

# Voltage Flags
TARGET_BOOT_ANIMATION_RES := 2560
TARGET_FACE_UNLOCK_SUPPORTED := true
VOLTAGE_BUILD_TYPE := OFFICIAL
EXTRA_UDFPS_ANIMATIONS := true
TORCH_STR_SUPPORTED := true

# Boost Framework
VOLTAGE_CPU_SMALL_CORES := 0,1,2
VOLTAGE_CPU_BIG_CORES := 3,4,5,6,7
VOLTAGE_CPU_SYS_BG := 0-3
VOLTAGE_CPU_BG := 0-2
VOLTAGE_CPU_FG := 0-7
VOLTAGE_CPU_LIMIT_BG := 0-2
VOLTAGE_CPU_UNLIMIT_UI := 0-7
VOLTAGE_CPU_LIMIT_UI := 0-5
VOLTAGE_CPU_DISPLAY := 6-7
