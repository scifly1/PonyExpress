/*
 * Copyright 2010 Paul Elms
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

import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

/**
 * Utility class with general utility methods.
 *
 */
public class Utils {
	
	private static final String TAG = "PonyExpressUtils";

	/**
	 * Formats a time in millisecods into a h:mm:ss string
	 * @param milliseconds
	 * @return h:mm:ss string
	 */
	static public String milliToTime(int milliseconds){
		int seconds = (milliseconds / 1000);
		int minutes = seconds / 60;
		int hours = minutes / 60;
		//Extract the remainder in each case to 
		//get the number of hours,minutes,seconds
		seconds = seconds % 60;
		minutes = minutes % 60;
		hours = hours % 60;
		
		return String.format("%d:%02d:%02d", hours,minutes,seconds);
	}

	/**
	 * Parse the url string to a URL type.
	 * @param _url string from the Intent.
	 * @return URL object.
	 */
	static public URL getURL(String _url) {
		URL url;
		try {
			url = new URL(_url);
		} catch (MalformedURLException e) {
			Log.e(TAG, "Episode URL badly formed.", e);
			return null;
		}
		return url;
	}
	
	/**
	 * Strips words from the end of strings eg: "Ogg Feed"
	 * @param string String to strip from.
	 * @param to_strip string to strip.
	 */
	static public String stripper(String string, String to_strip){
		if (string.endsWith(to_strip)){
			String stripped = string.replace(to_strip,"");
			return stripped;
		}else {
			return string;
		}
	}
}
