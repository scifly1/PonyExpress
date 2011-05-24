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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.Podcast;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.util.EpisodeFeedParser;
import org.sixgun.ponyexpress.util.PodcastFeedParser;
import org.sixgun.ponyexpress.util.SixgunPodcastsParser;
import org.sixgun.ponyexpress.util.Utils;
import org.sixgun.ponyexpress.view.RemoteImageView;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.AsyncTask.Status;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Launch Activity for PonyExpress.
 */
public class PonyExpressActivity extends ListActivity {

	private static final String UPDATE_IN_PROGRESS = "ponyexpress.update.inprogress";
	private static final String PODCAST_BEING_UPDATED = "ponyexpress.podcast.being.updated";
	private static final String TAG = "PonyExpressActivity";
	private static final String UPDATEFILE = "Updatestatus";
	private static final String LASTUPDATE = "lastupdate";
	private static final int SETUP_ACCOUNT = 0;
	private static final int ABOUT_DIALOG = 4;
	private PonyExpressApp mPonyExpressApp; 
	private UpdateEpisodes mUpdateTask; 
	private String mPodcastBeingUpdated;
	private Bundle mSavedState;
	private ProgressDialog mProgDialog;
	private int mEpisodesToHold;
	private int mUpdateDelta;
	private GregorianCalendar mLastUpdate;
	private boolean mAdditionalPodcasts;
	private BroadcastReceiver mPodcastDeletedReceiver;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//Get the application context.
		mPonyExpressApp = (PonyExpressApp)getApplication();
		
		//Get the update delta and number of episodes to hold from preferences
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final String updateDelta = prefs.getString(getString(R.string.update_freqs_key), "24");
		final Resources res = getResources();
		//Check if no-refresh has been set
		if (updateDelta.equals(res.getStringArray(R.array.update_freqs)[0])){
			mUpdateDelta = 999;
		} else mUpdateDelta = Integer.parseInt(updateDelta);
		
		mEpisodesToHold = Integer.parseInt(prefs.getString(getString(R.string.eps_stored_key), "6"));
		Log.d(TAG,"Eps to hold: " + mEpisodesToHold);
		Log.d(TAG,"update delta: " + mUpdateDelta);
		
		//Check if additional podcasts are activated
		mAdditionalPodcasts = prefs.getBoolean(getString(R.string.add_podcasts), false);
		if (mAdditionalPodcasts){
			ImageButton addPodcastsButton = (ImageButton) findViewById(R.id.add_podcasts_button);
			addPodcastsButton.setVisibility(View.VISIBLE);
		}
		
		//Create Progress Dialogs for later use.
		mProgDialog = new ProgressDialog(this);
		mProgDialog.setMessage("Checking for new Episodes. Please wait...");
		
		//Check SDCard contents and database match.
		new DatabaseCheck().execute();
		
		//Update the Episodes list if the database has been upgraded.
		if (mPonyExpressApp.getDbHelper().mDatabaseUpgraded){
			updateFeeds();
			mPonyExpressApp.getDbHelper().mDatabaseUpgraded = false;
		}
		
		//If the user has set an autoupdate frequency (mUpdateDelta != 999), check the 
		//last time the database was updated and update if necessary
		if (mUpdateDelta != 999 && isTimeToUpdate()){
			//update delta has passed and we have connectivity so update
			updateFeeds();
		}
		
		mPodcastDeletedReceiver = new PodcastDeleted();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter("org.sixgun.ponyexpress.PODCAST_DELETED");
		registerReceiver(mPodcastDeletedReceiver, filter);
		listPodcasts();
		if (mSavedState != null){
			restoreLocalState(mSavedState);
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mPodcastDeletedReceiver);
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
			String podcast_name = state.getString(PODCAST_BEING_UPDATED);
			if (podcast_name == null){
				podcast_name = "";
			}
			mUpdateTask = (UpdateEpisodes) new UpdateEpisodes().execute(podcast_name);
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
			outState.putString(PODCAST_BEING_UPDATED, mPodcastBeingUpdated);
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
	

