/*
 * Copyright 2011-2013 Paul Elms
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

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;

import org.sixgun.ponyexpress.Podcast;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.ReturnCodes;
import org.sixgun.ponyexpress.SearchSuggestionsProvider;
import org.sixgun.ponyexpress.util.BackupFileWriter;
import org.sixgun.ponyexpress.util.BackupParser;
import org.sixgun.ponyexpress.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class AddNewPodcastFeedActivity extends Activity {

	public static final String PONY_EXPRESS_PODCASTS_OPML = "PonyExpress_Podcasts.opml";
	private static final String TAG = "PonyExpress AddNewPopdcastFeedActivity";
	private static final int FILE_CHOOSER = 1;
	private static final int CLEAR_HISTORY = 0;

	private TextView mFeedText;
	private PonyExpressApp mPonyExpressApp;
	private ProgressDialog mProgDialog;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_feed);

		//Create Progress Dialogs for later use.
		mProgDialog = new ProgressDialog(this);

		mPonyExpressApp = (PonyExpressApp)getApplication();
		mFeedText = (EditText) findViewById(R.id.feed_entry);
		handleIntent(getIntent());

		
	}
	@Override
	protected void onNewIntent(Intent intent) {
	    setIntent(intent);
	    handleIntent(intent);
	}
	
	private void handleIntent(Intent intent){
		//If the feedURl has been sent in the intent populate the text box
		if (intent.getExtras().getString(PodcastKeys.FEED_URL) != null){
			if (!intent.getExtras().getString(PodcastKeys.FEED_URL).equals("")){
				String url = getIntent().getExtras().getString(PodcastKeys.FEED_URL);
				//Chop off the scheme (http:// or podcast://)
				int index = url.indexOf("//");
				if (index != -1){
					url = url.substring(index +2);
					mFeedText.append(url);
					okButtonPressed(null);
				} else {
					Toast.makeText(this, R.string.url_error, Toast.LENGTH_SHORT).show();
				}
			}
		}
		// Get the intent, if a search action, get the query and send on to MiroActivity
		else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
	                SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
	        suggestions.saveRecentQuery(query, null);
	      final Intent searchIntent = new Intent(mPonyExpressApp, MiroActivity.class);
	      searchIntent.setAction(Intent.ACTION_SEARCH);
	      searchIntent.putExtra(SearchManager.QUERY, query);
	      startActivityForResult(searchIntent, PonyExpressActivity.ADD_FEED);
		}
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.add_feeds_options_menu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.settings_menu:
			startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
			return true;
		case R.id.clear_search:
			showDialog(CLEAR_HISTORY);
			return true;
		case R.id.search:
			onSearchRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		//No need for a switch/case on id as only one dialog. 
		AlertDialog.Builder builder = new Builder(this);
		builder.setMessage(R.string.clear_history_dialog_message)
	       .setTitle(R.string.clear_history_dialog_title);
		// Add the buttons
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User clicked OK button
		        	   SearchRecentSuggestions suggestions = new SearchRecentSuggestions(mPonyExpressApp,
		        		        SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
		        		suggestions.clearHistory();
		           }
		       });
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User cancelled the dialog
		        	   dialog.cancel();
		           }
		       });
		AlertDialog dialog = builder.create();
		return dialog;
		
	}
	public void browseButtonPressed(View v){
		final Intent intent = new Intent(mPonyExpressApp, MiroActivity.class);
		startActivityForResult(intent, PonyExpressActivity.ADD_FEED);
		
	}
	
	public void searchButtonPressed(View v){
		onSearchRequested();
	}
	
	public void okButtonPressed(View v) {
		final String feed = mFeedText.getText().toString();
		addPodcast(feed);		
	}
	
	public void cancelButtonPressed(View v){
		setResult(RESULT_CANCELED);
		finish();
	}
	
	public void backupButtonPressed(View v){
		Log.d(TAG,"Opening FileChooser...");
		Intent intent = new Intent(mPonyExpressApp, FileChooserActivity.class);
		intent.putExtra(FileChooserActivity._Theme, android.R.style.Theme_Dialog);
		intent.putExtra(FileChooserActivity._SaveDialog, true);
		intent.putExtra(FileChooserActivity._DefaultFilename, PONY_EXPRESS_PODCASTS_OPML);
		
		startActivityForResult(intent, FILE_CHOOSER);
		
	}
	
	public void restoreButtonPressed(View v){
		Log.d(TAG,"Restore from backup file...");
		Intent intent = new Intent(mPonyExpressApp, FileChooserActivity.class);
		intent.putExtra(FileChooserActivity._Theme, android.R.style.Theme_Dialog);
		intent.putExtra(FileChooserActivity._MultiSelection, false);
		
		startActivityForResult(intent, FILE_CHOOSER);

	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_CANCELED){
			//User may back out of MiroAcivity
			return;
		}
		switch (requestCode) {
		case FILE_CHOOSER:
			if (resultCode == RESULT_OK) {
				boolean saveDialog = data.getBooleanExtra(FileChooserActivity._SaveDialog, false);
				/*
				 * a list of files will always return,
				 * if selection mode is single, the list contains one file
				 */
				@SuppressWarnings("unchecked")
				List<LocalFile> files = (List<LocalFile>)
						data.getSerializableExtra(FileChooserActivity._Results);
				
				if (!files.isEmpty()){
					if (saveDialog){ //Backing up
						Log.d(TAG, "Path returned is: " + files.get(0).getPath());
						//Start the backup Async
						Backup task = (Backup) new Backup().execute(files.get(0));
						if (task.isCancelled()){
							Log.d(TAG, "Backup canceled");
						}
					} else {
						//Start the restore Async
						Restore task = (Restore) new Restore().execute(files.get(0));
						if (task.isCancelled()){
							Log.d(TAG, "Restore canceled");
						}
					}
				}
			} break;
		case PonyExpressActivity.ADD_FEED:
			final String feed_url = data.getExtras().getString(PodcastKeys.FEED_URL);
			addPodcast(feed_url);
			break;
		default:
			Log.e(TAG, "Unknown request code in result recieved by AddNEwPodcastFeedsActivity");
		}
	}

	/**
	 *  Cancel progress dialog when activity destroyed.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Dismiss dialog now or it will leak.
		if (mProgDialog.isShowing()){
			mProgDialog.dismiss();
		}
	}
	private void addPodcast(String feed_url){
		Podcast podcast = new Podcast();

		URL  feedUrl = Utils.getURL(feed_url);
		HttpURLConnection conn = null;
		try {
			conn = Utils.checkURL(feedUrl);
		} catch (SocketTimeoutException e) {
			Log.e(TAG, "Feed url timed out", e);
		}
		if (conn != null){
			conn.disconnect();
			podcast.setFeedUrl(feedUrl);
			//Check if the new url is already in the database
			boolean checkDatabase = mPonyExpressApp.getDbHelper().checkDatabaseForUrl(podcast);
			if (checkDatabase) {
				Toast.makeText(mPonyExpressApp, R.string.already_in_db, Toast.LENGTH_SHORT).show();
			}else{
				final String name = mPonyExpressApp.getDbHelper().addNewPodcast(podcast);
				Toast.makeText(mPonyExpressApp, R.string.adding_podcast, Toast.LENGTH_SHORT).show();
				//Send podcast name back to PonyExpressActivity so it can update the new feed.
				sendToMainActivity(name);
				
			}
		} else Toast.makeText(mPonyExpressApp, R.string.url_error, Toast.LENGTH_SHORT).show();
	}

	private class Backup extends AsyncTask<File,Integer,Integer>{

		/*
		 * This is carried out in the UI thread before the background tasks are started.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgDialog.setMessage(getText(R.string.backing_up_feeds));
			mProgDialog.show();
		}

		@Override
		protected Integer doInBackground(File... f){
			List<String> podcastlist = mPonyExpressApp.getDbHelper().getAllPodcastsUrls();
			final BackupFileWriter backupwriter = new BackupFileWriter();
			return backupwriter.writeBackupOpml(podcastlist,f[0]);
		}

		protected void onPostExecute(Integer return_code) {
			mProgDialog.hide();
			//Handle the return codes.
			switch (return_code) {
			case ReturnCodes.ALL_OK:
				Toast.makeText(mPonyExpressApp,
						R.string.backup_successful, Toast.LENGTH_LONG).show();
				Log.d(TAG,"Backup finished...");
				break;
			}
		}
	}

	/**
	 * This Async uses BackupParser to restore podcast feeds from a backup file that is 
	 * compatible with gPodder.net.
	 */
	private class Restore extends AsyncTask <File,Void,Integer>{

		/*
		 * This is carried out in the UI thread before the background tasks are started.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgDialog.setMessage(getText(R.string.restoring));
			mProgDialog.show();

		}

		@Override
		protected Integer doInBackground(File... f) {

			final BackupParser backupparser = new BackupParser(f[0]);
			List<String> podcasts = backupparser.parse();

			//This is where we handle the return from backupparser.parse().
			//If an empty List<String> was returned, handle it here...
			int return_code = 0;
			try{
				return_code = Integer.valueOf(podcasts.get(0));
			} catch (IndexOutOfBoundsException e){
				//An empty List<String> was returned
				return ReturnCodes.PARSING_ERROR;
			} catch (NumberFormatException e){
				//If we are here, we have a proper List<String> of urls. Let's put em to good use.
				for (String url: podcasts){
					Podcast podcast = new Podcast();
					URL feedUrl = Utils.getURL(url);
					podcast.setFeedUrl(feedUrl);
					boolean checkDatabase = mPonyExpressApp.getDbHelper().checkDatabaseForUrl(podcast);
					if (!checkDatabase) {
						mPonyExpressApp.getDbHelper().addNewPodcast(podcast);
					}
				}
				return ReturnCodes.ALL_OK;
			}

			//If a single integer in place 0 was returned, there was an error.
			//Handle it here.
			switch (return_code) {
				case (ReturnCodes.NO_BACKUP_FILE):
					return ReturnCodes.NO_BACKUP_FILE;

				case (ReturnCodes.PARSING_ERROR):
					return ReturnCodes.PARSING_ERROR;
			}
			//We should never get here, but the compiler asks for a return...
			return null;
		}

		protected void onPostExecute(Integer return_code) {
			mProgDialog.hide();
			//Handle the return codes.
			switch (return_code) {
			case ReturnCodes.NO_BACKUP_FILE:
				Toast.makeText(mPonyExpressApp,
						R.string.there_is_no_backup, Toast.LENGTH_LONG).show();
				break;
			case ReturnCodes.PARSING_ERROR:
				Toast.makeText(mPonyExpressApp,
						R.string.error_parsing_backup_file, Toast.LENGTH_LONG).show();
				break;
			case ReturnCodes.ALL_OK:
				Log.d(TAG,"Restore finished...");
				sendToMainActivity(PonyExpressActivity.UPDATE_ALL);
			}
		}
	}

	/**
	 * This method closes this activity and sends a string back to the main activity.
	 * @param update_code the name of a podcast to update or UPDATE_ALL
	 */
	private void sendToMainActivity(String update_code){
		Intent intent = new Intent(mPonyExpressApp, PonyExpressActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(PodcastKeys.NAME, update_code);
		startActivity(intent);
	}

}
