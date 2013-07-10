/*
 * Copyright 2013 James Daws
 *
 *  This file is part of PonyExpress.
 *
 *  PonyExpress is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PonyExpress is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PonyExpress.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.sixgun.ponyexpress.util;

import org.sixgun.ponyexpress.BuildConfig;
import android.util.Log;

public class PonyLogger {
	
	public static void d(String tag, String msg){
		if (BuildConfig.DEBUG) {
			Log.d(tag, msg);
		}
	}
	
	public static void e(String tag, String msg, Throwable error){
		Log.e(tag, msg, error);
	}
	
	public static void e(String tag, String msg){
		Log.e(tag, msg);
	}
	
	public static void i(String tag, String msg){
		Log.i(tag, msg);
	}
	
	public static void v(String tag, String msg){
		Log.v(tag, msg);
	}
	
	public static void w(String tag, String msg){
		Log.w(tag, msg);
	}
	
	public static void w(String tag, String msg, Throwable error){
		Log.w(tag, msg, error);
	}
	
	public static void wtf(String tag, String msg){
		Log.wtf(tag, msg);
	}
}
