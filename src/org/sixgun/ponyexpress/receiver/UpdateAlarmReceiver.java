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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateAlarmReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		
		checkConnectivity();
		//TODO handle returns
		
		checkForAnotherUpdater();
		//TODO handle returns
		
		checkIfInBackground();
		//TODO handle returns
		
		startUpdaterService();
		//TODO Something productive!?!
		
		
	}

	private void checkIfInBackground() {
		// TODO Auto-generated method stub
		
	}

	private void startUpdaterService() {
		// TODO Auto-generated method stub
		
	}

	private void checkForAnotherUpdater() {
		// TODO Auto-generated method stub
		
	}

	private void checkConnectivity() {
		// TODO Auto-generated method stub
		
	}

}
