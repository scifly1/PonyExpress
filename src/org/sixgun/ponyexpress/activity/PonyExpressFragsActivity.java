/*
 * Copyright 2013 Paul Elms
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
package org.sixgun.ponyexpress.activity;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.fragment.PonyExpressFragment;
import org.sixgun.ponyexpress.fragment.ProgressDialogFragment;
import org.sixgun.ponyexpress.service.ScheduledDownloadService;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class PonyExpressFragsActivity extends FragmentActivity {

	private static final String TAG = "PonyExpressActivity";
	public static final String FIRST = "first";
	private static final String LAST_CACHE_CLEAR = "last_cache_clear";
	private PonyExpressApp mPonyExpressApp;
	private PonyExpressFragment mPonyFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main_frags);
		
		mPonyExpressApp = (PonyExpressApp)getApplication();
		FragmentManager fm = getSupportFragmentManager();
		mPonyFragment = (PonyExpressFragment) fm.findFragmentById(R.id.ponyexpress_fragment);
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//Is this the first run?
		final boolean first = prefs.getBoolean(FIRST, true);
		if (first){
			onFirstRun(prefs);
		}else{
			//Make sure the update alarm and scheduled downloads are set properly.
			mPonyFragment.updateFeed(PonyExpressFragment.SET_ALARM_ONLY);

			if (!mPonyExpressApp.isScheduledDownloadServiceRunning()){
				Intent intent = new Intent(mPonyExpressApp, ScheduledDownloadService.class);
				intent.putExtra(PonyExpressActivity.SET_ALARM_ONLY, true);
				mPonyExpressApp.startService(intent);
			}
		}
		//Clear the Images on disk if it has not been done for a month
		maintainImageCache(prefs);

		//Check SDCard contents and database match.
		new DatabaseCheck().execute();
		
		//If started by clicking a podcast link call AddPodcast in Pony fragment
		Intent intent = getIntent();
		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)){
			final String url = intent.getDataString();
			mPonyFragment.addPodcast(null, url);
		}	
	}
	//Update the feeds when new podcast(s) have been added.
	/* (non-Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		PonyLogger.d(TAG, "New Intent recieved");
		final String podcast_name = intent.getExtras().
				getString(PodcastKeys.NAME);
		mPonyFragment.updateFeed(podcast_name);
	}

	// This method shows a dialog, and calls updateFeed() if this is the first time Pony 
	// has been run.  The SharedPrefrences then get changed to false.
	private void onFirstRun(SharedPreferences prefs) {
		mPonyFragment.updateFeed(PonyExpressFragment.UPDATE_SIXGUN_SHOW_LIST);
		//Sets the preference to false so this doesn't get called again.
		final SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(FIRST, false);
		editor.commit();
	}
	/**
	 * Clears the images on disk every 30 days so that it does not waste resources.
	 * @param prefs
	 */
	private void maintainImageCache(SharedPreferences prefs) {
		final long now = System.currentTimeMillis();
		final long last_cache_clear = prefs.getLong(LAST_CACHE_CLEAR, now);
		if (last_cache_clear == now){
			//prefs not set yet, set it now
			prefs.edit().putLong(LAST_CACHE_CLEAR, now).commit();
		}
		final long delta = 1000l*60l*60l*24l*30l; //30 days 
		final long next = last_cache_clear + delta;
		if (next < now && 
				mPonyExpressApp.getInternetHelper().isDownloadAllowed() ){
			PonyExpressApp.sBitmapManager.clear();
			prefs.edit().putLong(LAST_CACHE_CLEAR, now).commit();
			PonyLogger.d(TAG, "Clearing image cache");
		}else{
			PonyLogger.d(TAG, "Not clearing cache");
		}

	}

	private class DatabaseCheck extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			//Get a List of the file names that are on the SD Card for each podcast.
			File rootPath = Environment.getExternalStorageDirectory();
			File path = new File(rootPath,PonyExpressApp.PODCAST_PATH);
			final String[] podcastsOnDisk = path.list();
			List<String> filesOnDisk = new ArrayList<String>();
			if (podcastsOnDisk != null){
				for (String podcast: podcastsOnDisk){
					//Check if podcast is really the backup file.
					if (podcast.contains("opml")){
						continue; 
					}
					File podcast_path = new File(path,podcast);
					String[] files = podcast_path.list();
					try{
						filesOnDisk.addAll(Arrays.asList(files));
					}catch(NullPointerException e){
						PonyLogger.w(TAG,"NullPOinter");
					}
				}
				//Get a Map of filenames (and their index) that are in the database
				final Map<Long, String> filesInDatabase = 
						mPonyExpressApp.getDbHelper().getFilenamesOnDisk();
				if (filesOnDisk != null){
					//If file is in database but not on disk, mark as not downloaded in database.
					final int mapSize = filesInDatabase.size();
					Iterator<Entry<Long, String>> fileIter = filesInDatabase.entrySet().iterator();
					for (int i = 0; i < mapSize; i++){
						Map.Entry<Long, String> entry = (Map.Entry<Long, String>)fileIter.next();
						if (!filesOnDisk.contains(entry.getValue())){
							mPonyExpressApp.getDbHelper().update(entry.getKey(), 
									EpisodeKeys.DOWNLOADED, "false");
						}
					}
				}
			}
			return null;
		}
	}
}
