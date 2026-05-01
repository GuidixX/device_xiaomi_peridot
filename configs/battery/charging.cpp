#include <fstream>
#include <thread>
#include <chrono>
#include <android/log.h>

#define TAG "charging"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

#define T_NODE "/sys/class/power_supply/battery/temp"
#define L_NODE "/sys/class/power_supply/battery/charge_control_limit"

int main() {
    int last_idx = -1;
    while (true) {
        std::ifstream f_t(T_NODE);
        int t = 0;
        if (!(f_t >> t)) {
            std::this_thread::sleep_for(std::chrono::seconds(5));
            continue;
        }
        f_t.close();

        int idx = 0;
        if (t >= 440) idx = 16;
        else if (t >= 410) idx = 10;
        else if (t >= 380) idx = 5;
        else idx = 0;

        if (idx != last_idx) {
            std::ofstream f_l(L_NODE);
            if (f_l.is_open()) {
                f_l << idx;
                f_l.close();
                LOGI("%d.%dC | idx: %d", t/10, t%10, idx);
                last_idx = idx;
            }
        }
        std::this_thread::sleep_for(std::chrono::seconds(5));
    }
    return 0;
}
