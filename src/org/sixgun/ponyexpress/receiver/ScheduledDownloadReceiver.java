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

import org.sixgun.ponyexpress.service.ScheduledDownloadService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;


public class ScheduledDownloadReceiver extends BroadcastReceiver {

	private String TAG = "ScheduledDownloadReceiver";
	private static final String WIFI_LOCK = "Pony Express scheduled download wifi lock";

	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG,"Recieved scheduled download alarm!");
		
		//If device is asleep when alarm triggered it may go back
		// to sleep before the service is started so we need a wakelock.
		if (ScheduledDownloadService.sWakeLock == null){
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			ScheduledDownloadService.sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		}
		if (!ScheduledDownloadService.sWakeLock.isHeld()){
			Log.d(TAG, "Acquiring wake lock");
			ScheduledDownloadService.sWakeLock.acquire();
		}
		//Get a wifiLock to use the wifi
		if (ScheduledDownloadService.sWifiLock == null){
			WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
			ScheduledDownloadService.sWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,WIFI_LOCK);
		}
		if (!ScheduledDownloadService.sWifiLock.isHeld()){
			Log.d(TAG, "Acquiring Wifi lock");
			ScheduledDownloadService.sWifiLock.acquire();
		}
		//Start SheduledDownloadServiceSevice
		intent = new Intent(context,ScheduledDownloadService.class);
		context.startService(intent);
	}

}
