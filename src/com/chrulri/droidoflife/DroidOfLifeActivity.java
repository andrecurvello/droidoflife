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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.chrulri.droidoflife.LifeRuntime.LifeRuntimeException;

public class DroidOfLifeActivity extends Activity {
	static final String TAG = DroidOfLifeActivity.class.getSimpleName();

	static final long ITERATION_DELAY_MS = 100;

	private IterationTask iterationTask;
	private SurfaceView view;
	private Bitmap bitmap;

	private void refreshTitle() {
		String title = "" + getText(R.string.app_name);
		// append iteration
		int iteration = LifeRuntime.getIteration();
		if (iteration > 0) {
			title += " (#" + iteration + ")";
		}
		// append automatic mode
		if (iterationTask != null) {
			title += " - " + getText(R.string.auto_short);
		}
		setTitle(title);
	}
	
	private void doRender() {
		Canvas canvas = view.getHolder().lockCanvas();

		LifeRuntime.render(bitmap);

		Rect dst = new Rect(0, 0, view.getWidth(), view.getHeight());
		canvas.drawBitmap(bitmap, null, dst, null);

		view.getHolder().unlockCanvasAndPost(canvas);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO ask for width/height
		final int width = 200;
		final int height = 100;

		try {
			LifeRuntime.create(width, height);
		} catch (LifeRuntimeException e) {
			e.printStackTrace();
			// TODO show error

			// emergency exit
			finish();
			return;
		}
		view = new SurfaceView(this);
		view.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (iterationTask == null) {
					iterationTask = new IterationTask();
					iterationTask.execute();
				} else {
					iterationTask.cancel(false);
					iterationTask = null;
				}
				refreshTitle();
				return true;
			}
		});
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// disable iteration task for manual mode
				if (iterationTask != null) {
					iterationTask.cancel(false);
					iterationTask = null;
				} else {
					// manual iteration
					try {
						LifeRuntime.iterate();
						doRender();
					} catch (IllegalAccessException e) {
						Log.e(TAG, "ManualMode", e);
					}
				}
				refreshTitle();
			}
		});
		view.getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
				doRender();
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				doRender();
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				// ignore
			}
			
		});

		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		
		setContentView(view);

		refreshTitle();

		// Manual
		// TODO show manual
	}

	@Override
	protected void onResume() {
		super.onResume();
		// restart iteration
		if (iterationTask != null) {
			iterationTask = new IterationTask();
			iterationTask.execute();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// pause iteration
		if (iterationTask != null) {
			iterationTask.cancel(false);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// remove iteration task
		iterationTask = null;
		view = null;
		bitmap = null;
		// destroy life with a nuclear bomb (!!)
		try {
			LifeRuntime.destroy();
		} catch (IllegalAccessException e) {
			// ignore
		}
	}

	class IterationTask extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected void onProgressUpdate(Void... values) {
			refreshTitle();
		}

		@Override
		protected Void doInBackground(Void... params) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
			wakeLock.acquire();
			try {
				while (!isCancelled()) {
					// new generation ready, hurray!
					try {
						LifeRuntime.iterate();
						doRender();
						// tell everyone
						publishProgress();
					} catch (IllegalAccessException e) {
						Log.e(TAG, "IterationTask", e);
						return null;
					}
					// sleep for next generation
					try {
						Thread.sleep(ITERATION_DELAY_MS);
					} catch (InterruptedException e) {
						// ignore
					}
				}
				return null;
			} finally {
				wakeLock.release();
			}
		}
	}
}
