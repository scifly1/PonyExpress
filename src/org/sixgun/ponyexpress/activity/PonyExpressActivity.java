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
package org.sixgun.ponyexpress.activity;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.util.EpisodeFeedParser;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

/**
 * Launch Activity for PonyExpress.
 */
public class PonyExpressActivity extends ListActivity {

	private static final String UPDATE_IN_PROGRESS = "ponyexpress.update.inprogress";
	private static final String TAG = "PonyExpressActivity";
	private PonyExpressApp mPonyExpressApp; 
	private UpdateEpisodes mUpdateTask;
	@SuppressWarnings("unused")
	private DatabaseCheck mDataCheck;  
	private Bundle mSavedState;
	private ProgressDialog mProgDialog;
	private ProgressDialog mProgDialogDb;
	private int mEpisodesToHold = 10;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//Get the application context.
		mPonyExpressApp = (PonyExpressApp)getApplication();
		
		//Create Progress Dialogs for later use.
		mProgDialogDb = new ProgressDialog(this);
		mProgDialogDb.setMessage("Checking database consistency...");
		mProgDialog = new ProgressDialog(this);
		mProgDialog.setMessage("Checking for new Episodes. Please wait...");
		
		//Check SDCard contents and database match.
		//FIXME This does not handle activity lifecycle changes, but it's fairly quick
		//so should not be a problem.
		mDataCheck = (DatabaseCheck) new DatabaseCheck().execute();
		
		//Update the Episodes list if the database has been upgraded.
		if (mPonyExpressApp.getDbHelper().mDatabaseUpgraded){
			mUpdateTask = (UpdateEpisodes) new UpdateEpisodes().execute();
			if (mUpdateTask.isCancelled()){
				Log.d(TAG, "Cancelled Update, No Connectivity");
				mUpdateTask = null;
			}
			mPonyExpressApp.getDbHelper().mDatabaseUpgraded = false;
		}
		
