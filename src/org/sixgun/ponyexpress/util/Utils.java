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

/**
 * Utility class with general utility methods.
 *
 */
public class Utils {
	
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

}
