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

import org.sixgun.ponyexpress.service.ScheduledDownloadService;
import org.sixgun.ponyexpress.service.UpdaterService;
import org.sixgun.ponyexpress.util.InternetHelper;
import org.sixgun.ponyexpress.util.PonyExpressDbAdaptor;
import org.sixgun.ponyexpress.util.Bitmap.BitmapManager;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;

/**
 * Application class to provide an application context for the DbAdaptor
 * instance and InternetHelper that require a context and may live longer than 
 * their calling Activity/Service.
 */
public class PonyExpressApp extends Application {
	public static final String APPLICATION_NAME = "Pony Express";
	
	public static final String PODCAST_PATH = "/Android/data/org.sixgun.PonyExpress/files/";
	public static BitmapManager sBitmapManager;
	
	private PonyExpressDbAdaptor DbHelper;
	private InternetHelper mInternetHelper;

	

	/* (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		
		//Create/open database
		DbHelper = new PonyExpressDbAdaptor(this);
		DbHelper.open();
		//Set up InternetHelper
		mInternetHelper = new InternetHelper(this);
		sBitmapManager = new BitmapManager(this);
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

	/**
	 * @return the mInternetHelper
	 */
	public InternetHelper getInternetHelper() {
		return mInternetHelper;
	}
	
	/**
	 * This method checks to see if the Updater service is running.
	 * @return
	 * boolean
	 */
	public boolean isUpdaterServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (UpdaterService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	/** This method checks to see if the scheduled download service is running.
	 * 
	 */
	public boolean isScheduledDownloadServiceRunning(){
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
			if (ScheduledDownloadService.class.getName().equals(service.service.getClassName())){
				return true;
			}
		}
		return false;
	}

	
}
