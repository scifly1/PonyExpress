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

import android.app.Application;

/**
 * Application class to provide an application context for the DbAdaptor
 * instance and other objects that require a context and may live longer than 
 * there calling object:  ie: progress bars.
 */
public class PonyExpressApp extends Application {
	private PonyExpressDbAdaptor DbHelper;

	

	/* (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		
		//Create/open database
		DbHelper = new PonyExpressDbAdaptor(this);
		DbHelper.open();
	}

	/* (non-Javadoc)
	 * @see android.app.Application#onTerminate()
	 */
	@Override
	public void onTerminate() {
		super.onTerminate();
		DbHelper.close();
	}
	/**
	 * @return the mDbHelper
	 */
	public PonyExpressDbAdaptor getDbHelper() {
		return DbHelper;
	}

	
}
