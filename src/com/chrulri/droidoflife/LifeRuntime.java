package com.chrulri.droidoflife;


/**
 * Droid of Life - Native Worker Class
 */
final class LifeRuntime {
	static final String TAG = LifeRuntime.class.getSimpleName();

	/* ************************************************************************************************************* */
	
	static final int OK = 0;
	static final int NOMEMORY = 1;

	static {
		System.loadLibrary("dol");
	}

	private static native int nRuntimeCreate(int width, int height);

	private static native void nRuntimeIterate();
	
	private static native void nRuntimeDestroy();
	
	/* ************************************************************************************************************* */
	
	private static native void nGLrender();
	
	private static native void nGLresize(int width, int heigth);

	/* ************************************************************************************************************* */

	private static LifeRuntime runtime = null;

	private int iteration;

	private LifeRuntime() {
		iteration = 0;
	}

	public static int getIteration() {
		if(runtime == null)
			return 0;
		return runtime.iteration;
	}
	
	/**
	 * creates runtime
	 * 
	 * @param width count of cells per row
	 * @param height count of rows of cells
	 * @throws LifeRuntimeException 
	 */
	public static void create(int width, int height) throws LifeRuntimeException {
		runtime = new LifeRuntime();
		int ret = nRuntimeCreate(width, height);
		if (ret != OK) {
			throw new LifeRuntimeException("failed to initialize the droid of life runtime", "_init", ret);
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
		return ++runtime.iteration;
	}

	/**
	 * destroy that beautiful place of life
	 * 
	 * @throws IllegalAccessException if runtime is not initialized yet
	 */
	public static void destroy() throws IllegalAccessException {
		checkRuntime();
		nRuntimeDestroy();
		runtime = null;
	}

	/**
	 * @throws IllegalAccessException if runtime is not initialized yet
	 */
	private static void checkRuntime() throws IllegalAccessException {
		if (runtime == null) {
			throw new IllegalAccessException("runtime is not initialized yet");
		}
	}

	/* ************************************************************************************************************* */
	
	/**
	 * tell native runtime to render the scene
	 */
	public static void render() {
		nGLrender();
	}

	/** 
	 * tell native runtime to resize the scene
	 */
	public static void resize(int width, int height) {
		nGLresize(width, height);
	}

	/* ************************************************************************************************************* */

	public static class LifeRuntimeException extends Exception {
		private static final long serialVersionUID = 1L;

		public LifeRuntimeException(String detailMessage, String nativeMethodName, int nativeReturnValue) {
			super(detailMessage, new NativeException(nativeMethodName, nativeReturnValue));
		}
	}

	public static class NativeException extends Exception {
		private static final long serialVersionUID = 1L;

		public NativeException(String methodName, int returnValue) {
			super("method " + methodName + " returns " + returnValue);
		}
	}
}
