/*
 * Copyright 2012 Paul Elms
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
package org.sixgun.ponyexpress.receiver;

import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class RescheduledUpdateReceiver extends UpdateAlarmReceiver {
	
	private String TAG = "RescheduledUpdateReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		
		PonyLogger.d(TAG, "Recieved connectivity change!");
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()){
			PonyLogger.i(TAG, "Internet connected");
			//disable this receiver now it has done it's job. 
			ComponentName receiver = new ComponentName(context, RescheduledUpdateReceiver.class);
			PackageManager pm = context.getPackageManager();
			pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 
				PackageManager.DONT_KILL_APP);
			//Trigger the update.
			super.onReceive(context, intent);	
		} else {
			PonyLogger.i(TAG, "No Internet connection");
			return;
		}
		
	}
	
	
	
}
