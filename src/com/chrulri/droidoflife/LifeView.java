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
package com.chrulri.droidoflife;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class LifeView extends SurfaceView {
	static final String TAG = LifeView.class.getSimpleName();

	private int mSettings;
	private Bitmap mBitmap;
	private Matrix mMatrix = new Matrix();
	private RectF mBounds = new RectF();
	private RectF mSource = new RectF();

	public LifeView(Context context) {
		super(context);
		init();
	}

	public LifeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public LifeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				// ignore
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d(TAG, "surfaceCreated(" + holder + ")");
				performRender();
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
				Log.d(TAG, "surfaceChanged(" + holder + "," + format + ","
						+ width + "," + height + ")");
				mBounds.set(0, 0, width, height);
				mMatrix.setRectToRect(mSource, mBounds, ScaleToFit.CENTER);
				performRender();
			}
		});
	}

	public void createBitmap(int width, int height) {
		Log.d(TAG, "createBitmap(" + width + "," + height + ")");
		mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		mSource.set(0, 0, width, height);
		mMatrix.setRectToRect(mSource, mBounds, ScaleToFit.CENTER);
		performRender();
	}

	public void loadRuntimeSettings() {
		Log.d(TAG, "loadRuntimeSettings");
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getContext());
		mSettings = 0;
		if (prefs.getBoolean(Setup.PREF_SHOW_DEATHBIRTH, false)) {
			mSettings |= (1 << LifeRuntime.SETTINGS_SHOW_DEATHBIRTH);
		}
		performRender();
	}

	public void performRender() {
		if (mBitmap != null) {
			LifeRuntime.render(mBitmap, mSettings);

			Canvas canvas = getHolder().lockCanvas();
			if (canvas != null) {
				canvas.drawBitmap(mBitmap, mMatrix, null);
				getHolder().unlockCanvasAndPost(canvas);
			}
		}
	}
}
