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

import org.sixgun.ponyexpress.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

/**
 * Class that provides functions for checking if internet access is
 * available, and if so the type of access (WiFi of mobile).
 *
 */
public class InternetHelper {
	public static final int NO_CONNECTION = -1;
	public static final int DOWNLOAD_OK = 0;
	public static final int MOBILE_NOT_ALLOWED = 1;
	
	private Context mCtx;
	private ConnectivityManager mConnectivity;
	private NetworkInfo mInfo;
	
	public InternetHelper(Context ctx){
		mCtx = ctx;
		mConnectivity =  (ConnectivityManager) mCtx.getSystemService(
				Context.CONNECTIVITY_SERVICE);
	}
	
	/**
	 * Detect whether the phone has an internet connection.
	 * @return True if the phone can connect to the internet, false if not.
	 */
	public final boolean checkConnectivity(){
		mInfo = mConnectivity.getActiveNetworkInfo();
		if (mInfo == null){
			 return false;
		} else return true;	
	}
	
	/** Get the type of network connection currently active.
	 * @return Either TYPE_WIFI or TYPE_MOBILE, or NO_CONNECTION if no connection.
	 */
	public final int getConnectivityType(){
		mInfo = mConnectivity.getActiveNetworkInfo();
		if (mInfo != null){
			return mInfo.getType();
		} else {
			return NO_CONNECTION;
		}
	}
	
	/**
	 * Returns true if the preferences allow download over a mobile network or
	 * we are on wifi.
	 * @return
	 */
	public boolean isDownloadAllowed() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
		final boolean onlyOnWiFi = prefs.getBoolean(mCtx.getString(R.string.wifi_only_key), true);
		if (onlyOnWiFi && getConnectivityType() == ConnectivityManager.TYPE_MOBILE){
			return false;
		}
		return true;
	}
	
	/**
	 * Returns one of NO_CONNECTION, MOBILE_NOT_ALLOWED, DOWNLOAD_OK
	 * @return
	 */
	public int isDownloadPossible(){
		if (!checkConnectivity()){
			return InternetHelper.NO_CONNECTION;
		} else if (isDownloadAllowed()){
			return InternetHelper.DOWNLOAD_OK;
		}
		else return InternetHelper.MOBILE_NOT_ALLOWED;
	}
}
