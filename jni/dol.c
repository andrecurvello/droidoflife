/******************************************************************************
 *  Droid of Life, an open source Android game based on Conway's Game of Life *
 *  Copyright (C) 2012  Christian Ulrich <chrulri@gmail.com>                  *
 *                                                                            *
 *  This program is free software: you can redistribute it and/or modify      *
 *  it under the terms of the GNU General Public License as published by      *
 *  the Free Software Foundation, either version 3 of the License, or         *
 *  (at your option) any later version.                                       *
 *                                                                            *
 *  This program is distributed in the hope that it will be useful,           *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *  GNU General Public License for more details.                              *
 *                                                                            *
 *  You should have received a copy of the GNU General Public License         *
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
 ******************************************************************************/
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <errno.h>
#include <stdlib.h>
#include <pthread.h>

#include "com_chrulri_droidoflife_LifeRuntime.h"

//#define DEBUG

#define UNUSED  __attribute__((unused))

#define SET_BIT(var,pos)	((var) |=  (1<<(pos)))
#define CLEAR_BIT(var,pos)	((var) &= ~(1<<(pos)))
#define CHECK_BIT(var,pos)	((var) &   (1<<(pos)))

#define LOG_TAG "LifeNativeRuntime"
#define LOGE(...)	__android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#ifdef DEBUG
#define LOGD(...)	__android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#else
#define LOGD(...)	((void)0)
#endif

#define MIN_ALIVE_NEIGHBOURS	2
#define MAX_ALIVE_NEIGHBOURS	3

#define COLOR_ALIVE	0xFF00FF00
#define COLOR_BORN	0xFF008800
#define COLOR_DEAD	0xFF000000
#define COLOR_DIED	0xFF888888

typedef uint8_t cell_t;
typedef cell_t* cbuf_t;
#define BITS	(sizeof(cell_t)*8)

/* *** VARIABLES *** */
static pthread_mutex_t s_mutex;
static cbuf_t s_cbuf = 0;		// current cell buffer
static cbuf_t s_cbuf_s = 0;		// successor cell buffer
static cbuf_t s_cbuf_l = 0;     // life state cell buffer (bit set = has just been born / died)
static int s_width = 0;			// world width
static int s_height = 0;		// world height
static size_t s_bufsize = 0;	// cell buffer size: w*h/sizeof(cbuf)
static size_t s_worldsize = 0;	// world size: w*h

/* *** UTILITIES *** */
static inline void lockRuntime() {
	int ret;
	if((ret = pthread_mutex_lock(&s_mutex))) {
		LOGE("phread_mutex_lock failed (0x%x)", ret);
	}
}

static inline void unlockRuntime() {
	int ret;
	if((ret = pthread_mutex_unlock(&s_mutex))) {
		LOGE("phread_mutex_unlock failed (0x%x)", ret);
	}
}

static inline void destroyRuntime() {
	LOGD("destroyRuntime() called");

	free(s_cbuf);
	free(s_cbuf_s);
	free(s_cbuf_l);
	s_cbuf = s_cbuf_s = s_cbuf_l = 0;
	s_bufsize = s_worldsize = 0;
	s_width = s_height = 0;

	LOGD("destroyRuntime() exited");
}

static inline uint isBitSet(cbuf_t ptr, int offset) {
//	LOGD("isBitSet(%d, %d) called", ptr, offset);
	int t, poff, off;
	if(offset < 0) {
		offset = -offset;
		poff = -(offset / BITS + 1);
		off = BITS - (offset % BITS);
//		LOGD(" offset<0: off=%d, poff=%d", off, poff);
		t = CHECK_BIT(*(ptr + poff), off);
	} else if (offset < 8) {
//		LOGD(" offset<8: ---");
		t = CHECK_BIT(*ptr, offset);
	} else {
		poff = offset / BITS;
		off = offset % BITS;
//		LOGD(" ---: off=%d, poff=%d", off, poff);
		t = CHECK_BIT(*(ptr + poff), off);
	}
//	LOGD("isBitSet(%d) exited", t);
	return t;
}

/* *** INITIALIZATION *** */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	LOGD("JNI_OnLoad(..) called");
	int ret;

	srand(time(NULL));

	// initialize mutex
	if((ret = pthread_mutex_init(&s_mutex, NULL))) {
		LOGE("pthread_mutex_init failed (0x%x)", ret);
	}

	// go ahead..
	return JNI_VERSION_1_6;
}

/* *** RUNTIME *** */
jint Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeCreate(JNIEnv *env UNUSED, jclass clazz UNUSED, jint width, jint height) {
	LOGD("nRuntimeCreate(%d, %d) called", width, height);

	lockRuntime();

	if(width <= 0 || height <= 0) {
		LOGE("%dx%d is an invalid size for a living room", width, height);
		unlockRuntime();
		return com_chrulri_droidoflife_LifeRuntime_E_INVALID_SIZE;
	}

	// initialize variables
	s_width = width;
	s_height = height;
	s_worldsize = width * height;
	s_bufsize = (s_worldsize / BITS) + (s_worldsize % BITS == 0 ? 0 : 1);

	s_cbuf = malloc(s_bufsize);
	if(!s_cbuf) {
		LOGE("s_cbuf failed to malloc(%d)", s_bufsize);
		destroyRuntime();
		unlockRuntime();
		return errno;
	}
	s_cbuf_s = malloc(s_bufsize);
	if(!s_cbuf_s) {
		LOGE("s_cbuf_s failed to malloc(%d)", s_bufsize);
		destroyRuntime();
		unlockRuntime();
		return errno;
	}
	s_cbuf_l = malloc(s_bufsize);
	if(!s_cbuf_l) {
		LOGE("s_cbuf_l failed to malloc(%d)", s_bufsize);
		destroyRuntime();
		unlockRuntime();
		return errno;
	}

	// random start
	cbuf_t cells = s_cbuf;
	cell_t cell;
	uint i;
	size_t x = s_bufsize;
	while(x--) {
		cell = 0;
		for(i = 0; i < BITS; i++) {
			cell |= (rand() % 5 == 0) << i;
		}
		*(cells++) = cell;
	}
	memset(s_cbuf_l, 0, s_bufsize);

	unlockRuntime();

	LOGD("nRuntimeCreate(..) exited");
	return com_chrulri_droidoflife_LifeRuntime_OK;
}

void Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeIterate(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGD("nRuntimeIterate() called");

	lockRuntime();

	/*** this is where the magic begins ***/

	memset(s_cbuf_s, 0, s_bufsize); // reset successor buffer
	memset(s_cbuf_l, 0, s_bufsize); // reset life state buffer
	uint i, bi, alives, left, right, current;
	int ci; // must not be uint because of negative indexes
	cbuf_t ptr, sptr, lptr;
	for(i = 0; i < s_worldsize; i++) {
		bi = i / BITS;	// buffer index
		ci = i % BITS;	// cell index

		ptr = (cbuf_t)(s_cbuf + bi);
		sptr = (cbuf_t)(s_cbuf_s + bi);
		lptr = (cbuf_t)(s_cbuf_l + bi);
		alives = 0;
		current = isBitSet(ptr, ci);

		// CHECK NEIGHBOURS //
		LOGD("-> check neighbours of %d", i);
		left = i % s_width;		// index modulo stride -> 0 if most left element & if it's left most element, it cannot be the right most too
		right = (left + 1) != s_width;	// next one is left one of next row
		// left
		if(left && isBitSet(ptr, ci - 1))
			alives++;
		// right
		if(right && isBitSet(ptr, ci + 1))
			alives++;
		// upper row (index must not be less than stride)
		if(i >= s_width) {
			// upper
			if(isBitSet(ptr, ci - s_width))
				alives++;
			// upper left
			if(left && isBitSet(ptr, ci - s_width - 1))
				alives++;
			// upper right
			if(left && isBitSet(ptr, ci - s_width + 1))
				alives++;
		}
		// lower row (index must not be greater than buffer size minus stride)
		if(s_worldsize - i > s_width) {
			// lower
			if(isBitSet(ptr, ci + s_width))
				alives++;
			// lower left
			if(left && isBitSet(ptr, ci + s_width - 1))
				alives++;
			// lower right
			if(left && isBitSet(ptr, ci + s_width + 1))
				alives++;
		}
		// SET SUCCESSORS //
		if(alives < MIN_ALIVE_NEIGHBOURS || alives > MAX_ALIVE_NEIGHBOURS) {
			if(current) {
				SET_BIT(*lptr, ci);
			}
		} else if(current) {
			SET_BIT(*sptr, ci);
		} else if (alives == MAX_ALIVE_NEIGHBOURS) {
			SET_BIT(*sptr, ci);
			SET_BIT(*lptr, ci);
		}
	}

	/*** the magic has happened, amen! ***/

	// swap buffers, current cell buffer is next successor cell buffer
	ptr = s_cbuf;
	s_cbuf = s_cbuf_s;
	s_cbuf_s = ptr;
	// life state buffer remains the same

	unlockRuntime();

	LOGD("nRuntimeIterate() exited");
}


void Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeDestroy(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGD("nRuntimeDestroy() called");

	lockRuntime();

	destroyRuntime();

	unlockRuntime();

	LOGD("nRuntimeDestroy() exited");
}

void Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeBitmap(JNIEnv *env, jclass clazz UNUSED, jobject bitmap) {
	LOGD("nRuntimeBitmap(%d) called", bitmap);

	lockRuntime();

	if(!s_cbuf) {
		LOGE("nRuntimeBitmap(..) exited without runtime!");
		unlockRuntime();
		return;
	}

	// render settings
	uint enableBornDeath = 1; // TODO read user settings

	AndroidBitmapInfo  info;
	uint32_t          *pixels;
	int                ret;

	if((ret = AndroidBitmap_getInfo(env, bitmap, &info))) {
		LOGE("AndroidBitmap_getInfo(..) failed: 0x%x", ret);
		unlockRuntime();
		return;
	}

	if(info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		LOGE("Bitmap format is not RGBA_8888!");
		unlockRuntime();
		return;
	}

	if((ret = AndroidBitmap_lockPixels(env, bitmap, (void**)&pixels))) {
		LOGE("AndroidBitmap_lockPixels(..) failed: 0x%x", ret);
		unlockRuntime();
		return;
	}

	uint32_t *ptr = pixels;
	uint i, b;
	for(i = 0; i < s_worldsize; i++) {
		b = enableBornDeath ? isBitSet(s_cbuf_l, i) : 0;
		*(ptr++) = isBitSet(s_cbuf, i) ?
				(b ? COLOR_BORN : COLOR_ALIVE) :
				(b ? COLOR_DIED : COLOR_DEAD);
	}

	if((ret = AndroidBitmap_unlockPixels(env, bitmap))) {
		LOGE("AndroidBitmap_unlockPixels(..) failed: 0x%x", ret);
		unlockRuntime();
		return;
	}

	unlockRuntime();

	LOGD("nRuntimeBitmap(..) exited");
}