		//Hook up reload button with updateEpisodes()
		ImageView reload_button =  (ImageButton)findViewById(R.id.view_refresh);	
		reload_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mUpdateTask = (UpdateEpisodes) new UpdateEpisodes().execute();
				if (mUpdateTask.isCancelled()){
					Log.d(TAG, "Cancelled Update, No Connectivity");
					mUpdateTask = null;
				}
			}
		});
	}

	/** 
	 * (Re-)list the episodes.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		listEpisodes();
		if (mSavedState != null){
			restoreLocalState(mSavedState);
		}
	}

	/**
	 *  Cancel progress dialog and close any running updates when activity destroyed.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Dismiss dialog now or it will leak.
		if (mProgDialog.isShowing()){
			mProgDialog.dismiss();
		}
		onUpdateEpisodesClose();
	}
	
	/** Restores the state of the Activity including any previously running Updates.
	 */
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		restoreLocalState(state);
		mSavedState = null;
	}

	private void restoreLocalState(Bundle state) {
		if (state.getBoolean(UPDATE_IN_PROGRESS)){
			mUpdateTask = (UpdateEpisodes) new UpdateEpisodes().execute();
		}
	}

	/** If Episodes are being updated then save this status, so it can be restarted.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveUpdateInProgress(outState);
		mSavedState = outState;
	}

	private void saveUpdateInProgress(Bundle outState) {
		final UpdateEpisodes task = mUpdateTask;
		if (task != null && task.getStatus() != Status.FINISHED){
			task.cancel(true);
			outState.putBoolean(UPDATE_IN_PROGRESS, true);
		} else {
			outState.putBoolean(UPDATE_IN_PROGRESS, false);
		}
		mUpdateTask = null;
		
	}

	/**
	 * Cancels a running update task (if any).  Called when the activity is destroyed.
	 */
	private void onUpdateEpisodesClose() {
		if (mUpdateTask != null && mUpdateTask.getStatus() == Status.RUNNING){
			mUpdateTask.cancel(true);
			mUpdateTask = null;
		}
	}


	/* (non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		//Get all info from database and put it in an Intent for EpisodeTabs
		final String title = mPonyExpressApp.getDbHelper().getEpisodeTitle(id);
		final String description = mPonyExpressApp.getDbHelper().getDescription(id);
		//Seperate episode number from filename for hashtag.
		Pattern digits = Pattern.compile("[0-9]+");
		Matcher m = digits.matcher(title);
		m.find();
		String epNumber = m.group(); 
		Log.d(TAG, "Episode number: " + epNumber);

		Intent intent = new Intent(this,EpisodeTabs.class);
		intent.putExtra(EpisodeKeys.TITLE, title);
		intent.putExtra(EpisodeKeys.DESCRIPTION, description);
		intent.putExtra(EpisodeKeys.EP_NUMBER, epNumber);
		intent.putExtra(EpisodeKeys._ID, id);
		//Determine if Episode has been downloaded and add required extras.
		final boolean downloaded = mPonyExpressApp.getDbHelper().isEpisodeDownloaded(id);
		if (downloaded){
			final String filename = mPonyExpressApp.getDbHelper().getEpisodeFilename(id);
			intent.putExtra(EpisodeKeys.FILENAME, filename);
			final int listened = mPonyExpressApp.getDbHelper().getListened(id);
			intent.putExtra(EpisodeKeys.LISTENED, listened);
		} else {
			final String url = mPonyExpressApp.getDbHelper().getEpisodeUrl(id);
			intent.putExtra(EpisodeKeys.URL, url);
			final int size = mPonyExpressApp.getDbHelper().getEpisodeSize(id);
			intent.putExtra(EpisodeKeys.SIZE, size);
		}
		startActivity(intent);
	}

	/**
	 * Parse the RSS feed and update the database with new episodes in a background thread.
	 * 
	 */
	private class UpdateEpisodes extends AsyncTask <Void,Void,Void>{
		
		/*
		 * This is carried out in the UI thread before the background tasks are started.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
				mProgDialog.show();
			} else {
				Toast.makeText(mPonyExpressApp, 
						R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
				cancel(true);
			}
		}
		/*
		 * This is done in a new thread,
		 */
		@Override
		protected Void doInBackground(Void... params) {
			
			final String feed = "http://feeds.feedburner.com/linuxoutlaws-ogg";

			EpisodeFeedParser parser = new EpisodeFeedParser(mPonyExpressApp,feed);

			List<Episode> episodes = parser.parse();

			final Date date = mPonyExpressApp.getDbHelper().getLatestEpisodeDate();
						
			for (Episode episode: episodes){
				//Add any new episodes
				if (episode.getDate().compareTo(date) > 0) {
					mPonyExpressApp.getDbHelper().insertEpisode(episode);
				}		
				
				//Determine how many episodes to remove
				final int rows = mPonyExpressApp.getDbHelper().getNumberOfRows();
				final int episodesToDelete = rows - mEpisodesToHold;
				//Remove correct number of episodes from oldest episodes to maintain required number.
				for (int i = episodesToDelete; i > 0; i--){
					final long rowID = 
						mPonyExpressApp.getDbHelper().getOldestEpisode();
					if (rowID != -1){
						if (mPonyExpressApp.getDbHelper().isEpisodeDownloaded(rowID)){
							//delete from SD Card
							deleteFile(rowID);
						}
						//remove from database after deleting.
						mPonyExpressApp.getDbHelper().deleteEpisode(rowID);
					} else {Log.e(TAG, "Cannot find oldest episode");}
				}
			}
			return null;
		}
		/** Deletes a file from the SD Card.
		 * 
		 * @param rowID of the file to be deleted from the database.
		 */
		private void deleteFile(long rowID) {
			File rootPath = Environment.getExternalStorageDirectory();
			File dirPath = new File(rootPath,PonyExpressApp.PODCAST_PATH);
			String filename = mPonyExpressApp.getDbHelper().getEpisodeFilename(rowID);
			File fullPath = new File(dirPath,filename);
			fullPath.delete();			
		}
		/* 
		 */
		@Override
		protected void onCancelled() {
			super.onCancelled();
		}
		/* 
		 */
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mProgDialog.hide();
			listEpisodes();			
		}
		
	};

	/**
	 * Query the database for all Episode titles to populate the ListView.
	 */
	private void listEpisodes(){
		Cursor c = mPonyExpressApp.getDbHelper().getAllEpisodeNames();
		startManagingCursor(c);
		//Set up columns to map from, and layout to map to
		String[] from = new String[] { EpisodeKeys.TITLE };
		int[] to = new int[] { R.id.episode_text };
		
		SimpleCursorAdapter episodes = new SimpleCursorAdapter(
				this, R.layout.episode_row, c, from, to);
		setListAdapter(episodes);
		
	}
	
	private class DatabaseCheck extends AsyncTask<Void, Void, Void> {
		

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgDialogDb.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			//Get a List of the file names that are on the SD Card.
			File rootPath = Environment.getExternalStorageDirectory();
			File path = new File(rootPath,PonyExpressApp.PODCAST_PATH);
			final String[] filesOnDisk = path.list();
			//Get a Map of filenames (and their index) that are in the database
			final Map<Long, String> filesInDatabase = 
				mPonyExpressApp.getDbHelper().getFilenamesOnDisk();
			if (filesOnDisk != null){
				List<String> diskFiles = Arrays.asList(filesOnDisk);
				//If file is in database but not on disk, mark as not downloaded in database.
				final int mapSize = filesInDatabase.size();
				Iterator<Entry<Long, String>> fileIter = filesInDatabase.entrySet().iterator();
				for (int i = 0; i < mapSize; i++){
					Map.Entry<Long, String> entry = (Map.Entry<Long, String>)fileIter.next();
					if (!diskFiles.contains(entry.getValue())){
						mPonyExpressApp.getDbHelper().update(entry.getKey(), 
								EpisodeKeys.DOWNLOADED, "false");
					}
				}
			}
			return null;
		}
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mProgDialogDb.dismiss();
		}
	}
	
}
