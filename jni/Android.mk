LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := dol
LOCAL_SRC_FILES := dol.c
LOCAL_LDLIBS    := -llog -lGLESv1_CM
#LOCAL_CFLAGS    := -DDEBUG
include $(BUILD_SHARED_LIBRARY)
