#include <android/log.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>

namespace {
constexpr char kTag[] = "SamuraiJackNative";

void HandleAppCmd(android_app* app, int32_t cmd) {
    switch (cmd) {
        case APP_CMD_INIT_WINDOW:
            __android_log_print(ANDROID_LOG_INFO, kTag, "Native window initialized");
            break;
        case APP_CMD_TERM_WINDOW:
            __android_log_print(ANDROID_LOG_INFO, kTag, "Native window terminated");
            break;
        default:
            break;
    }
}
}  // namespace

extern "C" void android_main(android_app* app) {
    app->onAppCmd = HandleAppCmd;

    while (!app->destroyRequested) {
        int events;
        android_poll_source* source = nullptr;
        while (ALooper_pollOnce(-1, nullptr, &events, reinterpret_cast<void**>(&source)) >= 0) {
            if (source != nullptr) {
                source->process(app, source);
            }
            if (app->destroyRequested) {
                break;
            }
        }
    }
}
