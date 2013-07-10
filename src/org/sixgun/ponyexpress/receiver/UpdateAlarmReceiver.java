/*
 * Copyright 2012 James Daws
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


import org.sixgun.ponyexpress.activity.PonyExpressActivity;
import org.sixgun.ponyexpress.service.UpdaterService;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;


public class UpdateAlarmReceiver extends BroadcastReceiver{

	private String TAG = "PonyExpress UpdaterAlarmReceiver";
		
	@Override
	public void onReceive(Context context, Intent intent) {
		
		PonyLogger.d(TAG, "Recieved update alarm!");
		//If device is asleep when alarm triggered it may go back
		// to sleep before the service is started so we need a wakelock.
		if (UpdaterService.sWakeLock == null){
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			UpdaterService.sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		}
		if (!UpdaterService.sWakeLock.isHeld()){
			PonyLogger.d(TAG, "Acquiring wake lock");
			UpdaterService.sWakeLock.acquire();
		}
		//Start UpdaterSevice with UPDATE_ALL string
		intent = new Intent(context,UpdaterService.class);
		intent.putExtra(PonyExpressActivity.UPDATE_ALL, true);
		context.startService(intent);
	}
}
