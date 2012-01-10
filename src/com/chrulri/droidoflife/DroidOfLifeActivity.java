package com.chrulri.droidoflife;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

import com.chrulri.droidoflife.LifeRuntime.LifeRuntimeException;

public class DroidOfLifeActivity extends Activity {
	static final String TAG = DroidOfLifeActivity.class.getSimpleName();
	
	static final long ITERATION_DELAY_MS = 500;

	private boolean manualMode;
	private IterationTask iterationTask;
	private GLSurfaceView glView;
	
	private void refreshTitle() {
		String title = "" + getText(R.string.app_name);
		// append iteration
		int iteration = LifeRuntime.getIteration();
		if(iteration > 0) {
			title += " (#" + iteration + ")";
		}
		// append manual / automatic mode
		if(manualMode) {
			CharSequence mm = getText(R.string.mm_short);
			title += " - " + mm;
		} else if(iterationTask != null) {
			CharSequence auto = getText(R.string.auto_short);
			title += " - " + auto;
		}

		setTitle(title);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		manualMode = false;
		
		// TODO ask for width/height
		final int width = 32;
		final int height = 32;

		try {
			LifeRuntime.create(width, height);
		} catch (LifeRuntimeException e) {
			e.printStackTrace();
			// TODO show error
			
			// emergency exit
			finish();
			return;
		}
		glView = new GLSurfaceView(this);
		glView.setRenderer(new LifeRenderer());
		glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		glView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if(manualMode) {
					// manual iteration
					try {
						LifeRuntime.iterate();
						glView.requestRender();
					} catch (LifeRuntimeException e) {
						Log.e(TAG, "ManualMode", e);
					} catch (IllegalAccessException e) {
						Log.e(TAG, "ManualMode", e);
					}
				} else if(iterationTask == null) {
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

		glView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				manualMode = !manualMode;
				if(manualMode) {
					// disable iteration task for manual mode
					if(iterationTask != null) {
						iterationTask.cancel(false);
						iterationTask = null;
					}
					Toast.makeText(DroidOfLifeActivity.this, R.string.mm_active, Toast.LENGTH_SHORT);
				} else {
					Toast.makeText(DroidOfLifeActivity.this, R.string.mm_inactive, Toast.LENGTH_SHORT);
				}
				refreshTitle();
			}
		});

		setContentView(glView);

		refreshTitle();
		
		// Manual
		// TODO show manual
	}

	@Override
	protected void onResume() {
		super.onResume();
		// restart iteration
		if(iterationTask != null) {
			iterationTask = new IterationTask();
			iterationTask.execute();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		// pause iteration
		if(iterationTask != null) {
			iterationTask.cancel(false);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// remove iteration task
		iterationTask = null;
		// remove OpenGL view
		setContentView(null);
		glView = null;
		// destroy life with a nuclear bomb (!!)
		try {
			LifeRuntime.destroy();
		} catch (LifeRuntimeException e) {
			Log.e(TAG, "onStop", e);
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
			while(!isCancelled()) {
				// new generation ready, hurray!
				try {
					LifeRuntime.iterate();
					glView.requestRender();
					// tell everyone
					publishProgress();
				} catch (LifeRuntimeException e) {
					Log.e(TAG, "IterationTask", e);
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
		}
	}
}
