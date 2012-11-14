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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastCursorAdapter;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.service.ScheduledDownloadService;
import org.sixgun.ponyexpress.service.UpdaterService;
import org.sixgun.ponyexpress.util.BackupFileWriter;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Launch Activity for PonyExpress.
 */
public class PonyExpressActivity extends ListActivity {

	private static final String TAG = "PonyExpressActivity";
	public static final String LASTUPDATE = "lastupdate";
	public static final String FIRST = "first";
	
	//Update codes
	public static final String UPDATE_SIXGUN_SHOW_LIST = "Update_Sixgun";
	public static final String UPDATE_ALL = "Update_all";
	public static final String UPDATE_SINGLE = "Update_single";
	public static final String SET_ALARM_ONLY = "Set_alarm_only";
		
	private static final int ABOUT_DIALOG = 4;
	private static final int ADD_FEED = 0;

	protected PonyExpressApp mPonyExpressApp; 
	private ProgressDialog mProgDialog;
	private int mEpisodesToHold;
	private BroadcastReceiver mPodcastDeletedReceiver;
	OnClickListener mClickHandler;
	private int mListSize;
	private ViewGroup mListFooter;
	private boolean mListingPodcasts;

	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		ViewGroup list_root = (ViewGroup) findViewById(R.id.podcast_list_root);
		list_root.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
				//Determine if we need to add a footer and hide the base footer.
				if (mListingPodcasts){ //onGlobalLayout is called frequently, we only want this to run
					//when listing the podcasts.
					ListView list = getListView();
					int last_pos_visible = list.getLastVisiblePosition();
					if (last_pos_visible == -1){
						Log.d(TAG, "We should not be here!!");
						return; //we are being called when exiting activity which should not happen as mListingPodcasts should be false.
					}
					ViewGroup footer_layout = (ViewGroup) findViewById(R.id.footer_layout);
					if (last_pos_visible < mListSize - 1 && footer_layout.getVisibility() == View.VISIBLE && list.getFooterViewsCount() < 1){
						//The last position is not visible and we don't have a footer already
						//so add footer to list and hide the 'other' footer.
						listPodcasts(true);
					} else if (last_pos_visible == mListSize && footer_layout.getVisibility() == View.GONE){
						//last Position is visible so remove footer if present.
						list.removeFooterView(mListFooter);
						footer_layout.setVisibility(View.VISIBLE);
					}
					mListingPodcasts = false;
				}			
			}
		});
		
		//Get the application context.
		mPonyExpressApp = (PonyExpressApp)getApplication();
		
		//Get the number of episodes to hold from preferences
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mEpisodesToHold = Integer.parseInt(prefs.getString(getString(R.string.eps_stored_key), "6"));
		Log.d(TAG,"Eps to hold: " + mEpisodesToHold);
				
		//Create Progress Dialogs for later use.
		mProgDialog = new ProgressDialog(this);
		mProgDialog.setMessage(getText(R.string.setting_up));
		
		//Is this the first run?
		final boolean first = prefs.getBoolean(FIRST, true);
		if (first){
			onFirstRun(prefs);
		}else{
			//Make sure the update alarm and scheduled downloads are set properly.
			updateFeed(SET_ALARM_ONLY);
			
			if (!mPonyExpressApp.isScheduledDownloadServiceRunning()){
				Intent intent = new Intent(mPonyExpressApp, ScheduledDownloadService.class);
				intent.putExtra(PonyExpressActivity.SET_ALARM_ONLY, true);
				mPonyExpressApp.startService(intent);
			}
		}
		
		//Check SDCard contents and database match.
		new DatabaseCheck().execute();
		
		//Update the Episodes list if the database has been upgraded.
		if (mPonyExpressApp.getDbHelper().mDatabaseUpgraded){
			updateFeed(UPDATE_ALL);
			mPonyExpressApp.getDbHelper().mDatabaseUpgraded = false;
		}
		
		mPodcastDeletedReceiver = new PodcastDeleted();
		
		listPodcasts(false);
		
		//If started by clicking a podcast link call AddPodcast
		Intent intent = getIntent();
		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)){
			final String url = intent.getDataString();
			addPodcast(null, url);
		}
	}
	
	// This method shows a dialog, and calls updateFeed() if this is the first time Pony 
	// has been run.  The SharedPrefrences then get changed to false.
	private void onFirstRun(SharedPreferences prefs) {
		mProgDialog.show();
		updateFeed(UPDATE_SIXGUN_SHOW_LIST);
		//Sets the preference to false so this doesn't get called again.
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FIRST, false);
        editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter("org.sixgun.ponyexpress.PODCAST_DELETED");
		registerReceiver(mPodcastDeletedReceiver, filter);
		listPodcasts(false);
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
	}
	
	private class PonyPodcastCursorAdapter extends PodcastCursorAdapter{

		public PonyPodcastCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			//Call super's method to get all layout sorted
			super.bindView(view, context, cursor);

			//Add Click listener's for each row.
			final int id_index = cursor.getColumnIndex(PodcastKeys._ID);
			final long id = cursor.getLong(id_index);
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					selectPodcast(id);

				}
			});
			view.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					openContextMenu(v);
					return true;
				}
			});

		}
	}

	
	

	/**
	 * Starts the EpisodesActivity with the selected podcast
	 * @param id row_id of the podcast in the database
	 */
	protected void selectPodcast(long id) {
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
	public void addPodcast(View v, String url) {
		Intent intent = new Intent(mPonyExpressApp, AddNewPodcastFeedActivity.class);
		intent.putExtra(PodcastKeys.FEED_URL, url);
		startActivityForResult(intent, ADD_FEED);
	}
	
	/**
	 * Show the playlist
	 */
	public void showPlaylist(View v) {
		startActivity(new Intent(mPonyExpressApp, PlaylistActivity.class));
	}

	
	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		//No need for Switch/case on requestCode as only one result expected
		if (resultCode == RESULT_OK){
			mProgDialog.show();
			final String podcast_name = data.getExtras().
					getString(PodcastKeys.NAME);
			updateFeed(podcast_name);
		}
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
	        updateFeed(UPDATE_ALL);
	        return true;
	    case R.id.settings_menu:
	    	startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
	    	return true;
	    case R.id.add_podcast:
	    	addPodcast(null, "");
	    	return true;
	    case R.id.identica_account_settings:
	    	//Fire off AccountSetup screen
	    	startActivity(new Intent(
					mPonyExpressApp,IdenticaAccountSetupActivity.class));
	    	return true;
	    case R.id.about:
	    	showDialog(ABOUT_DIALOG);
	    	return true;
	    case R.id.add_sixgun:
	        updateFeed(UPDATE_SIXGUN_SHOW_LIST);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
		}
	}

	private void updateFeed(String podcastName){
		if (mPonyExpressApp.isUpdaterServiceRunning() && podcastName != SET_ALARM_ONLY){
        	Toast.makeText(mPonyExpressApp, 
					R.string.please_wait, Toast.LENGTH_LONG).show();
		}else{
			UpdateEpisodes task = (UpdateEpisodes) new UpdateEpisodes().execute(podcastName);
			if (task.isCancelled()){
				Log.d(TAG, "Cancelled Update, No Connectivity");
			}
		}
	}
	/**
	 * This method lists the podcasts found in the database in the ListView.
	 * @param addFooter This is a bit of a hack where we draw the list without 
	 * a footer, determine with the globalLayoutListener if we need a footer and then
	 * re-call this to add a footer to the adapter.
	 */
	protected void listPodcasts(boolean addFooter) {
		Cursor c = mPonyExpressApp.getDbHelper().getAllPodcastNamesAndArt();
		startManagingCursor(c);
		//Create a CursorAdapter to map podcast title and art to the ListView.
		PonyPodcastCursorAdapter adapter = new PonyPodcastCursorAdapter(mPonyExpressApp, c);
		mListingPodcasts = true;
		//Add footer to the listview if required.
		if (addFooter){
			ViewGroup footer_layout = (ViewGroup) findViewById(R.id.footer_layout);
			mListFooter = (ViewGroup) getLayoutInflater().inflate(R.layout.main_footer, null);
			ListView list = getListView();
			//prevent more than one footer being added each time we pass through here.
			if (list.getFooterViewsCount() == 0){
				list.addFooterView(mListFooter);
				footer_layout.setVisibility(View.GONE);
			}
		}
		mListSize = adapter.getCount();
		setListAdapter(adapter);
		
		registerForContextMenu(getListView());
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
			selectPodcast(info.id);
			return true;
		case R.id.refresh_feeds:
			final String podcast_name = mPonyExpressApp.getDbHelper().getPodcastName(info.id);
			updateFeed(podcast_name);
			return true;
		case R.id.remove_podcast:
			boolean deleted = mPonyExpressApp.getDbHelper().removePodcast(info.id);
			if(!deleted) {
				Toast.makeText(mPonyExpressApp, R.string.delete_failed, Toast.LENGTH_SHORT)
					.show();
			}
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
			if (!mPonyExpressApp.getInternetHelper().checkConnectivity()){
				Toast.makeText(mPonyExpressApp, 
						R.string.no_internet_connection, Toast.LENGTH_LONG).show();
				cancel(true);
			}
			if (mPonyExpressApp.isUpdaterServiceRunning()){
				cancel(true);
			}
		}
		/*
		 * This is done in a new thread,
		 */
		@Override
		protected Void doInBackground(String... input_string) {
			
			//Start UpdaterSevice with the input_string[0]
			Intent intent = new Intent(mPonyExpressApp,UpdaterService.class);
			intent.putExtra(input_string[0], true);
			intent.putExtra(UPDATE_SINGLE, input_string[0]);
			startService(intent);
						
			//Pause until all UpdaterServices are done
			while (mPonyExpressApp.isUpdaterServiceRunning()){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Log.e(TAG, "UpdateEpisodes failed to sleep");
				}
			}
			return null;
		}
		
		/* 
		 */
		@Override
		protected void onCancelled() {
			super.onCancelled();
			mProgDialog.hide();
		}
		/* 
		 */
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mProgDialog.hide();
			//re-list podcasts to update new episode counts
			listPodcasts(false);
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
					//Check if podcast is really the backup file.
					if (podcast.equals(BackupFileWriter.BACKUP_FILENAME)){
						continue;
					}
					File podcast_path = new File(path,podcast);
					String[] files = podcast_path.list();
					try{
						filesOnDisk.addAll(Arrays.asList(files));
					}catch(NullPointerException e){
						Log.d(TAG,"NullPOinter");
					}
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
			//need to recheck the ListSize to see if the footer needs to be moved
			onResume();
		}
		
	}
}