	/**
	 * Starts the PodcastTabs activity with the selected podcast
	 * @param id row_id of the podcast in the database
	 */
	private void selectPodcast(View v, long id) {
		//Get the podcast name and album art url and number of unlistened episodes.

		final String name = mPonyExpressApp.getDbHelper().getPodcastName(id);
		final String url = mPonyExpressApp.getDbHelper().getAlbumArtUrl(id);
		//Store in an intent and send to EpisodesActivity
		Intent intent = new Intent(this,EpisodesActivity.class);
		intent.putExtra(PodcastKeys.NAME, name);
		intent.putExtra(PodcastKeys.ALBUM_ART_URL, url);
		startActivity(intent);
	}
	
	/**
	 * Bring up the Settings (preferences) menu via a button click.
	 * @param v, a reference to the button that was clicked to call this.
	 */
	public void showSettings(View v){
		startActivity(new Intent(
        		mPonyExpressApp,PreferencesActivity.class));
	}
	
	/**
	 * Bring up the About dialog via a button click.
	 * @param v, a reference to the button that was clicked to call this.
	 */
	public void showAbout(View v){
		showDialog(ABOUT_DIALOG);
	}
	
	/**
	 * Bring up the add Podcasts activity.
	 * @param v
	 */
	public void addPodcast(View v) {
		startActivity(new Intent(
				mPonyExpressApp, AddNewPodcastFeedActivity.class));
	}
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int, android.os.Bundle)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id){
		case ABOUT_DIALOG:
			dialog = AboutDialog.create(this);
			break;
		default:
			dialog = null;
			break;
		}
		return dialog;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_options_menu, menu);
	    return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.update_feeds:
	        updateFeeds();
	        return true;
	    case R.id.settings_menu:
	    	startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
	        return true;
	    case R.id.identica_account_settings:
	    	//Fire off AccountSetup screen
			startActivityForResult(new Intent(
					mPonyExpressApp,IdenticaAccountSetupActivity.class),
					SETUP_ACCOUNT);
	    	return true;
	    case R.id.about:
	    	showDialog(ABOUT_DIALOG);
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
		}
	}

	private boolean isTimeToUpdate(){
		SharedPreferences updateStatus = getSharedPreferences(UPDATEFILE, 0);
		final long lastUpdateMillis = updateStatus.getLong(LASTUPDATE, 0);
		mLastUpdate = new GregorianCalendar(Locale.US);
		mLastUpdate.setTimeInMillis(lastUpdateMillis);
		//Add on the update delta and compare with now.
		mLastUpdate.add(Calendar.HOUR_OF_DAY, mUpdateDelta);
		final GregorianCalendar now = new GregorianCalendar(Locale.US);
		if (mLastUpdate.compareTo(now) < 0 && 
				(mPonyExpressApp.getInternetHelper().checkConnectivity())){
			return true;
		} else return false;
	}
	
	private void updateFeeds() {
		updateFeed("");
	}
	
	private void updateFeed(String podcastName){
		mUpdateTask = (UpdateEpisodes) new UpdateEpisodes().execute(podcastName);
		if (mUpdateTask.isCancelled()){
			Log.d(TAG, "Cancelled Update, No Connectivity");
			mUpdateTask = null;
		}
	}
	
	private void listPodcasts() {
		Cursor c = mPonyExpressApp.getDbHelper().getAllPodcastNamesAndArt();
		startManagingCursor(c);
		//Create a CursorAdapter to map podcast title and art to the ListView.
		PodcastCursorAdapter adapter = new PodcastCursorAdapter(mPonyExpressApp, c);
		setListAdapter(adapter);
		registerForContextMenu(getListView());
	}

	/**
	 * We subclass CursorAdapter to handle our display the results from our Podcast cursor. 
	 * Overide newView to create/inflate a view to bind the data to.
	 * Overide bindView to determine how the data is bound to the view.
	 */
	private class PodcastCursorAdapter extends CursorAdapter{

		public PodcastCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final int nameIndex = cursor.getColumnIndex(PodcastKeys.NAME);
			final int artUrlIndex = cursor.getColumnIndex(PodcastKeys.ALBUM_ART_URL);
			//get the number of unlistened episodes
			String name = cursor.getString(nameIndex);
			final String fullName = name;
			final int unlistened = mPonyExpressApp.getDbHelper().countUnlistened(name);
			
			//Remove the words "ogg feed" if present at the end.
			name = Utils.stripper(name, "Ogg Feed");
			
			TextView podcastName = (TextView) view.findViewById(R.id.podcast_text);
			RemoteImageView albumArt = (RemoteImageView)view.findViewById(R.id.album_art);
			TextView unlistenedText = (TextView) view.findViewById(R.id.unlistened_eps);
			
			podcastName.setText(name);
			String albumArtUrl = cursor.getString(artUrlIndex);
			if (albumArtUrl!= null && !"".equals(albumArtUrl) && !"null".equalsIgnoreCase(albumArtUrl)){
        		albumArt.setRemoteURI(albumArtUrl);
        		albumArt.loadImage();
			}
			final String unlistenedString = Utils.formUnlistenedString(context, unlistened);
			unlistenedText.setText(unlistenedString);
			
			//Add Click listener's for each row.
			final int id_index = cursor.getColumnIndex(PodcastKeys._ID);
			final long id = cursor.getLong(id_index);
			view.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					selectPodcast(v, id);
					
				}
			});
			view.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					openContextMenu(v);
					return true;
				}
			});
			ImageButton refresh = (ImageButton) view.findViewById(R.id.refresh_button);
			refresh.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					updateFeed(fullName);
				}
			});
			
			
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			v = vi.inflate(R.layout.podcast_row, parent, false);
			return v;
		}
		
	}
	
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.podcast_context, menu);
		Log.d(TAG,"Creating context menu");
		
		//Set the title of the menu
		AdapterView.AdapterContextMenuInfo item = (AdapterContextMenuInfo) menuInfo;
		TextView podcast_name = (TextView) item.targetView.findViewById(R.id.podcast_text);
		menu.setHeaderTitle(podcast_name.getText());
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()){
		case R.id.view_eps:
			selectPodcast(info.targetView, info.id);
			return true;
		case R.id.refresh_feeds:
			final String podcast_name = mPonyExpressApp.getDbHelper().getPodcastName(info.id);
			updateFeed(podcast_name);
			return true;
		case R.id.remove_podcast:
			boolean deleted = mPonyExpressApp.getDbHelper().removePodcast(info.id);
			//TODO Show Toast if deletion fails.
			return true;			
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Parse the RSS feed and update the database with new episodes in a background thread.
	 * 
	 */
	private class UpdateEpisodes extends AsyncTask <String,Void,Void>{
		
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
		protected Void doInBackground(String... name) {
			//FIXME this method is too long, break it up into smaller chuncks.
			//Check for new sixgun podcasts.
			CheckForNewPodcasts();
			
			boolean checkAll = true;
			if (!name[0].equals("")){
				checkAll = false;
				mPodcastBeingUpdated = name[0];
			}
			//Check mEpisodesToHold hasn't been changed since Activity was created.
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			mEpisodesToHold = Integer.parseInt(prefs.getString(getString(R.string.eps_stored_key), "6"));
			Log.d(TAG,"Eps to hold: " + mEpisodesToHold);
			
			//Get each podcast name if not updating a specific feed,
			//then its feed url, and update each.
			List<String> podcast_names;
			if (checkAll){
				podcast_names = 
					mPonyExpressApp.getDbHelper().listAllPodcasts();
			} else {
				podcast_names = new ArrayList<String>(Arrays.asList(mPodcastBeingUpdated));
			}
			
			for (String podcast: podcast_names){
				String podcast_url = 
					mPonyExpressApp.getDbHelper().getPodcastUrl(podcast);
				
				checkForNewArt(podcast_url);
				
				EpisodeFeedParser parser = new EpisodeFeedParser(mPonyExpressApp,
						podcast_url);
				List<Episode> episodes = parser.parse();
				
				for (Episode episode: episodes){
					//Add any episodes not already in database
					if (!mPonyExpressApp.getDbHelper().containsEpisode(episode.getTitle(),podcast)) {
						mPonyExpressApp.getDbHelper().insertEpisode(episode, podcast);
					}
				}
				
				//Determine how many episodes to remove to maintain mEpisodesToHold
				final int rows = mPonyExpressApp.getDbHelper().getNumberOfRows(podcast);
				final int episodesToDelete = rows - mEpisodesToHold;
				//Remove correct number of episodes from oldest episodes to maintain required number.
				for (int i = episodesToDelete; i > 0; i--){
					final long rowID = 
						mPonyExpressApp.getDbHelper().getOldestEpisode(podcast);
					if (rowID != -1){
						if (mPonyExpressApp.getDbHelper().isEpisodeDownloaded(rowID, podcast)){
							//delete from SD Card
							deleteFile(rowID, podcast);
						}
						//remove from database after deleting.
						mPonyExpressApp.getDbHelper().deleteEpisode(rowID, podcast);
					} else {Log.e(TAG, "Cannot find oldest episode");}
				}
			}
			return null;
		}
		private void CheckForNewPodcasts() {
			//Get current podcasts
			Log.d(TAG,"Checking for new Sixgun podcasts");
			final ArrayList<Podcast> current_sixgun_podcasts = mPonyExpressApp.getDbHelper().getCurrentPodcasts();
			//Get server list of sixgun podcasts and create list of urls
			final Context ctx = mPonyExpressApp.getApplicationContext();
			SixgunPodcastsParser parser = 
				new SixgunPodcastsParser(ctx, getString(R.string.sixgun_feeds));
			ArrayList<Podcast> sixgun_podcasts =(ArrayList<Podcast>) parser.parse();
			//Compare the two arraylists and remove any that exist in both
			sixgun_podcasts.removeAll(current_sixgun_podcasts);
			//Add any new podcasts to the podcasts table
			if (!sixgun_podcasts.isEmpty()){
				Log.d(TAG, "Adding new Podcasts!");
				mPonyExpressApp.getDbHelper().addNewPodcasts(sixgun_podcasts);
			}
		}
		
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
		
		/** Deletes a file from the SD Card.
		 * 
		 * @param rowID of the file to be deleted from the database.
		 */
		private void deleteFile(long rowID, String podcast_name) {
			File rootPath = Environment.getExternalStorageDirectory();
			File dirPath = new File(rootPath,PonyExpressApp.PODCAST_PATH);
			String filename = mPonyExpressApp.getDbHelper().getEpisodeFilename(rowID, podcast_name);
			//Add the podcast name as a folder under the PODCAST_PATH
			filename = podcast_name + filename;
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
			//Set mLastUpdate and store in shared preferences
			mLastUpdate = new GregorianCalendar(Locale.US);
			SharedPreferences updateStatus = getSharedPreferences(UPDATEFILE, 0);
			SharedPreferences.Editor editor = updateStatus.edit();
			editor.putLong(LASTUPDATE, mLastUpdate.getTimeInMillis());
			editor.commit();
			//re-list podcasts to update new episode counts
			listPodcasts();
		}
		
	};
	
	//FIXME Make this user startable to fix issue where user deletes a file.
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
					File podcast_path = new File(path,podcast);
					String[] files = podcast_path.list();
					filesOnDisk.addAll(Arrays.asList(files));
					//Get a Map of filenames (and their index) that are in the database
					final Map<Long, String> filesInDatabase = 
						mPonyExpressApp.getDbHelper().getFilenamesOnDisk(podcast);
					if (filesOnDisk != null){
						//If file is in database but not on disk, mark as not downloaded in database.
						final int mapSize = filesInDatabase.size();
						Iterator<Entry<Long, String>> fileIter = filesInDatabase.entrySet().iterator();
						for (int i = 0; i < mapSize; i++){
							Map.Entry<Long, String> entry = (Map.Entry<Long, String>)fileIter.next();
							if (!filesOnDisk.contains(entry.getValue())){
								mPonyExpressApp.getDbHelper().update(podcast, entry.getKey(), 
										EpisodeKeys.DOWNLOADED, "false");
							}
						}
					}
				}
			}
			return null;
		}
	}
	/**
	 * Receiver that takes a broadcast sent by the DbHandler when 
	 * the database has been changed by the deletion of a podcast, so the list 
	 * can be updated.
	 */
	public class PodcastDeleted extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			listPodcasts();
		}
		
	}
}
