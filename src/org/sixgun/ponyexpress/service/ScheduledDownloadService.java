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
package org.sixgun.ponyexpress.service;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.PonyExpressActivity;
import org.sixgun.ponyexpress.receiver.ScheduledDownloadReceiver;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;


public class ScheduledDownloadService extends IntentService {

	public static WakeLock sWakeLock;
	private String TAG = "PonyExpress ScheduledDownloadService";


	public ScheduledDownloadService() {
		super("ScheduledDownloadService");
	}

	/* (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "Scheduled download service started.");
		
		try {
			// See if we are to set the alarm only (after a reboot) or if we have to check 
			//for downloads
			Bundle data = intent.getExtras();
			boolean set_alarm_only = false;
			if (data != null){
				set_alarm_only = data.getBoolean(PonyExpressActivity.SET_ALARM_ONLY);
			}
			final long nextUpdate = getNextUpdateTime();
			if (nextUpdate == 0){
				Log.d(TAG, "Scheduled downloads not active");
				return;
			}
			if (set_alarm_only && nextUpdate >= System.currentTimeMillis() ){
				setNextAlarm();
			}else{
				//TODO Check for downloads
				Log.d(TAG, "Checking for downloads");
				//TODO Start downloads
				Log.d(TAG, "Starting scheduled downloads");
				
				//Set the next update alarm
				setNextAlarm();
			}

			
		} finally {
			if (sWakeLock != null){
				if (sWakeLock.isHeld()){
					sWakeLock.release();
					Log.d(TAG, "Releasing wakelock");
					Log.d(TAG,"Scheduled Downloader stopped");
				}
				sWakeLock = null;
			}
		}
		
	}

	

	/**
	 * This method sets the alarm manger based on the last update and the
	 * preferences.
	 */
	private void setNextAlarm() {
		if(checkBackgroundUpdate()){
			final Long updateTime = getNextUpdateTime();
			final AlarmManager alarm_mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			final Intent intent = new Intent(this, ScheduledDownloadReceiver.class);
			final PendingIntent pending_intent = PendingIntent.getBroadcast(this, 0, intent, 0);
			alarm_mgr.set(AlarmManager.RTC_WAKEUP, updateTime, pending_intent);
			Log.d(TAG, "Downloads scheduled for: " + Long.toString(updateTime));
		}else{
			Log.d(TAG, "No background downloads scheduled");
		}				
		
	}

	private boolean checkBackgroundUpdate() {
		SharedPreferences prefs = getPreferences();
		try{
			Long.parseLong(prefs.getString(getString(R.string.schedule_downloads_key), "12"));
		}catch (NumberFormatException e){
			return false;
		}
		return true;
		
	}

	/**
	 * This method returns the next time an scheduled download should happen based
	 * on the preferences in the form of a long in milliseconds. If downloads are not
	 * scheduled 0 is returned.
	 * @return
	 */
	private long getNextUpdateTime() {
		if (checkBackgroundUpdate()){
			SharedPreferences prefs = getPreferences();
			int updateTime = Integer.parseInt(prefs.getString(getString(R.string.schedule_downloads_key), "0"));
			Calendar cal = new GregorianCalendar();
			cal.set(Calendar.HOUR_OF_DAY, updateTime);
			cal.set(Calendar.MINUTE, 00);
			//Check if we want tomorrow
			if (System.currentTimeMillis() > cal.getTimeInMillis() - 60000){
				//-60000 so that a call to getNextUpdateTime from setNextAlarm after an
				//alarm doesn't set an alarm for 'now' triggering an new alarm.
				cal.roll(Calendar.DAY_OF_YEAR,true);
			}
			Log.d(TAG, "Date: " + cal.get(Calendar.DATE));
			Log.d(TAG, "Hour: " + cal.get(Calendar.HOUR_OF_DAY));
			Log.d(TAG, "Min: " + cal.get(Calendar.MINUTE));
			Log.d(TAG, "millis: " + cal.getTimeInMillis());
			return cal.getTimeInMillis();
		} else return 0; 
	}
	
	private SharedPreferences getPreferences(){
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

}
