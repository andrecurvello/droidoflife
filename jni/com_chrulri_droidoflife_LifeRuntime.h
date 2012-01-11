/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_chrulri_droidoflife_LifeRuntime */

#ifndef _Included_com_chrulri_droidoflife_LifeRuntime
#define _Included_com_chrulri_droidoflife_LifeRuntime
#ifdef __cplusplus
extern "C" {
#endif
#undef com_chrulri_droidoflife_LifeRuntime_OK
#define com_chrulri_droidoflife_LifeRuntime_OK 0L
#undef com_chrulri_droidoflife_LifeRuntime_NOMEMORY
#define com_chrulri_droidoflife_LifeRuntime_NOMEMORY 1L
/*
 * Class:     com_chrulri_droidoflife_LifeRuntime
 * Method:    nRuntimeCreate
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeCreate
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     com_chrulri_droidoflife_LifeRuntime
 * Method:    nRuntimeIterate
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeIterate
  (JNIEnv *, jclass);

/*
 * Class:     com_chrulri_droidoflife_LifeRuntime
 * Method:    nRuntimeDestroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeDestroy
  (JNIEnv *, jclass);

/*
 * Class:     com_chrulri_droidoflife_LifeRuntime
 * Method:    nRuntimeBitmap
 * Signature: (Landroid/graphics/Bitmap;)V
 */
JNIEXPORT void JNICALL Java_com_chrulri_droidoflife_LifeRuntime_nRuntimeBitmap
  (JNIEnv *, jclass, jobject);

#ifdef __cplusplus
}
#endif
#endif
