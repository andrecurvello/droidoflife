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

#include "com_chrulri_droidoflife_LifeRuntime.h"

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
#define COLOR_DEAD	0xFF000000

typedef uint8_t cell_t;
typedef cell_t* cbuf_t;
#define BITS	(sizeof(cell_t)*8)

#define SET_ALIVE(p, x)	SET_BIT(*(p), (x))
#define SET_DEAD(p, x)	CLEAR_BIT(*(p), (x))

/* *** VARIABLES *** */
static cbuf_t s_cbuf = 0;		// current cell buffer
static cbuf_t s_cbuf_s = 0;		// successor cell buffer
static int s_width = 0;			// world width
static int s_height = 0;		// world height
static size_t s_bufsize = 0;	// cell buffer size: w*h/sizeof(cbuf)
static size_t s_worldsize = 0;	// world size: w*h

/* *** UTILITIES *** */
static inline void destroyRuntime() {
	LOGD("destroyRuntime() called");

	free(s_cbuf);
	free(s_cbuf_s);
	s_cbuf = s_cbuf_s = 0;
	s_bufsize = s_worldsize = 0;
	s_width = s_height = 0;

	LOGD("destroyRuntime() exited");
}

static int isAlive(cbuf_t ptr, int x) {
	LOGD("isAlive(%d, %d) called", ptr, x);
	int t, poff, off;
	if(x < 0) {
		x = -x;
		poff = -(x / BITS + 1);
		off = BITS - (x % BITS);
		LOGD(" x<0: off=%d, poff=%d", off, poff);
		t = CHECK_BIT(*(ptr + poff), off);
	} else if (x < 8) {
		LOGD(" x<8: ---");
		t = CHECK_BIT(*ptr, x);
	} else {
		poff = x / BITS;
		off = x % BITS;
		LOGD(" ---: off=%d, poff=%d", off, poff);
		t = CHECK_BIT(*(ptr + poff), off);
	}
	LOGD("isAlive(%d) exited", t);
	return t;
}

/* *** RUNTIME *** */
jint Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeCreate(JNIEnv *env UNUSED, jclass clazz UNUSED, jint width, jint height) {
	LOGD("nRuntimeCreate(%d, %d) called", width, height);

	size_t x;

	// initialize random
	srand ( time(NULL) );

	// initialize variables
	s_width = width;
	s_height = height;
	s_worldsize = width * height;
	x = s_worldsize % BITS == 0 ? 0 : 1;
	s_bufsize = (s_worldsize / BITS) + x;

	s_cbuf = malloc(s_bufsize);
	if(!s_cbuf) {
		LOGE("s_cbuf failed to malloc(%d)", s_bufsize);
		destroyRuntime();
		return errno;
	}
	s_cbuf_s = malloc(s_bufsize);
	if(!s_cbuf_s) {
		LOGE("s_cbuf_s failed to malloc(%d)", s_bufsize);
		destroyRuntime();
		return errno;
	}

	// random start
	cbuf_t cells = s_cbuf;
	cell_t cell;
	uint i;
	x = s_bufsize;
	while(x--) {
		cell = 0;
		for(i = 0; i < BITS; i++) {
			cell |= (rand() % 5 == 0) << i;
		}
		*(cells++) = cell;
	}

	LOGD("nRuntimeCreate(..) exited");
	return com_chrulri_droidoflife_LifeRuntime_OK;
}

void Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeIterate(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGD("nRuntimeIterate() called");

	/*** this is where the magic begins ***/

	uint i, bi, alives, left, right;
	int ci; // must not be uint because of negative indexes
	cbuf_t ptr, sptr;
	for(i = 0; i < s_worldsize; i++) {
		bi = i / BITS;	// buffer index
		ci = i % BITS;	// cell index

		ptr = (cbuf_t)(s_cbuf + bi);
		sptr = (cbuf_t)(s_cbuf_s + bi);
		alives = 0;

		// CHECK NEIGHBOURS //
		LOGD("-> check neighbours of %d", i);
		left = i % s_width;		// index modulo stride -> 0 if most left element & if it's left most element, it cannot be the right most too
		right = (left + 1) != s_width;	// next one is left one of next row
		// left
		if(left && isAlive(ptr, ci - 1))
			alives++;
		// right
		if(right && isAlive(ptr, ci + 1))
			alives++;
		// upper row (index must not be less than stride)
		if(i >= s_width) {
			// upper
			if(isAlive(ptr, ci - s_width))
				alives++;
			// upper left
			if(left && isAlive(ptr, ci - s_width - 1))
				alives++;
			// upper right
			if(left && isAlive(ptr, ci - s_width + 1))
				alives++;
		}
		// lower row (index must not be greater than buffer size minus stride)
		if(s_worldsize - i > s_width) {
			// lower
			if(isAlive(ptr, ci + s_width))
				alives++;
			// lower left
			if(left && isAlive(ptr, ci + s_width - 1))
				alives++;
			// lower right
			if(left && isAlive(ptr, ci + s_width + 1))
				alives++;
		}
		// SET SUCCESSORS //
		if(alives < MIN_ALIVE_NEIGHBOURS || alives > MAX_ALIVE_NEIGHBOURS) {
			SET_DEAD(sptr, ci);
		} else if(isAlive(ptr, ci) || alives == MAX_ALIVE_NEIGHBOURS) {
			SET_ALIVE(sptr, ci);
		} else {
			SET_DEAD(sptr, ci);
		}
	}

	/*** the magic has happened, amen! ***/

	// swap buffers, current cell buffer is next successor cell buffer
	ptr = s_cbuf;
	s_cbuf = s_cbuf_s;
	s_cbuf_s = ptr;

	LOGD("nRuntimeIterate() exited");
}


void Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeDestroy(JNIEnv *env UNUSED, jclass clazz UNUSED) {
	LOGD("nRuntimeDestroy() called");

	destroyRuntime();

	LOGD("nRuntimeDestroy() exited");
}

void Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeBitmap(JNIEnv *env, jclass clazz UNUSED, jobject bitmap) {
	LOGD("nRuntimeBitmap(%d) called", bitmap);

	if(!s_cbuf) {
		LOGE("nRuntimeBitmap(..) exited without runtime!");
		return;
	}

	AndroidBitmapInfo  info;
	uint32_t          *pixels;
	int                ret;

	if((ret = AndroidBitmap_getInfo(env, bitmap, &info))) {
		LOGE("AndroidBitmap_getInfo(..) failed: 0x%x", ret);
		return;
	}

	if(info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		LOGE("Bitmap format is not RGBA_8888!");
		return;
	}

	if((ret = AndroidBitmap_lockPixels(env, bitmap, (void**)&pixels))) {
		LOGE("AndroidBitmap_lockPixels(..) failed: 0x%x", ret);
		return;
	}

	uint32_t *ptr = pixels;
	uint i;
	for(i = 0; i < s_worldsize; i++) {
		*(ptr++) = isAlive(s_cbuf, i) ? COLOR_ALIVE : COLOR_DEAD;
	}

	if((ret = AndroidBitmap_unlockPixels(env, bitmap))) {
		LOGE("AndroidBitmap_unlockPixels(..) failed: 0x%x", ret);
		return;
	}

	LOGD("nRuntimeBitmap(..) exited");
}
