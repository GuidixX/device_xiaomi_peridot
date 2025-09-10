#include <iostream>
#include <fstream>
#include <string>
#include <chrono>
#include <thread>
#include <unistd.h>
#include <syslog.h>
#include <signal.h>
#include <cstring>
#include <sys/stat.h>
#include <cstdlib>

class ThermalChargingService {
private:
    static constexpr const char* BATTERY_STATUS_PATH = "/sys/class/power_supply/battery/status";
    static constexpr const char* BATTERY_TEMP_PATH = "/sys/class/power_supply/battery/temp";
    static constexpr const char* BATTERY_CAPACITY_PATH = "/sys/class/power_supply/battery/capacity";
    static constexpr const char* BATTERY_CURRENT_PATH = "/sys/class/power_supply/battery/constant_charge_current";

    static constexpr int TEMP_CRITICAL = 450;  // 45.0°C
    static constexpr int TEMP_HIGH = 430;      // 43.0°C
    static constexpr int TEMP_MEDIUM = 390;    // 39.0°C
    static constexpr int TEMP_LOW = 350;       // 35.0°C

    static constexpr int CURRENT_CRITICAL = 3000000;   // 3A
    static constexpr int CURRENT_HIGH = 6000000;       // 6A
    static constexpr int CURRENT_MEDIUM = 7000000;     // 7A
    static constexpr int CURRENT_LOW = 10000000;       // 10A
    static constexpr int CURRENT_HIGH_CAPACITY = 5000000; // 5A at 90%
    static constexpr int CURRENT_NORMAL = 15000000;    // 15A
    static constexpr int CAPACITY_HIGH = 90;           // 90%

    bool running = true;

public:
    ThermalChargingService() {
        // Setup syslog
        openlog("thermal_charging_service", LOG_PID | LOG_CONS, LOG_DAEMON);
        syslog(LOG_INFO, "Thermal Charging Service started");
    }

    ~ThermalChargingService() {
        syslog(LOG_INFO, "Thermal Charging Service stopped");
        closelog();
    }

    void signalHandler(int signal) {
        if (signal == SIGTERM || signal == SIGINT) {
            syslog(LOG_INFO, "Received signal %d, shutting down", signal);
            running = false;
        }
    }

    std::string readFile(const std::string& path) {
        std::ifstream file(path);
        if (!file.is_open()) {
            syslog(LOG_ERR, "Failed to open file: %s", path.c_str());
            return "";
        }

        std::string content;
        std::getline(file, content);
        file.close();

        // Remove trailing whitespace
        content.erase(content.find_last_not_of(" \n\r\t") + 1);
        return content;
    }

    bool writeFile(const std::string& path, const std::string& content) {
        std::ofstream file(path);
        if (!file.is_open()) {
            syslog(LOG_ERR, "Failed to open file for writing: %s", path.c_str());
            return false;
        }

        file << content;
        file.close();
        return true;
    }

    bool isCharging() {
        std::string status = readFile(BATTERY_STATUS_PATH);
        return status == "Charging";
    }

    int getBatteryTemp() {
        std::string tempStr = readFile(BATTERY_TEMP_PATH);
        if (tempStr.empty()) {
            return -1;
        }

        char* endPtr;
        long temp = std::strtol(tempStr.c_str(), &endPtr, 10);
        if (endPtr == tempStr.c_str() || *endPtr != '\0') {
            syslog(LOG_ERR, "Failed to parse battery temperature: %s", tempStr.c_str());
            return -1;
        }
        return static_cast<int>(temp);
    }

    int getBatteryCapacity() {
        std::string capacityStr = readFile(BATTERY_CAPACITY_PATH);
        if (capacityStr.empty()) {
            return -1;
        }

        char* endPtr;
        long capacity = std::strtol(capacityStr.c_str(), &endPtr, 10);
        if (endPtr == capacityStr.c_str() || *endPtr != '\0') {
            syslog(LOG_ERR, "Failed to parse battery capacity: %s", capacityStr.c_str());
            return -1;
        }
        return static_cast<int>(capacity);
    }

    int getCurrentChargeCurrent() {
        std::string currentStr = readFile(BATTERY_CURRENT_PATH);
        if (currentStr.empty()) {
            return -1;
        }

        char* endPtr;
        long current = std::strtol(currentStr.c_str(), &endPtr, 10);
        if (endPtr == currentStr.c_str() || *endPtr != '\0') {
            syslog(LOG_ERR, "Failed to parse charge current: %s", currentStr.c_str());
            return -1;
        }
        return static_cast<int>(current);
    }

    bool setChargeCurrent(int current) {
        return writeFile(BATTERY_CURRENT_PATH, std::to_string(current));
    }

    void processCharging() {
        int temp = getBatteryTemp();
        int currentCurrent = getCurrentChargeCurrent();

        if (temp == -1 || currentCurrent == -1) {
            syslog(LOG_ERR, "Failed to read battery data (temp: %d, current: %d)", temp, currentCurrent);
            return;
        }

        int targetCurrent = CURRENT_NORMAL;
        const char* reason = "normal temperature";

        if (temp >= TEMP_CRITICAL) {
            targetCurrent = CURRENT_CRITICAL;
            reason = "critical temperature (>=45.0°C)";
        } else if (temp >= TEMP_HIGH) {
            targetCurrent = CURRENT_HIGH;
            reason = "high temperature (>=43.0°C)";
        } else if (temp >= TEMP_MEDIUM) {
            targetCurrent = CURRENT_MEDIUM;
            reason = "medium temperature (>=39.0°C)";
        } else if (temp >= TEMP_LOW) {
            targetCurrent = CURRENT_LOW;
            reason = "low temperature (>=35.0°C)";
        }

        if (currentCurrent != targetCurrent) {
            if (setChargeCurrent(targetCurrent)) {
                syslog(LOG_INFO, "Set charge current to %dA (temp: %d°C, reason: %s)", 
                       targetCurrent / 1000000, temp / 10, reason);
            } else {
                syslog(LOG_ERR, "Failed to set charge current to %dA", targetCurrent);
            }
        }
    }

    void run() {
        // Setup signal handlers
        signal(SIGTERM, [](int sig) { 
            static ThermalChargingService* instance = nullptr;
            if (instance) instance->signalHandler(sig);
        });
        signal(SIGINT, [](int sig) { 
            static ThermalChargingService* instance = nullptr;
            if (instance) instance->signalHandler(sig);
        });

        syslog(LOG_INFO, "Thermal Charging Service main loop started");

        while (running) {
            if (isCharging()) {
                processCharging();
                std::this_thread::sleep_for(std::chrono::seconds(1));
            } else {
                std::this_thread::sleep_for(std::chrono::seconds(3));
            }
        }

        syslog(LOG_INFO, "Thermal Charging Service main loop ended");
    }
};

int main(int argc, char* argv[]) {
    // Daemonize if not already
    if (argc > 1 && std::strcmp(argv[1], "-d") == 0) {
        pid_t pid = fork();
        if (pid < 0) {
            std::cerr << "Failed to fork" << std::endl;
            return 1;
        }
        if (pid > 0) {
            return 0; // Parent exits
        }

        // Child continues as daemon
        setsid();
        chdir("/");
        umask(0);

        // Close standard file descriptors
        close(STDIN_FILENO);
        close(STDOUT_FILENO);
        close(STDERR_FILENO);
    }

    ThermalChargingService service;
    service.run();

    return 0;
}
