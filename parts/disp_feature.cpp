#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <string.h>

#include <include/mi_disp.h>

#define DISP_FEATURE_DEV_PATH "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/disp_param"

int main(int argc, char **argv) {
    if (argc != 3) {
        fprintf(stderr, "Usage: %s <feature id> <0|1>\n", argv[0]);
        return -1;
    }

    __u32 feature_id = static_cast<__u32>(atoi(argv[1]));
    __s32 enabled = atoi(argv[2]);

    if (!isSupportDispFeatureId(feature_id)) {
        fprintf(stderr, "Unsupported feature id.\n");
        return -1;
    }

    if (enabled != 0 && enabled != 1) {
        fprintf(stderr, "Enabled should be 0 or 1\n");
        return -1;
    }

    int fd = open(DISP_FEATURE_DEV_PATH, O_RDWR);
    if (fd < 0) {
        fprintf(stderr, "Failed to open %s: %s\n", DISP_FEATURE_DEV_PATH, strerror(errno));
        return -1;
    }

    struct disp_base base = {
        MI_DISP_PRIMARY, MI_DISP_FLAG_BLOCK
    };

    struct disp_feature_req req = {
        base, feature_id, enabled, 0, 0, 0, 0
    };

    if (ioctl(fd, MI_DISP_IOCTL_SET_FEATURE, &req) < 0) {
        fprintf(stderr, "ioctl failed: %s\n", strerror(errno));
        close(fd);
        return -1;
    }

    close(fd);
    return 0;
}
