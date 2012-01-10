#include <jni.h>
#include <android/log.h>

#include <errno.h>
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

#define MIN_ALIVE_NEIGHBOURS	2
#define MAX_ALIVE_NEIGHBOURS	3

#define COLOR_ALIVE	RGB888TO565(0xA4,0xC6,0x39)				// Android hex code: #A4C639
#define COLOR_DEAD	RGB888TO565(0x00,0x00,0x00)				// dead = black

#define IS_ALIVE(x)	(x == COLOR_ALIVE)

#define RET_OK com_chrulri_droidoflife_LifeRuntime_OK
#define RET_NOMEMORY com_chrulri_droidoflife_LifeRuntime_NOMEMORY

typedef uint16_t* pixbuf_t;

/* *** VARIABLES *** */
static size_t s_pixels_size = 0;// pixel buffer size
static pixbuf_t s_pixels = 0;	// current pixel buffer
static pixbuf_t s_pixelss = 0; // successor pixel buffer
static pthread_mutex_t s_pixels_mutex;
static GLuint s_texture = 0;
static int s_life_w = 0;
static int s_life_h = 0;
static int s_scene_w = 0;
static int s_scene_h = 0;

/* *** UTILITIES *** */
static void checkGLerror(const char* op) {
	GLint error;
	for (error = glGetError(); error; error = glGetError()) {
		LOGE("after %s() glError (0x%x)", op, error);
	}
}

static inline void lockPixels() {
	int ret;
	if((ret = pthread_mutex_lock(&s_pixels_mutex))) {
		LOGE("phread_mutex_unlock failed (0x%x)", ret);
	}
}

static inline void unlockPixels() {
	int ret;
	if((ret = pthread_mutex_unlock(&s_pixels_mutex))) {
		LOGE("phread_mutex_unlock failed (0x%x)", ret);
	}
}

static inline void destroyRuntime() {
	free(s_pixels);
	free(s_pixelss);
	s_pixels = s_pixelss = 0;
	s_pixels_size = 0;
	s_life_w = s_life_h = 0;
}

/* *** INITIALIZATION *** */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	LOGI("JNI_OnLoad(..) called");
	int ret;

	// initialize mutex
	if((ret = pthread_mutex_init(&s_pixels_mutex, NULL))) {
		LOGE("pthread_mutex_init failed (0x%x)", ret);
	}

	// go ahead..
	return JNI_VERSION_1_6;
}

/* *** RUNTIME *** */
jint Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeCreate(JNIEnv *env UNUSED, jclass clazz UNUSED, jint width, jint height) {
	LOGI("nRuntimeCreate(%d, %d)", width, height);

	// initialize random
	srand ( time(NULL) );

	lockPixels();

	// initialize variables
	s_life_w = width;
	s_life_h = height;

	s_pixels_size = sizeof(s_pixels[0]) * s_life_w * s_life_h;

	s_pixels = malloc(s_pixels_size);
	if(!s_pixels) {
		LOGE("s_pixels failed to malloc(%d)", s_pixels_size);
		unlockPixels();
		destroyRuntime();
		return errno;
	}
	s_pixelss = malloc(s_pixels_size);
	if(!s_pixelss) {
		LOGE("s_pixelss failed to malloc(%d)", s_pixels_size);
		unlockPixels();
		destroyRuntime();
		return errno;
	}

	// random start
	int size = width*height;
	pixbuf_t pixels = s_pixels;
	while(size--) {
		*(pixels++) = rand() % 3 == 0 ? COLOR_ALIVE : COLOR_DEAD;
	}

	unlockPixels();

	return RET_OK;
}

void Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeIterate(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGI("nRuntimeIterate() called");

	lockPixels();

	/*** this is where the magic begins ***/

	uint i, alives, left, right;
	pixbuf_t ptr, sptr;
	for(i = 0; i < s_pixels_size; i++) {
		ptr = (pixbuf_t)(s_pixels + i);
		sptr = (pixbuf_t)(s_pixelss + i);
		alives = 0;

		// CHECK NEIGHBOURS //
		left = i % s_life_w;		// index modulo stride -> 0 if most left element
		right = (i + 1) % s_life_w;	// index plus one modulo stride -> 0 if most right element (next one is left one of next row)
		// left
		if(left && IS_ALIVE(*(ptr - 1)))
			alives++;
		// right
		if(right && IS_ALIVE(*(ptr + 1)))
			alives++;
		// upper row (index must not be less than stride)
		if(i > s_life_w) {
			// upper
			if(IS_ALIVE(*(ptr - s_life_w)))
				alives++;
			// upper left
			if(left && IS_ALIVE(*(ptr - s_life_w - 1)))
				alives++;
			// upper right
			if(left && IS_ALIVE(*(ptr - s_life_w + 1)))
				alives++;
		}
		// lower row (index must not be greater than buffer size minus stride)
		if(s_pixels_size - i > s_life_w) {
			// lower
			if(IS_ALIVE(*(ptr + s_life_w)))
				alives++;
			// lower left
			if(left && IS_ALIVE(*(ptr + s_life_w - 1)))
				alives++;
			// lower right
			if(left && IS_ALIVE(*(ptr + s_life_w + 1)))
				alives++;
		}
		// CHECK SUCCESSORS //
		if(alives < MIN_ALIVE_NEIGHBOURS || alives > MAX_ALIVE_NEIGHBOURS) {
			*sptr = COLOR_DEAD;
		} else {
			*sptr = *ptr;
			if(alives == MAX_ALIVE_NEIGHBOURS) {
				*sptr = COLOR_ALIVE;
			}
		}
	}

	/*** the magic has happened, amen! ***/

	// swap buffers, current pixel buffer is next successor buffer
	ptr = s_pixels;
	s_pixels = s_pixelss;
	s_pixelss = ptr;

	unlockPixels();

	LOGI("nRuntimeIterate() exited");
}


void Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeDestroy(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGI("nRuntimeDestroy() called");

	lockPixels();

	destroyRuntime();

	unlockPixels();

	LOGI("nRuntimeDestroy() exited");
}

/* *** OPENGL *** */
void Java_com_chrulri_droidoflife_LifeRuntime_nGLrender(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGI("nGLrender() called");

	// clear buffer
	glClear(GL_COLOR_BUFFER_BIT);

	lockPixels();

	if(s_pixels) {
		// render pixels
		glTexImage2D(
			GL_TEXTURE_2D,	/* target */
			0,				/* level */
			GL_RGB,			/* internal format */
			s_life_w,		/* width */
			s_life_h,		/* height */
			0,				/* border */
			GL_RGB,			/* format */
			GL_UNSIGNED_SHORT_5_6_5,/* type */
			s_pixels);		/* pixels */
		checkGLerror("glTexImage2D");
		glDrawTexiOES(0, 0, 0, s_scene_w, s_scene_h);
		checkGLerror("glDrawTexiOES");
	}

	unlockPixels();

	LOGI("nGLrender() exited");
}

void Java_com_chrulri_droidoflife_LifeRuntime_nGLresize(JNIEnv *env UNUSED, jclass clazz UNUSED, jint width, jint height) {
	LOGI("nGLresize(%d, %d) called", width, height);

	s_scene_w = width;
	s_scene_h = height;

	glDeleteTextures(1, &s_texture);
	glEnable(GL_TEXTURE_2D);
	glGenTextures(1, &s_texture);
	glBindTexture(GL_TEXTURE_2D, s_texture);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glShadeModel(GL_FLAT);
	checkGLerror("glShadeModel");
	glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
	checkGLerror("glColor4x");
	int rect[4] = {0, 0, s_life_w, s_life_h};
	glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, rect);
	checkGLerror("glTexParameteriv");

	LOGI("nGLresize(..) exited");
}
