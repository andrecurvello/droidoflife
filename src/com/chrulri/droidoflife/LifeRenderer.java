package com.chrulri.droidoflife;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

class LifeRenderer implements GLSurfaceView.Renderer {

	@Override
	public void onDrawFrame(GL10 gl) {
		LifeRuntime.render();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		LifeRuntime.resize(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// nothing to do
	}
}