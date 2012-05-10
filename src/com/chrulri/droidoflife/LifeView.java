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
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class LifeView extends SurfaceView {
    static final String TAG = LifeView.class.getSimpleName();

    private int mSettings;
    private Bitmap mBitmap;
    private final Matrix mMatrix = new Matrix();
    private final RectF mBounds = new RectF();
    private final RectF mSource = new RectF();
    private GestureDetector mDragGesture;
    private ScaleGestureDetector mZoomGesture;

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
                performRender(false);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                    int height) {
                mBounds.set(0, 0, width, height);
                mMatrix.setRectToRect(mSource, mBounds, ScaleToFit.CENTER);
                performRender(false);
            }
        });

        mDragGesture = new GestureDetector(getContext(),
                new SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        performBirth(e.getX(), e.getY());
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                            float velocityX, float velocityY) {
                        return false;
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                            float distanceX, float distanceY) {
                        performDrag(distanceX, distanceY);
                        return true;
                    }
                });

        mZoomGesture = new ScaleGestureDetector(getContext(),
                new ScaleGestureDetector.OnScaleGestureListener() {
                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {
                        performZoom(detector.getFocusX(), detector.getFocusY(),
                                detector.getScaleFactor());
                    }

                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        return true;
                    }

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        performZoom(detector.getFocusX(), detector.getFocusY(),
                                detector.getScaleFactor());
                        return true;
                    }
                });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDragGesture.onTouchEvent(event)) {
            return true;
        }
        if (mZoomGesture.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void createBitmap(int width, int height) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mSource.set(0, 0, width, height);
        mMatrix.setRectToRect(mSource, mBounds, ScaleToFit.CENTER);
        performRender(true);
    }

    public void loadRuntimeSettings() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        mSettings = 0;
        if (prefs.getBoolean(Setup.PREF_SHOW_DEATHBIRTH, false)) {
            mSettings |= (1 << LifeRuntime.SETTINGS_SHOW_DEATHBIRTH);
        }
        performRender(true);
    }

    public void performRender(boolean renderLife) {
        if (mBitmap != null) {
            if (renderLife) {
                LifeRuntime.render(mBitmap, mSettings);
            }

            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(mBitmap, mMatrix, null);
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    public void performZoom(float centerX, float centerY, float zoom) {
        mMatrix.postScale(zoom, zoom, centerX, centerY);
        // validate matrix
        float[] values = new float[9];
        mMatrix.getValues(values);
        float widthS = values[Matrix.MSCALE_X] * mSource.width();
        float heightS = values[Matrix.MSCALE_Y] * mSource.height();
        if (widthS < mBounds.width() && heightS < mBounds.height()) {
            mMatrix.setRectToRect(mSource, mBounds, ScaleToFit.CENTER);
        }

        validateMatrix();
        performRender(false);
    }

    public void performDrag(float deltaX, float deltaY) {
        mMatrix.postTranslate(-deltaX, -deltaY);

        validateMatrix();
        performRender(false);
    }

    private void validateMatrix() {
        float[] values = new float[9];
        mMatrix.getValues(values);
        float minLeft = mBounds.width() - (values[Matrix.MSCALE_X] * mSource.width());
        float minTop = mBounds.height() - (values[Matrix.MSCALE_Y] * mSource.height());

        float left = Math.min(values[Matrix.MTRANS_X], 0);
        if (left < minLeft) {
            left = minLeft;
            if (left > 0) {
                left /= 2; // center
            }
        }

        float top = Math.min(values[Matrix.MTRANS_Y], 0);
        if (top < minTop) {
            top = minTop;
            if (top > 0) {
                top /= 2; // center
            }
        }

        values[Matrix.MTRANS_X] = left;
        values[Matrix.MTRANS_Y] = top;
        mMatrix.setValues(values);
    }

    public void performBirth(float x, float y) {
        // TODO implement. issue #5
    }

}
