#include <fstream>
#include <thread>
#include <chrono>
#include <android/log.h>

#define TAG "charging"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

#define T_NODE "/sys/class/power_supply/battery/temp"
#define L_NODE "/sys/class/power_supply/battery/charge_control_limit"
#define FC_NODE "/sys/class/qcom-battery/fastcharge_enable"

int main() {
    int last_idx = -1;
    int last_mode = -1;
    while (true) {
        std::ifstream f_t(T_NODE);
        int t = 0;
        if (!(f_t >> t)) {
            std::this_thread::sleep_for(std::chrono::seconds(5));
            continue;
        }
        f_t.close();

        std::ifstream f_fc(FC_NODE);
        int fast_charge_mode = 1; // Default to fast charge
        if (f_fc >> fast_charge_mode) {
            f_fc.close();
        }

        int idx = 0;

        if (fast_charge_mode == 0) {
            // value_none
            if (t >= 370) idx = 16;
            else if (t >= 360) idx = 16;
            else if (t >= 350) idx = 10;
            else idx = 5;
        } else if (fast_charge_mode == 1) {
            // value_fast_charge
            if (t >= 410) idx = 16;
            else if (t >= 390) idx = 10;
            else if (t >= 370) idx = 5;
            else idx = 0;
        } else if (fast_charge_mode == 2) {
             // value_super_fast_charge
             if (t >= 440) idx = 16;
             else if (t >= 410) idx = 10;
             else if (t >= 380) idx = 5;
             else idx = 0;
        }

        if (idx != last_idx || fast_charge_mode != last_mode) {
            std::ofstream f_l(L_NODE);
            if (f_l.is_open()) {
                f_l << idx;
                f_l.close();
                LOGI("%d.%dC | FC Mode: %d | idx: %d", t/10, t%10, fast_charge_mode, idx);
                last_idx = idx;
                last_mode = fast_charge_mode;
            }
        }
        std::this_thread::sleep_for(std::chrono::seconds(5));
    }
    return 0;
}
