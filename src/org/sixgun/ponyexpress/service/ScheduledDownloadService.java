/*
 * Copyright 2012-13 Paul Elms
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.sixgun.ponyexpress.DownloadingEpisode;
import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.PonyExpressActivity;
import org.sixgun.ponyexpress.receiver.ScheduledDownloadReceiver;
import org.sixgun.ponyexpress.util.InternetHelper;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;


public class ScheduledDownloadService extends IntentService {
	
	private static final int NOTIFY_ID = 0;
	public static WakeLock sWakeLock;
	private String TAG = "PonyExpress ScheduledDownloadService";
	private PonyExpressApp mPonyExpressApp;
	private DownloaderService mDownloader;
	private boolean mDownloaderBound;
	public static WifiLock sWifiLock;


	public ScheduledDownloadService() {
		super("ScheduledDownloadService");
	}

	/* (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		PonyLogger.d(TAG, "Scheduled download service started.");
		mPonyExpressApp = (PonyExpressApp) getApplication();
		
		try {
			// See if we are to set the alarm only (after a reboot) or if we have to check 
			//for downloads
			Bundle data = intent.getExtras();
			boolean set_alarm_only = false;
			if (data != null){
				set_alarm_only = data.getBoolean(PonyExpressActivity.SET_ALARM_ONLY);
			}
			final long nextUpdate = getNextUpdateTime();
			PonyLogger.d(TAG, "Next download scheduled for: " + nextUpdate);
						
			if (nextUpdate == 0){
				PonyLogger.d(TAG, "Scheduled downloads not active");
				return;
			}
			if (set_alarm_only && nextUpdate >= System.currentTimeMillis() ){
				setNextAlarm(nextUpdate);
			}else{
				//Update the feeds to ensure they are current.
				if (!mPonyExpressApp.isUpdaterServiceRunning()){
					Intent up_intent = new Intent(mPonyExpressApp,UpdaterService.class);
					up_intent.putExtra(PonyExpressActivity.UPDATE_ALL, true);
					startService(up_intent);
				}
				while (mPonyExpressApp.isUpdaterServiceRunning()){
					//wait for it to finish
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						PonyLogger.e(TAG, "Interrupted waiting for updaterservice to finish");
					}
				}
				PonyLogger.d(TAG, "Checking for downloads");
				//Get list of podcasts and find undownloaded episodes in each
				ArrayList<DownloadingEpisode> downloads = getEpisodesToDownload();
				if (!downloads.isEmpty()){
					if (mPonyExpressApp.getInternetHelper().getConnectivityType() !=
							ConnectivityManager.TYPE_WIFI){
						//Either NO_CONNECTION or TYPE_MOBILE so
						PonyLogger.d(TAG, "Wait for WIFI");
						try {
							//Wait for WIFI to come up
							Thread.sleep(60000);
						} catch (InterruptedException e) {
							PonyLogger.e(TAG,"Interupted sleep waiting for wifi to come on");
						}
					}
					switch (mPonyExpressApp.getInternetHelper().getConnectivityType()){
					case InternetHelper.NO_CONNECTION:
						PonyLogger.d(TAG, "No connection for scheduled download");
						NotifyError(getString(R.string.no_connection_for_sched_download));
						setNextAlarm(nextUpdate);
						return;
					case ConnectivityManager.TYPE_MOBILE:
						if (!mPonyExpressApp.getInternetHelper().isDownloadAllowed()){
							PonyLogger.d(TAG, "Scheduled downloads not allowed on mobile network");
							NotifyError(getString(R.string.prefs_dont_allow_downloads));
							setNextAlarm(nextUpdate);
							return;
						}
						//Fallthrough, download allowed on mobile network
					case ConnectivityManager.TYPE_WIFI:
						PonyLogger.d(TAG, "Starting scheduled downloads");
						doBindDownloaderService();
						//Send intent for each episode to download with packaged episode
						for (DownloadingEpisode episode: downloads){
							Intent download_intent = new Intent(this,DownloaderService.class);
							Bundle bundle = Episode.packageEpisode(mPonyExpressApp, episode.getPodcastName(), episode.getRowID());
							download_intent.putExtras(bundle);
							download_intent.putExtra("action", DownloaderService.DOWNLOAD);
							startService(download_intent);
						}
						if (mDownloader == null){
							//Wait for the connection to be made
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								PonyLogger.e(TAG, "Interupted sleep waiting for Downloader to bind");
							}
						} 
						do {  //wait for downloads to finish
							try {
								Thread.sleep(10000);
							} catch (InterruptedException e) {
								PonyLogger.e(TAG,"Interupted sleep while downloading");
							}
						}
						while (mDownloader != null && mDownloader.isDownloading());
						doUnbindDownloaderService();
					}	
					setNextAlarm(nextUpdate);
				}
			}


		} finally {
			//release wifilock
			if (sWifiLock != null){
				if (sWifiLock.isHeld()){
					sWifiLock.release();
					PonyLogger.d(TAG, "Releasing wifilock");
				}
				sWifiLock = null;
			}
			if (sWakeLock != null){
				if (sWakeLock.isHeld()){
					sWakeLock.release();
					PonyLogger.d(TAG, "Releasing wakelock");
				}
				sWakeLock = null;
			}
		}
		PonyLogger.d(TAG, "Scheduled Downloader stopped");
	}

	

	/**
	 * This method sets the alarm manger based on the last update and the
	 * preferences.
	 */
	private void setNextAlarm(long update_time) {
		if(checkBackgroundUpdate()){
			long updateTime = update_time;
			if (updateTime < System.currentTimeMillis()){
				//Shouldn't happen
				NotifyError(getString(R.string.alarm_set_to_past));
				return;
			} 
			final AlarmManager alarm_mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			final Intent intent = new Intent(this, ScheduledDownloadReceiver.class);
			final PendingIntent pending_intent = PendingIntent.getBroadcast(this, 0, intent, 0);
			alarm_mgr.set(AlarmManager.RTC_WAKEUP, updateTime, pending_intent);
			PonyLogger.d(TAG, "Downloads scheduled for: " + Long.toString(updateTime));
		}else{
			PonyLogger.d(TAG, "No background downloads scheduled");
		}				
		
	}

	private boolean checkBackgroundUpdate() {
		SharedPreferences prefs = getPreferences();
		return prefs.getBoolean(getString(R.string.schedule_download_key), true);	
	}

	/**
	 * This method returns the next time an scheduled download should happen based
	 * on the preferences in the form of a long in milliseconds. If downloads are not
	 * scheduled 0 is returned.
	 * @return
	 */
	private long getNextUpdateTime() {
		if (checkBackgroundUpdate()){
			Calendar cal = new GregorianCalendar();
			SharedPreferences prefs = getPreferences();
			long updateTime = prefs.getLong(getString(R.string.schedule_download_time_key), cal.getTimeInMillis());
			cal.setTimeInMillis(updateTime); 
			//the correct time is now set, but the date is when the preference was set.
			final Calendar now = Calendar.getInstance();
			cal.set(Calendar.DATE, now.get(Calendar.DATE));
			cal.set(Calendar.MONTH, now.get(Calendar.MONTH));
			cal.set(Calendar.YEAR, now.get(Calendar.YEAR));
			//Check if we want tomorrow
			if (System.currentTimeMillis() > cal.getTimeInMillis() - 60000){
				//-60000 so that a call to getNextUpdateTime from setNextAlarm after an
				//alarm doesn't set an alarm for 'now' triggering an new alarm.
				cal.add(Calendar.DAY_OF_YEAR,1);
			}
			return cal.getTimeInMillis();
		} else return 0; 
	}
	
	private SharedPreferences getPreferences(){
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}
	
	private ArrayList<DownloadingEpisode> getEpisodesToDownload(){
		final ArrayList<String> podcasts = (ArrayList<String>) 
				mPonyExpressApp.getDbHelper().listAllPodcasts();
		List<DownloadingEpisode> downloads = new ArrayList<DownloadingEpisode>();
		for (String podcast: podcasts){
			Cursor c = mPonyExpressApp.getDbHelper().getAllUndownloadedAndUnlistened(podcast);
			if (c != null && c.getCount() > 0){
				c.moveToFirst();
				for (int i = 0; i < c.getCount(); i++){
					DownloadingEpisode episode = new DownloadingEpisode();
					episode.setPodcastName(podcast);
					episode.setRowID(c.getLong(0));
					downloads.add(episode);
					c.moveToNext();
				}
			}
			c.close();
		}
		return (ArrayList<DownloadingEpisode>) downloads;
	}
	
	protected void NotifyError(String error_message) {
		if (error_message == ""){
			return;
		}
		//Send a notification to the user telling them of the error
		//This uses an empty intent because there is no new activity to start.
		PendingIntent intent = PendingIntent.getActivity(mPonyExpressApp.getApplicationContext(), 
				0, new Intent(), 0);
		NotificationManager notifyManager = 
			(NotificationManager) mPonyExpressApp.getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.stat_notify_error;

		Notification notification = new Notification(
				icon, null,
				System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(mPonyExpressApp.getApplicationContext(), 
				mPonyExpressApp.getText(R.string.app_name), error_message, intent);
		notifyManager.notify(NOTIFY_ID,notification);
		
	}
	
	//This is all responsible for connecting/disconnecting to the Downloader service.
	private ServiceConnection mDownloaderConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mDownloader = null;

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to an explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mDownloader = ((DownloaderService.DownloaderServiceBinder)service).getService();
			
		}
	};

	protected void doBindDownloaderService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(this, 
				DownloaderService.class), mDownloaderConnection, Context.BIND_AUTO_CREATE);
		mDownloaderBound = true;
	}


	protected void doUnbindDownloaderService() {
		if (mDownloaderBound) {
			// Detach our existing connection.
			//Must use getApplicationContext.unbindService() as 
			//getApplicationContext().bindService was used to bind initially.
			unbindService(mDownloaderConnection);
			mDownloaderBound = false;
			mDownloader = null;
		}
	}

}
