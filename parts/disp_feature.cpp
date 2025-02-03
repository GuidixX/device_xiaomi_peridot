#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <string.h>
#include <include/mi_disp.h>

#define DISP_PARAM_NODE "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/disp_param"

int main(int argc, char **argv) {
    if (argc != 3) {
        fprintf(stderr, "Usage: %s <feature_id> <0|1>\n", argv[0]);
        return -1;
    }

    // Checking if feature_id = 8 (DC Dimming)
    __u32 feature_id = static_cast<__u32>(atoi(argv[1]));
    if (feature_id != DISP_FEATURE_DC) { // DISP_FEATURE_DC должно быть 8
        fprintf(stderr, "Invalid feature_id for DC Dimming. Use 8.\n");
        return -1;
    }

    __s32 enabled = atoi(argv[2]);
    if (enabled != 0 && enabled != 1) {
        fprintf(stderr, "Enabled must be 0 or 1\n");
        return -1;
    }

    // Openning node for writing
    int fd = open(DISP_PARAM_NODE, O_WRONLY);
    if (fd < 0) {
        fprintf(stderr, "Failed to open %s: %s\n", DISP_PARAM_NODE, strerror(errno));
        return -1;
    }

    char buffer[16];
    snprintf(buffer, sizeof(buffer), "%d %d", feature_id, enabled);

    ssize_t bytes_written = write(fd, buffer, strlen(buffer));
    if (bytes_written < 0) {
        fprintf(stderr, "Write failed: %s\n", strerror(errno));
        close(fd);
        return -1;
    }

    close(fd);
    printf("Success: DC Dimming set to %d\n", enabled);
    return 0;
}
