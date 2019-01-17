/*
 * Copyright 2014 Tampere University of Technology, Pori Department
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.otula.utils;

import android.util.Log;

/**
 * logging wrapper
 *
 */
public final class LogUtils {
	public static final String TAG = "OTULA_LOGGER";
	
	private LogUtils(){
		// nothing needed
	}
	
	/**
	 * 
	 * @param className
	 * @param methodName
	 * @param message
	 */
	public static void debug(String className, String methodName, String message){
		Log.d(TAG, String.valueOf(className)+'.'+String.valueOf(methodName)+": "+String.valueOf(message));
	}
	
	/**
	 * 
	 * @param className
	 * @param methodName
	 * @param message
	 */
	public static void error(String className, String methodName, String message){
		Log.e(TAG, String.valueOf(className)+'.'+String.valueOf(methodName)+": "+String.valueOf(message));
	}
	
	/**
	 * 
	 * @param className
	 * @param methodName
	 * @param message
	 */
	public static void warn(String className, String methodName, String message){
		Log.w(TAG, String.valueOf(className)+'.'+String.valueOf(methodName)+": "+String.valueOf(message));
	}
	
	/**
	 * print test message
	 * 
	 * @param message
	 */
	@Deprecated
	public static void test(Object message){
		Log.d(TAG, "TEST: "+String.valueOf(message));
	}
}
