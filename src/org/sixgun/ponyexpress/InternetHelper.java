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

package org.sixgun.ponyexpress;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Class that provides functions for checking if internet access is
 * available, and if so the type of access (WiFi of mobile).
 *
 */
public class InternetHelper {
	private Context mCtx;
	private ConnectivityManager mConnectivity;
	private NetworkInfo mInfo;
	
	public InternetHelper(Context ctx){
		mCtx = ctx;;
		mConnectivity =  (ConnectivityManager) mCtx.getSystemService(
				Context.CONNECTIVITY_SERVICE);
		mInfo = mConnectivity.getActiveNetworkInfo();
	}
	
	/**
	 * Detect whether the phone has an internet connection.
	 * @return True if the phone can connect to the internet, false if not.
	 */
	public final boolean checkConnectivity(){
		if (mInfo == null){
			 return false;
		} else return true;	
	}
}
