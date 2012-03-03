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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class UpdateAlarmReceiver extends BroadcastReceiver{

	private String TAG = "PonyExpress UpdaterAlarmReceiver";
		
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.d(TAG,"Recieved update alarm!");
		
		//Start UpdaterSevice with UPDATE_ALL string
		intent = new Intent(context,UpdaterService.class);
		intent.putExtra(PonyExpressActivity.UPDATE_ALL, true);
		context.startService(intent);
	}
}
