/*
 * Copyright 2011 Paul Elms
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

import java.net.URL;
import java.util.List;

import org.sixgun.ponyexpress.Podcast;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.ReturnCodes;
import org.sixgun.ponyexpress.util.BackupFileWriter;
import org.sixgun.ponyexpress.util.BackupParser;
import org.sixgun.ponyexpress.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class AddNewPodcastFeedActivity extends Activity {

	private static final String TAG = "PonyExpress AddNewPopdcastFeedActivity";

	private TextView mFeedText;
	private TextView mGroupText;
	private TextView mTagText;
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
		mGroupText = (EditText) findViewById(R.id.group_entry);
		mTagText = (EditText) findViewById(R.id.tag_entry);

		//If the feedURl has been sent in the intent populate the text box
		if (!getIntent().getExtras().getString(PodcastKeys.FEED_URL).equals("")){
			String url = getIntent().getExtras().getString(PodcastKeys.FEED_URL);
			//Chop off the scheme (http:// or podcast://)
			int index = url.indexOf("//");
			if (index != -1){
				url = url.substring(index +2);
				mFeedText.append(url);
			} else {
				Toast.makeText(this, R.string.url_error, Toast.LENGTH_SHORT).show();
			}
			
		}
	}
	
	public void okButtonPressed(View v) {
		final String feed = mFeedText.getText().toString();
		final String group = mGroupText.getText().toString();
		final String tag = mTagText.getText().toString();

		Podcast podcast = new Podcast();

		URL  feedUrl = Utils.getURL(feed);
		if (Utils.checkURL(feedUrl) != null){
			podcast.setFeedUrl(feedUrl);
			//TODO Check identica group exists, (query identica).
			if (!group.equals("") && !group.equals("!")){
				//Remove the leading '!' and store.
				podcast.setIdenticaGroup(group.substring(1));
			}
			if(!tag.equals("") && !tag.equals("#")){
				if(tag.startsWith("#")) {
					podcast.setIdenticaTag(tag.substring(1));
				} else {
					podcast.setIdenticaTag(tag);
				}
			}
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
	
	public void cancelButtonPressed(View v){
		setResult(RESULT_CANCELED);
		finish();
	}
	
	public void backupButtonPressed(View v){
		Log.d(TAG,"Backing up to file...");
		//Start the backup Async
		Backup task = (Backup) new Backup().execute();
		if (task.isCancelled()){
			Log.d(TAG, "Backup canceled");
		}
	}
	
	public void restoreButtonPressed(View v){
		Log.d(TAG,"Restore from backup file...");
		//Start the restore Async
		Restore task = (Restore) new Restore().execute();
		if (task.isCancelled()){
			Log.d(TAG, "Restore canceled");
		}
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

	private class Backup extends AsyncTask<Void,Integer,Integer>{

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
		protected Integer doInBackground(Void... v) {
			List<String> podcastlist = mPonyExpressApp.getDbHelper().getAllPodcastsUrls();
			final BackupFileWriter backupwriter = new BackupFileWriter();
			return backupwriter.writeBackupOpml(podcastlist);
		}

		protected void onPostExecute(Integer return_code) {
			mProgDialog.hide();
			//Handle the return codes.
			switch (return_code) {
			case ReturnCodes.ASK_TO_OVERWRITE:
				startOverwriteDialog();
				break;
			case ReturnCodes.SD_CARD_NOT_WRITABLE:
				Toast.makeText(mPonyExpressApp,
						R.string.cannot_write, Toast.LENGTH_LONG).show();
				break;
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
	private class Restore extends AsyncTask <Void,Void,Integer>{

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
		protected Integer doInBackground(Void... params) {

			final BackupParser backupparser = new BackupParser();
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
	 * @param name
	 */
	private void sendToMainActivity(String update_code){
		Intent intent = new Intent();
		intent.putExtra(PodcastKeys.NAME, update_code);
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * This is the method that creates and shows the "Ask to overwrite" dialog.
	 */
	private void startOverwriteDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getText(R.string.a_backup_file_));
		builder.setCancelable(false);
		builder.setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Utils.deleteBackupFile();
				//Start the backup Async
				Backup task = (Backup) new Backup().execute();
				if (task.isCancelled()){
					Log.d(TAG, "Backup canceled");
				}
			}
		});
		builder.setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
}
