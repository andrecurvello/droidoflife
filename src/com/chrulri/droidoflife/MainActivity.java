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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;

import com.chrulri.droidoflife.LifeRuntime.LifeRuntimeException;

public class MainActivity extends FragmentActivity {
	static final String TAG = MainActivity.class.getSimpleName();

	static final int RESULT_SETTINGS = 0xF0;

	static final long ITERATION_DELAY_MS = 100;

	private IterationTask iterationTask;
	private SurfaceHolder surface;
	private Bitmap bitmap;
	private int settingsCache;

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
	
	private void restartRuntime() {
		if(bitmap != null) {
			LifeRuntime.destroy();
		}

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

		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		refreshTitle();
	}

	private boolean doIteration() {
		try {
			LifeRuntime.iterate();
			doRender();
		} catch (IllegalAccessException e) {
			Log.e(TAG, "error on iteration", e);
			return false;
		}
		return true;
	}

	private void doRender() {
		Canvas canvas = surface.lockCanvas();

		LifeRuntime.render(bitmap, settingsCache);

		Rect dst = surface.getSurfaceFrame();
		canvas.drawBitmap(bitmap, null, dst, null);

		surface.unlockCanvasAndPost(canvas);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getSupportActionBar().setDisplayShowHomeEnabled(false);

		settingsCache = SettingsActivity.loadSettings(this);

		SurfaceView view = (SurfaceView) findViewById(R.id.main_surfaceView);
		surface = view.getHolder();

		restartRuntime();

		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// disable iteration task for manual mode
				if (iterationTask != null) {
					iterationTask.cancel(false);
					iterationTask = null;
				} else {
					doIteration();
				}
				refreshTitle();
			}
		});
		surface.addCallback(new SurfaceHolder.Callback() {
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
		surface = null;
		bitmap = null;
		// destroy life with a nuclear bomb (!!)
		LifeRuntime.destroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.mi_automatic:
			// toggle automatic mode
			if (iterationTask == null) {
				iterationTask = new IterationTask();
				iterationTask.execute();
			} else {
				iterationTask.cancel(false);
				iterationTask = null;
			}
			refreshTitle();
			return true;
		case R.id.mi_restart:
			// restart game of life
			restartRuntime();
			doRender();
			return true;
		case R.id.mi_settings:
			// open settings activity
			startActivityForResult(new Intent(this, SettingsActivity.class), RESULT_SETTINGS);
			return true;
		case R.id.mi_help:
			// open help dialog
			DialogFragment help = new HelpDialogFragment();
			help.show(getSupportFragmentManager(), null);
			return true;
		case R.id.mi_about:
			// open about dialog
			DialogFragment about = new AboutDialogFragment();
			about.show(getSupportFragmentManager(), null);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	  switch(requestCode) {
	  case RESULT_SETTINGS:
	    settingsCache = SettingsActivity.loadSettings(this);
	    break;
	  default:
	    super.onActivityResult(requestCode, resultCode, data);
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
					if(!doIteration()) {
						// exit on error
						return null;
					}
					// tell everyone
					publishProgress();
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

		@Override
		protected void onPostExecute(Void result) {
			// clean exit on error
			iterationTask = null;
		}
	}
}
