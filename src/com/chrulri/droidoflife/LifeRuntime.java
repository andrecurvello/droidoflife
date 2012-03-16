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

import android.graphics.Bitmap;

/**
 * Droid of Life - Native Worker Class
 */
final class LifeRuntime {
    static final String TAG = LifeRuntime.class.getSimpleName();

    /* ************************************************************************************************************* */

    static final int OK = 0;
    static final int E_INVALID_SIZE = 1;

    static final int SETTINGS_SHOW_DEATHBIRTH = 0;

    static {
        System.loadLibrary("dol");
    }

    private static native int nRuntimeCreate(int width, int height);

    private static native void nRuntimeIterate();

    private static native void nRuntimeDestroy();

    private static native void nRuntimeBitmap(Bitmap bmp, int settings);

    /* ************************************************************************************************************* */

    private static LifeRuntime mRUNTIME = null;

    private int mIteration;

    private LifeRuntime() {
        mIteration = 0;
    }

    public static int getIteration() {
        if (mRUNTIME == null)
            return 0;
        return mRUNTIME.mIteration;
    }

    /**
     * creates runtime
     * 
     * @param width count of cells per row
     * @param height count of rows of cells
     * @throws LifeRuntimeException
     */
    public static void create(int width, int height) throws LifeRuntimeException {
        mRUNTIME = new LifeRuntime();
        int ret = nRuntimeCreate(width, height);
        if (ret != OK) {
            throw new LifeRuntimeException(
                    "failed to initialize the droid of life runtime", "_init", ret);
        }
    }

    /**
     * iterate through one generation of life
     * 
     * @return number of generation
     * @throws IllegalAccessException if runtime is not initialized yet
     */
    public static int iterate() throws IllegalAccessException {
        checkRuntime();
        nRuntimeIterate();
        return ++mRUNTIME.mIteration;
    }

    /**
     * tell native runtime to render the scene
     */
    public static void render(Bitmap bmp, int settings) {
        nRuntimeBitmap(bmp, settings);
    }

    /**
     * destroy that beautiful place of life
     */
    public static void destroy() {
        nRuntimeDestroy();
        mRUNTIME = null;
    }

    /**
     * @throws IllegalAccessException if runtime is not initialized yet
     */
    private static void checkRuntime() throws IllegalAccessException {
        if (mRUNTIME == null) {
            throw new IllegalAccessException("runtime is not initialized yet");
        }
    }

    /* ************************************************************************************************************* */

    public static class LifeRuntimeException extends Exception {
        private static final long serialVersionUID = 1L;

        public LifeRuntimeException(String detailMessage, String nativeMethodName,
                int nativeReturnValue) {
            super(detailMessage, new NativeException(nativeMethodName,
                    nativeReturnValue));
        }
    }

    public static class NativeException extends Exception {
        private static final long serialVersionUID = 1L;

        public NativeException(String methodName, int returnValue) {
            super("method " + methodName + " returns " + returnValue);
        }
    }
}
