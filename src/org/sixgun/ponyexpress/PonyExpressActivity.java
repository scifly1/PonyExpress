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

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Launch Activity for PonyExpress.
 */
public class PonyExpressActivity extends ListActivity {

	private static final String UPDATE_IN_PROGRESS = "ponyexpress.update.inprogress";
	private static final int DOWNLOAD_ID = Menu.FIRST;
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
		ImageView reload_button =  (ImageView)findViewById(R.id.view_refresh);
		//Create Progress Dialog for use later.
		mProgDialog = new ProgressDialog(this);
		reload_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mUpdateTask = (UpdateEpisodes) new UpdateEpisodes().execute();
			}
		});
		registerForContextMenu(getListView());
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
			
			mUpdateTask = null;
		}
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
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case DOWNLOAD_ID:
			String url = mPonyExpressApp.getDbHelper().getEpisodeUrl(info.id);
			Intent i = new Intent(this,Downloader.class);
			i.putExtra(EpisodeKeys.URL, url);
			startService(i);
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Creates the Context menu (shown on a long click of a menu item).
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0,DOWNLOAD_ID,0,R.string.download);
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

			SaxFeedParser parser = new SaxFeedParser(mPonyExpressApp,feed);

			List<Episode> Episodes = parser.parse();

			final Date date = mPonyExpressApp.getDbHelper().getLatestEpisodeDate();

			for (Episode episode: Episodes){
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
