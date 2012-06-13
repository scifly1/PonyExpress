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
import android.app.ProgressDialog;
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

		OnClickListener OKButtonListener =  new OnClickListener() {

			@Override
			public void onClick(View v) {
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
					boolean mCheckDatabase = mPonyExpressApp.getDbHelper().checkDatabaseForUrl(podcast);
					if (mCheckDatabase == true) {
						Toast.makeText(mPonyExpressApp, R.string.already_in_db, Toast.LENGTH_SHORT).show();
					}else{
						final String name = mPonyExpressApp.getDbHelper().addNewPodcast(podcast);
						Toast.makeText(mPonyExpressApp, R.string.adding_podcast, Toast.LENGTH_SHORT).show();
						//Send podcast name back to PonyExpressActivity so it can update the new feed.
						Intent intent = new Intent();
						intent.putExtra(PodcastKeys.NAME, name);
						setResult(RESULT_OK, intent);
						finish();
					}
				} else Toast.makeText(mPonyExpressApp, R.string.url_error, Toast.LENGTH_SHORT).show();
			}
		};
		OnClickListener CancelButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		};

		OnClickListener restoreButtonListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG,"Restore from backup file...");
				//Start the restore Async
				Restore task = (Restore) new Restore().execute();
				if (task.isCancelled()){
					Log.d(TAG, "Restore canceled");
				}
			}
		};

		OnClickListener backupButtonListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d(TAG,"Backing up to file...");
				List<String> podcastlist = mPonyExpressApp.getDbHelper().getAllPodcastsUrls();
				@SuppressWarnings("unchecked")
				Backup task = (Backup) new Backup().execute(podcastlist);
				if (task.isCancelled()){
					Log.d(TAG, "Backup canceled");
				}
			}
		};

		mPonyExpressApp = (PonyExpressApp)getApplication();
		mFeedText = (EditText) findViewById(R.id.feed_entry);
		mGroupText = (EditText) findViewById(R.id.group_entry);
		mTagText = (EditText) findViewById(R.id.tag_entry);
		Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(OKButtonListener);
		Button cancelButton = (Button) findViewById(R.id.cancel);
		cancelButton.setOnClickListener(CancelButtonListener);
		Button backupButton = (Button) findViewById(R.id.backup);
		backupButton.setOnClickListener(backupButtonListener);
		Button restoreButton = (Button) findViewById(R.id.restore);
		restoreButton.setOnClickListener(restoreButtonListener);

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

	private class Backup extends AsyncTask<List<String>,Integer,Integer>{

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
		protected Integer doInBackground(List<String>... list) {
			final BackupFileWriter backupwriter = new BackupFileWriter();
			return backupwriter.writeBackupOpml(list[0]);
		}

		protected void onPostExecute(Integer return_code) {
			mProgDialog.hide();
			switch (return_code) {
			case ReturnCodes.ASK_TO_OVERWRITE:
				//TODO Toast
				break;
			case ReturnCodes.SD_CARD_NOT_WRITABLE:
				//TODO Toast
				break;
			case ReturnCodes.ALL_OK:
				//TODO Toast
				Log.d(TAG,"Backup finished...");
				break;
			}
		}
	}

	/**
	 * This Async uses BackupParser to restore podcast feeds from a backup file that is 
	 * compatible with gPodder.net.
	 */
	private class Restore extends AsyncTask <Void,Void,Void>{

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
		protected Void doInBackground(Void... params) {

			final BackupParser backupparser = new BackupParser();
			List<String> podcasts = backupparser.parse();
			for (String url: podcasts){
				Podcast podcast = new Podcast();
				URL feedUrl = Utils.getURL(url);
				podcast.setFeedUrl(feedUrl);
				boolean checkDatabase = mPonyExpressApp.getDbHelper().checkDatabaseForUrl(podcast);
				if (!checkDatabase) {
					mPonyExpressApp.getDbHelper().addNewPodcast(podcast);
				}
			}
			return null;
		}

		protected void onPostExecute(Void v) {
			Intent intent = new Intent();
			intent.putExtra(PodcastKeys.NAME, PonyExpressActivity.UPDATE_ALL);
			setResult(RESULT_OK, intent);
			mProgDialog.hide();
			Log.d(TAG,"Restore finished...");
			finish();
		}
	}
}
