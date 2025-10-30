/*
 * Copyright (C) 2021-2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <string>

typedef struct dalvik_heap_info {
    std::string heapstartsize;
    std::string heapgrowthlimit;
    std::string heapsize;
    std::string heaptargetutilization;
    std::string heapminfree;
    std::string heapmaxfree;
} dalvik_heap_info_t;

void set_dalvik_heap(void);
