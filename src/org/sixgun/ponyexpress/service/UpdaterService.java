package org.sixgun.ponyexpress.service;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.Podcast;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.PonyExpressActivity;
import org.sixgun.ponyexpress.util.EpisodeFeedParser;
import org.sixgun.ponyexpress.util.PodcastFeedParser;
import org.sixgun.ponyexpress.util.SixgunPodcastsParser;
import org.sixgun.ponyexpress.util.Utils;

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

	@Override
	protected void onHandleIntent(Intent intent) {
		mPonyExpressApp = (PonyExpressApp)getApplication();
		Log.d(TAG,"Updater Service started");
		
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		showStatusNotification();
				
		Bundle data = intent.getExtras();
		final boolean updateAll = data.getBoolean(PonyExpressActivity.UPDATE_ALL);
		final boolean updateSixgun = data.getBoolean(PonyExpressActivity.UPDATE_SIXGUN_SHOW_LIST);
		
		checkFirstRun();
		
		if (updateSixgun){
			checkForNewSixgunShows();
			updateAllFeeds();
		}
		
		if (updateAll){
			updateAllFeeds();
		}else{
			//TODO Single update...
		}
		
		mNM.cancel(NOTIFY_1);
		Log.d(TAG,"Updater Service stopped");
	}
	
	public void checkForNewSixgunShows() {
		Log.d(TAG,"Checking for new Sixgun podcasts");
		//Get server list of sixgun podcasts and create list of urls
		final Context ctx = mPonyExpressApp.getApplicationContext();
		SixgunPodcastsParser parser = 
			new SixgunPodcastsParser(ctx, getString(R.string.sixgun_feeds));
		ArrayList<Podcast> sixgun_podcasts =(ArrayList<Podcast>) parser.parse();
		//Sixgun.org cannot be contacted
		if (sixgun_podcasts.isEmpty()){
			Log.d(TAG,"Cannot parse sixgun list, loading default podcast.");
			String[] default_feed = ctx.getResources().getStringArray(R.array.default_lo_feed);
			PodcastFeedParser default_parser = new PodcastFeedParser(ctx, default_feed[0]);
			Podcast default_podcast = default_parser.parse();
			if (default_podcast != null){
				default_podcast.setIdenticaTag(default_feed[1]);
				default_podcast.setIdenticaGroup(default_feed[2]);
				boolean checkdb = mPonyExpressApp.getDbHelper().checkDatabaseForUrl(default_podcast);
				if (checkdb == false) {
					//Add any new podcasts to the podcasts table
					Log.d(TAG, "Adding new Podcasts!");
					mPonyExpressApp.getDbHelper().addNewPodcast(default_podcast);
				}	
			}
		}
		//Check if any podcast is already in the Database
		for (Podcast podcast:sixgun_podcasts) {
			boolean checkdb = mPonyExpressApp.getDbHelper().checkDatabaseForUrl(podcast);
			if (checkdb == false) {
				//Add any new podcasts to the podcasts table
				Log.d(TAG, "Adding new Podcasts!");
				mPonyExpressApp.getDbHelper().addNewPodcast(podcast);		
			}
		}
	}
	
	private void updateAllFeeds(){
		Log.d(TAG,"Updating all episodes");
		List<String> podcast_names = mPonyExpressApp.getDbHelper().listAllPodcasts();
		//TODO Hanlde return errors
		for (String podcast: podcast_names){
			updateFeed(podcast);
		}
	}
	
	private void updateFeed(String podcast_name){
		Log.d(TAG, "Updating " + podcast_name);
		String podcast_url = mPonyExpressApp.getDbHelper().getPodcastUrl(podcast_name);
		
		if (!mPonyExpressApp.getInternetHelper().checkConnectivity()){
			//TODO Handle Errors
			showErrorNotification(getText(R.string.interent_lost));
		}
		
		if (!pingUrl(podcast_url)) {
			//TODO add text parm...
			showErrorNotification("TODO");
			return;
		}
		
		checkForNewArt(podcast_url);
		EpisodeFeedParser parser = new EpisodeFeedParser(mPonyExpressApp,podcast_url);
		List<Episode> episodes = parser.parse();
		
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
		removeExtraEpisodes(podcast_name);
		
		//TODO Remove after testing is done
		//showErrorNotification(getText(R.string.interent_lost));
		showErrorNotification(podcast_url + getText(R.string.url_offline));
		//TODO maybe more work needs done??? double check
	}
	
	private boolean pingUrl(String podcast_url) {
		try {
            URL url = new URL(podcast_url);
            HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
            urlconn.setRequestProperty("Connection", "close");
            urlconn.setConnectTimeout(1000 * 20); // mTimeout is in seconds
            urlconn.connect();
            if (urlconn.getResponseCode() == 200) {
            	Log.e(TAG,"getResponseCode == 200");
            	return true;
            }
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
		showErrorNotification(podcast_url + getText(R.string.url_offline));
		return false;
	}

	private SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}
	
	private int getEpisodesToHold(){
		SharedPreferences prefs = getPreferences();
		return Integer.parseInt(prefs.getString(getString(R.string.eps_stored_key), "6"));
	}
	
	private void checkFirstRun() {
		//Checks if this is the first run and adds the Sixgun.org shows if it is.
		SharedPreferences prefs = getPreferences();
		final boolean first = prefs.getBoolean("first", true);
			if (first == true){
				//Log that this is the first run
				Log.d(TAG,"First run!");
				//Calls the method that checks for Sixgun.org shows
				checkForNewSixgunShows();
				//Sets the preference to false so this doesn't get called again.
				final SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean("first", false);
				editor.commit();
			}
	}
	
	//Called from updateFeed()
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
	
	private void removeExtraEpisodes(String podcast_name){
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
				}
				//remove from database after deleting.
				mPonyExpressApp.getDbHelper().deleteEpisode(rowID, podcast_name);
			} else {Log.e(TAG, "Cannot find oldest episode");}
		}
	}

	/**
     * Show this notification while this service is running.
     */
    private void showStatusNotification() {
    	// TODO Set the proper text for the notify
    	CharSequence text = "//TODO";

    	// Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.pony_icon, text,
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        
        // TODO Set this to the proper value so The user can click on the notification and
        // get a detailed look.
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
        // get a detailed look.
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

	