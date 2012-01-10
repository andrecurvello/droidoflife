package com.chrulri.droidoflife;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class LifeGLView extends GLSurfaceView {

	public LifeGLView(Context context) {
		super(context);
		setRenderer(new LifeRenderer());
		setRenderMode(RENDERMODE_WHEN_DIRTY);
	}
	
	class LifeRenderer implements Renderer {

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
}