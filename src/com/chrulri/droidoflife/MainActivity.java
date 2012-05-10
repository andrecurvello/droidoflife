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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.chrulri.droidoflife.LifeRuntime.LifeRuntimeException;

public class MainActivity extends SherlockFragmentActivity {
    static final String TAG = MainActivity.class.getSimpleName();

    static final int RESULT_SETTINGS = 0xF0;

    private IterationTask mIterationTask;
    private LifeView mLifeView;

    private void refreshTitle() {
        String title = "";
        // append iteration
        int iteration = LifeRuntime.getIteration();
        if (iteration > 0) {
            title += " #" + iteration;
        }
        // append automatic mode
        if (mIterationTask != null) {
            title += " - " + getText(R.string.auto_short);
        }
        setTitle(title);
    }

    private void restartRuntime() {
        LifeRuntime.destroy();

        // TODO ask for width/height
        final int width = 200;
        final int height = 100;

        try {
            LifeRuntime.create(width, height);
        } catch (LifeRuntimeException e) {
            Log.error(TAG, "restartRuntime()", e);
            // TODO show error

            // emergency exit
            finish();
            return;
        }

        mLifeView.createBitmap(width, height);

        refreshTitle();
    }

    private boolean doIteration() {
        try {
            LifeRuntime.iterate();
            mLifeView.performRender(true);
        } catch (IllegalAccessException e) {
            Log.error(TAG, "error on iteration", e);
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mLifeView = (LifeView) findViewById(R.id.main_lifeView);
        mLifeView.loadRuntimeSettings();

        restartRuntime();

        refreshTitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // restart iteration
        if (mIterationTask != null) {
            mIterationTask = new IterationTask();
            mIterationTask.execute();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // pause iteration
        if (mIterationTask != null) {
            mIterationTask.cancel(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remove iteration task
        mIterationTask = null;
        // destroy life with a nuclear bomb (!!)
        LifeRuntime.destroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_manually:
                Log.debug(TAG, "trigger manual iteration");
                // disable iteration task for manual mode
                if (mIterationTask != null) {
                    mIterationTask.cancel(false);
                    mIterationTask = null;
                } else {
                    doIteration();
                }
                refreshTitle();
                break;
            case R.id.mi_automatic:
                Log.debug(TAG, "toggle automatic mode");
                if (mIterationTask == null) {
                    mIterationTask = new IterationTask();
                    mIterationTask.execute();
                } else {
                    mIterationTask.cancel(false);
                    mIterationTask = null;
                }
                refreshTitle();
                return true;
            case R.id.mi_restart:
                Log.debug(TAG, "restart game of life");
                restartRuntime();
                return true;
            case R.id.mi_settings:
                Log.debug(TAG, "open settings activity");
                startActivityForResult(new Intent(this, SettingsActivity.class),
                        RESULT_SETTINGS);
                return true;
            case R.id.mi_help:
                Log.debug(TAG, "open help video");
                startActivity(new Intent(Intent.ACTION_VIEW, Setup.HELP_VIDEO_URI));
                return true;
            case R.id.mi_about:
                Log.debug(TAG, "open about dialog");
                AboutDialogFragment about = new AboutDialogFragment();
                about.show(getSupportFragmentManager(), null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_SETTINGS:
                mLifeView.loadRuntimeSettings();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class IterationTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onProgressUpdate(Void... values) {
            refreshTitle();
        }

        @Override
        protected Void doInBackground(Void... params) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
            wakeLock.acquire();
            try {
                while (!isCancelled()) {
                    // new generation ready, hurray!
                    if (!doIteration()) {
                        // exit on error
                        return null;
                    }
                    // tell everyone
                    publishProgress();
                    // sleep for next generation
                    try {
                        Thread.sleep(Setup.ITERATION_DELAY_MS);
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
            mIterationTask = null;
        }
    }
}
