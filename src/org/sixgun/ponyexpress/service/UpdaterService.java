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

package org.sixgun.ponyexpress.service;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.ReturnCodes;
import org.sixgun.ponyexpress.Podcast;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.PonyExpressActivity;
import org.sixgun.ponyexpress.receiver.UpdateAlarmReceiver;
import org.sixgun.ponyexpress.util.EpisodeFeedParser;
import org.sixgun.ponyexpress.util.PodcastFeedParser;
import org.sixgun.ponyexpress.util.SixgunPodcastsParser;
import org.sixgun.ponyexpress.util.Utils;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class UpdaterService extends IntentService {
	
	private String TAG = "PonyExpress UpdaterService";
	private PonyExpressApp mPonyExpressApp;
	private NotificationManager mNM;
	private static final int NOTIFY_1 = 1;
	private static final int NOTIFY_2 = 2;
	
		
	

	public UpdaterService() {
		super("UpdaterService");
	}

	/**
	 * This method is called first when UpdaterService is started. It parses an input intent
	 * and calls the other methods as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		// Get the application, and Log that this service has started.
		mPonyExpressApp = (PonyExpressApp)getApplication();
		Log.d(TAG,"Updater Service started");
		
		// Initialize the status notification
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
						
		// Get the input data from the intent and parse it, starting the various
		// updater methods and setting notification text as needed.
		Bundle data = intent.getExtras();
		final boolean update_sixgun = data.getBoolean(PonyExpressActivity.UPDATE_SIXGUN_SHOW_LIST);	
		final boolean update_all = data.getBoolean(PonyExpressActivity.UPDATE_ALL);
		final boolean set_alarm_only = data.getBoolean(PonyExpressActivity.SET_ALARM_ONLY);
		final String update_single = data.getString(PonyExpressActivity.UPDATE_SINGLE);
				
		if (set_alarm_only){
			final long nextUpdate = getNextUpdateTime();
			if (nextUpdate >= System.currentTimeMillis()){
				setNextAlarm();
			}else{
				showStatusNotification(getText(R.string.checking_all));
				updateAllFeeds();
			}
		}
		
		if (update_sixgun){
			showStatusNotification(getText(R.string.checking_sixgun));
			checkForNewSixgunShows();
			updateAllFeeds();
		}
		
		if (update_all) {
			showStatusNotification(getText(R.string.checking_all));
			updateAllFeeds();
		}
		
		if (!set_alarm_only && !update_sixgun && !update_all && update_single != null) {
			showStatusNotification(getText(R.string.checking) + " " + update_single);
			updateFeed(data.getString(PonyExpressActivity.UPDATE_SINGLE));
		}
		
		// This method is done at this point, so cancel the notification and log. 
		mNM.cancel(NOTIFY_1);
		Log.d(TAG,"Updater Service stopped");
	}
	
	/**
	 * This method return a boolean true if the SharedPreferences contain a number.  Otherwise, it returns false.
	 * @return
	 */
	private boolean checkBackgroundUpdate() {
		SharedPreferences prefs = getPreferences();
		try{
			Long.parseLong(prefs.getString(getString(R.string.update_freqs_key), "24"));
		}catch (NumberFormatException e){
			return false;
		}
		return true;
	}

	/**
	 * This method sets the alarm manger based on the last update and the
	 * preferences.
	 */
	private void setNextAlarm() {
		if(checkBackgroundUpdate()){
			final Long updateTime = getNextUpdateTime();
			final AlarmManager alarm_mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			final Intent intent = new Intent(this, UpdateAlarmReceiver.class);
			final PendingIntent pending_intent = PendingIntent.getBroadcast(this, 0, intent, 0);
			alarm_mgr.set(AlarmManager.RTC_WAKEUP, updateTime, pending_intent);
			Log.d(TAG, "Update scheduled for: " + Long.toString(updateTime));
		}else{
			Log.d(TAG, "No background updates scheduled");
		}				
	}

	/**
	 * This method returns the next time an automatic update should happen based
	 * on the last update and the preferences in the form of a long in milliseconds. 
	 * @return
	 */
	private long getNextUpdateTime() {
		SharedPreferences prefs = getPreferences();
		final Long lastUpdate = prefs.getLong(PonyExpressActivity.LASTUPDATE, System.currentTimeMillis());
		final Long updateDelta =  3600000 * Long.parseLong
				(prefs.getString(getString(R.string.update_freqs_key), "24"));
		return lastUpdate + updateDelta;
		
	}

	/**
	 * This method sets a preference for the last time all episodes where updated.  It sets the 
	 * current time if this is the first run
	 */
	private void setLastUpdateTime() {
		SharedPreferences prefs = getPreferences();
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(PonyExpressActivity.LASTUPDATE, System.currentTimeMillis());
		editor.commit();
	}

	/**
	 * This method checks Sixgun.org for new shows, and adds anything new to the database.
	 * It will also call loadDefaultShow() if sixgun.org is offline.
	 */
	
	public void checkForNewSixgunShows() {
		Log.d(TAG,"Checking for new Sixgun podcasts");
		//Get server list of sixgun podcasts and create list of urls
		final Context ctx = mPonyExpressApp.getApplicationContext();
		SixgunPodcastsParser parser = 
			new SixgunPodcastsParser(ctx, getString(R.string.sixgun_feeds));
		ArrayList<Podcast> sixgun_podcasts =(ArrayList<Podcast>) parser.parse();
		//Sixgun.org cannot be contacted
		if (sixgun_podcasts.isEmpty()){
			//Sixgun.org is offline so load the default.
			loadDefaultShow();
			
		}else{
			//sixgun.org is online so load the sixgun.org shows
			for (Podcast podcast:sixgun_podcasts) {
				addSixgunShow(podcast);
			}
		}
	}
	
	/**
	 * This method adds a Sixgun podcast to the database, if it is not already in the database.
	 * @param podcast
	 */
	private void addSixgunShow(Podcast podcast) {
		boolean checkdb = mPonyExpressApp.getDbHelper().checkDatabaseForUrl(podcast);
		if (checkdb == false) {
			//Add any new podcasts to the podcasts table
			Log.d(TAG, "Adding new Podcasts!");
			mPonyExpressApp.getDbHelper().addNewPodcast(podcast);		
		}
	}

	/**
	 * This method loads a default Linux Outlaws feed that is stored in strings.xml.
	 * It should only be called if sixgun.org is offline during a first run.
	 */
	private void loadDefaultShow() {
		Log.d(TAG,"Cannot parse sixgun list, loading default podcast.");
		final Context ctx = mPonyExpressApp.getApplicationContext();
		String[] default_feed = ctx.getResources().getStringArray(R.array.default_lo_feed);
		PodcastFeedParser default_parser = new PodcastFeedParser(ctx, default_feed[0]);
		Podcast default_podcast = default_parser.parse();
		if (default_podcast != null){
			default_podcast.setIdenticaTag(default_feed[1]);
			default_podcast.setIdenticaGroup(default_feed[2]);
			addSixgunShow(default_podcast);
			}	
		}

	/**
	 * This method uses a for loop to send all the shows in the database to the updateFeed()
	 * method one by one.  It also checks a return code to see if internet connectivity 
	 * has been lost and breaks if need be.  Finally, it calls the methods that handle the update alarm.
	 */
	private void updateAllFeeds(){
		Log.d(TAG,"Updating all episodes");
		List<String> podcast_names = mPonyExpressApp.getDbHelper().listAllPodcasts();
		
		for (String podcast_name: podcast_names){
			int return_code = updateFeed(podcast_name);
			if (return_code == ReturnCodes.INTERNET_CONNECTIVITY_LOST){
				//This means the internet is unreachable so this loop needs to stop
				break;
			}
		}
		setLastUpdateTime();
		//Set the next update alarm
		setNextAlarm();
	}
	
	/**
	 * This method updates a feed.
	 * @param podcast_name
	 * @return
	 */
	private int updateFeed(String podcast_name){
		Log.d(TAG, "Updating " + podcast_name);
		
		String podcast_url = mPonyExpressApp.getDbHelper().getPodcastUrl(podcast_name);
		if (!mPonyExpressApp.getInternetHelper().checkConnectivity()){
			showErrorNotification(getText(R.string.interent_lost));
			Log.e(TAG,"Internet connectivity lost during update");
			return ReturnCodes.INTERNET_CONNECTIVITY_LOST;
		}
		
		if (pingUrl(podcast_url) == ReturnCodes.URL_OFFLINE) {
			showErrorNotification(podcast_url + " " + getText(R.string.url_offline));
			Log.e(TAG, podcast_url + " offline during update");
			return ReturnCodes.URL_OFFLINE;
		}
		
		checkForNewArt(podcast_url);
		
		EpisodeFeedParser parser = new EpisodeFeedParser(mPonyExpressApp,podcast_url);
		List<Episode> episodes = null;
		
		try{
			episodes = parser.parse();
		}catch (RuntimeException e){
			showErrorNotification(getString(R.string.parse_error));
			Log.e(TAG, podcast_url + " - RuntimeException during feed parse");
			return ReturnCodes.PARSING_ERROR;
		}
				
		addAndRemoveEpisodes(episodes, podcast_name);
						
		return ReturnCodes.ALL_OK;
	}
	
	/**
	 * This method adds and/or deletes episodes in the database as needed.
	 * @param episodes
	 * @param podcast_name
	 */
	private void addAndRemoveEpisodes(List<Episode> episodes, String podcast_name) {
		for (int i = 0; i < (getEpisodesToHold()) ; i++){
			//Add any episodes not already in database
			try{
				if (!mPonyExpressApp.getDbHelper().containsEpisode(episodes.get(i).getTitle(),podcast_name)) {
					mPonyExpressApp.getDbHelper().insertEpisode(episodes.get(i), podcast_name);
				}
			}catch(IndexOutOfBoundsException e){
				//The feed has fewer episodes than the number to keep so log and break
				Log.d(TAG, "Number of episodes in this feed is less than the number to keep");
				break;
			}
		}
		
		//Determine how many episodes to remove to maintain mEpisodesToHold
		final int rows = mPonyExpressApp.getDbHelper().getNumberOfRows(podcast_name);
		final int episodesToDelete = rows - getEpisodesToHold();
		//Remove correct number of episodes from oldest episodes to maintain required number.
		for (int i = episodesToDelete; i > 0; i--){
			final long rowID = 
					mPonyExpressApp.getDbHelper().getOldestEpisode(podcast_name);
			if (rowID != -1){
				if (mPonyExpressApp.getDbHelper().isEpisodeDownloaded(rowID, podcast_name)){
					//delete from SD Card
					Utils.deleteFile(mPonyExpressApp, rowID, podcast_name);
					//remove from playlist if in it.
					mPonyExpressApp.getDbHelper().removeEpisodeFromPlaylist(podcast_name, rowID);
				}
				//remove from database after deleting.
				mPonyExpressApp.getDbHelper().deleteEpisode(rowID, podcast_name);
			} else {Log.e(TAG, "Cannot find oldest episode");}
		}
	}

	/**
	 * This method pings a podcast url and returns the ALL_OK return code
	 * if the http response code is 200.
	 * @param podcast_url
	 * @return Return code int
	 */
	private int pingUrl(String podcast_url) {
		try {
            URL url = new URL(podcast_url);
            HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
            urlconn.setRequestProperty("Connection", "close");
            urlconn.setConnectTimeout(20000); // Timeout is 20 seconds
            urlconn.connect();
            if (urlconn.getResponseCode() == 200) {
            	return ReturnCodes.ALL_OK;
            } else {
            	showErrorNotification(podcast_url + getText(R.string.url_offline));
        		return ReturnCodes.URL_OFFLINE;
            }
		} catch (MalformedURLException e1) {
			showErrorNotification(podcast_url + getText(R.string.url_offline));
			return ReturnCodes.URL_OFFLINE;			
		} catch (IOException e) {
			showErrorNotification(podcast_url + getText(R.string.url_offline));
			return ReturnCodes.URL_OFFLINE;
		}		
	}

	private SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}
	
	private int getEpisodesToHold(){
		SharedPreferences prefs = getPreferences();
		return Integer.parseInt(prefs.getString(getString(R.string.eps_stored_key), "6"));
	}
	
	/**
	 * This method is used to check and update the album art.
	 * @param podcast_url
	 */
	private void checkForNewArt(String podcast_url){
		PodcastFeedParser parser = new PodcastFeedParser(mPonyExpressApp,podcast_url);
		String art_url;
		try
		{
			art_url = parser.parseAlbumArtURL();
		}
		catch (NullPointerException ex)
		{
			// No album art.  Just return.
			return;
		}
		mPonyExpressApp.getDbHelper().updateAlbumArtUrl(podcast_url, art_url);
	}
		
	/**
     * Show this notification while UpdaterService service is running.
	 * @param text 
     */
    private void showStatusNotification(CharSequence text) {
    	
    	// Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.pony_icon, text,
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        
        // TODO Set this to the proper value so The user can click on the notification and
        // get a detailed error report.
        PendingIntent intent = PendingIntent.getActivity(mPonyExpressApp, 
				0, new Intent(this, PonyExpressActivity.class), 0);

		notification.setLatestEventInfo(mPonyExpressApp, 
				getText(R.string.app_name), text, intent);
		
		// Send the notification.
        mNM.notify(NOTIFY_1, notification);
        
    }
    
    /**
     * Show this notification if an error occurred while updating.
     */
    private void showErrorNotification(CharSequence text) {
        
    	// Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.stat_notify_error, text,
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        
        // TODO Set this to the proper value so The user can click on the notification and
        // get a detailed error report.
		Intent intent = new Intent(this, PonyExpressActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pintent = PendingIntent.getActivity(mPonyExpressApp, 
				0, intent, PendingIntent.FLAG_ONE_SHOT);
		
		notification.setLatestEventInfo(mPonyExpressApp, 
				getText(R.string.app_name), text, pintent);
		
		// Send the notification.
        mNM.notify(NOTIFY_2, notification);
    }
}

	