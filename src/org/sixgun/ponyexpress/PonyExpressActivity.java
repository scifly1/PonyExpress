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

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
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
	private Bundle mSavedState;
	private ProgressDialog mProgDialog; 


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//Get the application context.
		mPonyExpressApp = (PonyExpressApp) this.getApplication();
		
		//Hook up reload button with updateEpisodes()
		ImageView reload_button =  (ImageButton)findViewById(R.id.view_refresh);
		//Create Progress Dialog for use later.
		mProgDialog = new ProgressDialog(this);
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
	 *  Close the database and cancel any update tasks when activity destroyed.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
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
		
		final String title = mPonyExpressApp.getDbHelper().getEpisodeTitle(id);
		final String description = mPonyExpressApp.getDbHelper().getDescription(id);
		//Seperate episode number from filename for hashtag.
		Pattern digits = Pattern.compile("[0-9]+");
		Matcher m = digits.matcher(title);
		m.find();
		String epNumber = m.group(); 
		Log.d(TAG, "Episode number: " + epNumber);

		//Determine if Episode has been downloaded and create required intents.
		final boolean downloaded = mPonyExpressApp.getDbHelper().getEpisodeDownloaded(id);
		if (downloaded){
			final String filename = mPonyExpressApp.getDbHelper().getEpisodeFilename(id);
			Intent listenIntent = new Intent(this,PlayerActivity.class);
			listenIntent.putExtra(EpisodeKeys.FILENAME, filename);
			listenIntent.putExtra(EpisodeKeys.TITLE, title);
			listenIntent.putExtra(EpisodeKeys.DESCRIPTION, description);
			listenIntent.putExtra(EpisodeKeys.EP_NUMBER, epNumber);
			startActivity(listenIntent);
		} else {
			final String url = mPonyExpressApp.getDbHelper().getEpisodeUrl(id);
			Intent downloadIntent = new Intent(this,DownloadActivity.class);
			downloadIntent.putExtra(EpisodeKeys.TITLE, title);
			downloadIntent.putExtra(EpisodeKeys.DESCRIPTION, description);
			downloadIntent.putExtra(EpisodeKeys.URL, url);
			downloadIntent.putExtra(EpisodeKeys._ID, id);
			downloadIntent.putExtra(EpisodeKeys.EP_NUMBER,epNumber);
			startActivity(downloadIntent);
		}
		
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
				mProgDialog.setMessage("Checking for new Episodes. Please wait...");
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
				if (episode.getDate().compareTo(date) > 0) {
					mPonyExpressApp.getDbHelper().insertEpisode(episode);
				}	
			}
			return null;
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
			mProgDialog.dismiss();
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
	
}
