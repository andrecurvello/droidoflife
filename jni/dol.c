#include <jni.h>
#include <android/log.h>

#include <stdlib.h>
#include <pthread.h>

#include <GLES/gl.h>
#include <GLES/glext.h>

#include "com_chrulri_droidoflife_LifeRuntime.h"

#define UNUSED  __attribute__((unused))

#define LOG_TAG "LifeNativeRuntime"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define RGB888TO565(r, g, b)  (((r>>3) << (5+6)) | ((g>>2) << 6) | (b>>3))

#define COLOR_ALIVE	RGB888TO565(0xA4,0xC6,0x39)				// Android hex code: #A4C639
#define COLOR_BORN  RGB888TO565(0xA4>>1,0xC6>>1,0x39>>1)	// half of alive
#define COLOR_DEAD	RGB888TO565(0x00,0x00,0x00)				// dead = black

#define S_PIXELS_SIZE (sizeof(s_pixels[0]) * s_life_w * s_life_h)

#define RETOK com_chrulri_droidoflife_LifeRuntime_OK

static void check_gl_error(const char* op) {
	GLint error;
	for (error = glGetError(); error; error = glGetError()) {
		LOGE("after %s() glError (0x%x)\n", op, error);
	}
}

/* *** VARIABLES *** */
static uint16_t *s_pixels = 0;
static pthread_mutex_t s_pixels_mutex;
static GLuint s_texture = 0;
static int s_life_w = 0;
static int s_life_h = 0;
static int s_scene_w = 0;
static int s_scene_h = 0;

/* *** INITIALIZATION *** */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	LOGI("JNI_OnLoad(..) called");

	// initialize mutex
	pthread_mutex_init(&s_pixels_mutex, NULL);

	// go ahead..
	return JNI_VERSION_1_6;
}

/* *** RUNTIME *** */
jint Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeCreate(JNIEnv *env UNUSED, jclass clazz UNUSED, jint width, jint height) {
	LOGI("nRuntimeCreate(%d, %d)", width, height);

	// initialize random
	srand ( time(NULL) );

	pthread_mutex_lock(&s_pixels_mutex);
	// initialize variables
	s_life_w = width;
	s_life_h = height;
	s_pixels = malloc(S_PIXELS_SIZE);

	// random start
	int size = width*height;
	uint16_t *pixels = s_pixels;
	while(size--) {
		*(pixels++) = rand() % 3 == 0 ? COLOR_ALIVE : COLOR_DEAD;
	}
	pthread_mutex_unlock(&s_pixels_mutex);
	return RETOK;
}

jint Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeIterate(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGI("nRuntimeIterate() called");

	pthread_mutex_lock(&s_pixels_mutex);

	// TODO implement

	pthread_mutex_unlock(&s_pixels_mutex);
	return RETOK;
}


jint Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeDestroy(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGI("nRuntimeDestroy() called");

	pthread_mutex_lock(&s_pixels_mutex);

	free(s_pixels);
	s_pixels = 0;
	s_life_w = s_life_h = 0;

	pthread_mutex_unlock(&s_pixels_mutex);
	return RETOK;
}

/* *** OPENGL *** */
void Java_com_chrulri_droidoflife_LifeRuntime_nGLrender(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGI("nGLrender() called");

	pthread_mutex_lock(&s_pixels_mutex);

	// clear buffer
	glClear(GL_COLOR_BUFFER_BIT);

	if(s_pixels) {
		// render pixels
		glTexImage2D(GL_TEXTURE_2D,	/* target */
			0,				/* level */
			GL_RGB,			/* internal format */
			s_life_w,		/* width */
			s_life_h,		/* height */
			0,				/* border */
			GL_RGB,			/* format */
			GL_UNSIGNED_SHORT_5_6_5,/* type */
			s_pixels);		/* pixels */
		check_gl_error("glTexImage2D");
		glDrawTexiOES(0, 0, 0, s_scene_w, s_scene_h);
		check_gl_error("glDrawTexiOES");
	}

	pthread_mutex_unlock(&s_pixels_mutex);
}

void Java_com_chrulri_droidoflife_LifeRuntime_nGLresize(JNIEnv *env UNUSED, jclass clazz UNUSED, jint width, jint height) {
	LOGI("nGLresize(%d, %d) called", width, height);

	glDeleteTextures(1, &s_texture);
	glEnable(GL_TEXTURE_2D);
	glGenTextures(1, &s_texture);
	glBindTexture(GL_TEXTURE_2D, s_texture);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glShadeModel(GL_FLAT);
	check_gl_error("glShadeModel");
	glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
	check_gl_error("glColor4x");
	int rect[4] = {0, s_life_h, s_life_w, -s_life_h};
	glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, rect);
	check_gl_error("glTexParameteriv");
	s_scene_w = width;
	s_scene_h = height;
}
