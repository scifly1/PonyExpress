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
package org.sixgun.ponyexpress.fragment;

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
import org.sixgun.ponyexpress.activity.MiroActivity;
import org.sixgun.ponyexpress.activity.MiroFragsActivity;
import org.sixgun.ponyexpress.activity.PonyExpressFragsActivity;
import org.sixgun.ponyexpress.activity.PreferencesActivity;
import org.sixgun.ponyexpress.util.BackupFileWriter;
import org.sixgun.ponyexpress.util.BackupParser;
import org.sixgun.ponyexpress.util.PonyLogger;
import org.sixgun.ponyexpress.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class AddNewPodcastsFragment extends Fragment implements OnClickListener {

	public static AddNewPodcastsFragment newInstance(String feed_url) {
        AddNewPodcastsFragment newPods = new AddNewPodcastsFragment();

        // Supply input as an argument.
        Bundle args = new Bundle();
        args.putString(PodcastKeys.FEED_URL, feed_url);
        newPods.setArguments(args);

        return newPods;
    }

	private PonyExpressApp mPonyExpressApp;
	private TextView mFeedText;
	public ProgressDialogFragment mProgDialog;
	private boolean mDualPane;
	private MiroCategoriesFragment mMiroCategories;
	private static final String TAG = "AddNewPodcastsFragment";
	private static final String PONY_EXPRESS_PODCASTS_OPML = "PonyExpress_Podcasts.opml";
	private static final int FILE_CHOOSER = 0;
	public static final int ADD_FEED = 1;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		mPonyExpressApp = (PonyExpressApp) getActivity().getApplication();
		
		//If the feedURl has been sent in the intent handle it
		//Get Podcast name and album art url from bundle or saved instance.
		Bundle data;
		if (savedInstanceState != null){
			data = savedInstanceState;
		} else {
			data = getArguments();
		}
		String url = data.getString(PodcastKeys.FEED_URL);
		if (url != null){
			if (!url.equals("")){
				//Chop off the scheme (http:// or podcast://)
				int index = url.indexOf("//");
				if (index != -1){
					new AddPodcast().execute(url);
				} else {
					Toast.makeText(getActivity(), R.string.url_error, Toast.LENGTH_SHORT).show();
				}
			}
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.add_feed, container, false);
		
		Button browse = (Button) v.findViewById(R.id.browse_miro);
		browse.setOnClickListener(this);
		Button search = (Button) v.findViewById(R.id.search_miro);
		search.setOnClickListener(this);
		Button export = (Button) v.findViewById(R.id.backup);
		export.setOnClickListener(this);
		Button load = (Button) v.findViewById(R.id.restore);
		load.setOnClickListener(this);
		Button ok =(Button) v.findViewById(R.id.ok);
		ok.setOnClickListener(this);
		Button cancel = (Button) v.findViewById(R.id.cancel);
		cancel .setOnClickListener(this);
		mFeedText = (TextView) v.findViewById(R.id.feed_entry);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// Check to see if we have a frame in which to embed the MiroFragments
        // directly in the containing UI.
        View secondFrame = getActivity().findViewById(R.id.second_pane);
        mDualPane = secondFrame != null && secondFrame.getVisibility() == View.VISIBLE;
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.browse_miro:
			if (mDualPane){
				if (mMiroCategories == null) {
					mMiroCategories = new MiroCategoriesFragment();
				}
				// Execute a transaction, replacing any existing fragment
				// with this one inside the frame.
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.second_pane, mMiroCategories, "miroCategories");
				ft.addToBackStack(null);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				ft.commit();
			} else {
				final Intent intent = new Intent(mPonyExpressApp, MiroFragsActivity.class);
				startActivityForResult(intent, ADD_FEED);
			}
			break;
		case R.id.search_miro:
			getActivity().onSearchRequested();
			break;
		case R.id.backup:
			PonyLogger.d(TAG, "Opening FileChooser...");
			Intent backup_intent = new Intent(mPonyExpressApp, FileChooserActivity.class);
			backup_intent.putExtra(FileChooserActivity._Theme, android.R.style.Theme_Dialog);
			backup_intent.putExtra(FileChooserActivity._SaveDialog, true);
			backup_intent.putExtra(FileChooserActivity._DefaultFilename, PONY_EXPRESS_PODCASTS_OPML);
			
			startActivityForResult(backup_intent, FILE_CHOOSER);
			break;
		case R.id.restore:
			PonyLogger.d(TAG, "Restore from backup file...");
			Intent res_intent = new Intent(mPonyExpressApp, FileChooserActivity.class);
			res_intent.putExtra(FileChooserActivity._Theme, android.R.style.Theme_Dialog);
			res_intent.putExtra(FileChooserActivity._MultiSelection, false);
			
			startActivityForResult(res_intent, FILE_CHOOSER);
			break;
		case R.id.ok:
			final String feed = mFeedText.getText().toString();
			new AddPodcast().execute(feed);
			break;
		case R.id.cancel:
			getActivity().getSupportFragmentManager().popBackStackImmediate();
			break;
		default:
			break;
		}
		
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.add_feeds_options_menu, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(R.id.main_group, false);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.settings_menu:
			startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
			return true;
		case R.id.clear_search:
			new ClearSearchDialog().show(getFragmentManager(), "clear_search_dialog");
			return true;
		case R.id.search:
			getActivity().onSearchRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_CANCELED){
			//User may back out of MiroAcivity
			return;
		}
		switch (requestCode) {
		case FILE_CHOOSER:
			if (resultCode == Activity.RESULT_OK) {
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
						PonyLogger.d(TAG, "Path returned is: "
									+ files.get(0).getPath());
						//Start the backup Async
						Backup task = (Backup) new Backup().execute(files.get(0));
						if (task.isCancelled()){
							PonyLogger.d(TAG, "Backup canceled");
						}
					} else {
						//Start the restore Async
						Restore task = (Restore) new Restore().execute(files.get(0));
						if (task.isCancelled()){
							PonyLogger.d(TAG, "Restore canceled");
						}
					}
				}
			} break;
		case ADD_FEED:
			final String feed_url = data.getExtras().getString(PodcastKeys.FEED_URL);
			new AddPodcast().execute(feed_url);
			break;
		default:
			PonyLogger.e(TAG, "Unknown request code in result recieved by AddNewPodcastsFragment");
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private class AddPodcast extends AsyncTask<String, Void, Integer> {


		private String aPodcastName;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgDialog = ProgressDialogFragment.newInstance(getText(R.string.adding_podcast));
			mProgDialog.show(getFragmentManager(), "update Progress Dialog");
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			mProgDialog.dismiss();
		}

		@Override
		protected Integer doInBackground(String... params) {
			Podcast podcast = new Podcast();
			HttpURLConnection conn = null;

			final URL  feedUrl = Utils.getURL(params[0]);
			try {
				conn = Utils.openConnection(feedUrl);
			} catch (SocketTimeoutException e) {
				PonyLogger.e(TAG, "Feed url timed out", e);
			}
			if (conn != null){
				conn.disconnect();
				podcast.setFeedUrl(feedUrl);
				//Check if the new url is already in the database
				boolean checkDatabase = mPonyExpressApp.getDbHelper().checkDatabaseForUrl(podcast);
				if (checkDatabase) {
					return ReturnCodes.ALREADY_DOWNLOADED;
				}else{
					aPodcastName = mPonyExpressApp.getDbHelper().addNewPodcast(podcast);
					return ReturnCodes.ALL_OK;
				}
			} else return ReturnCodes.URL_OFFLINE;

		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			mProgDialog.dismiss();

			switch (result){
			case ReturnCodes.ALREADY_DOWNLOADED:
				Toast.makeText(mPonyExpressApp, R.string.already_in_db, Toast.LENGTH_SHORT).show();
				break;
			case ReturnCodes.URL_OFFLINE:
				Toast.makeText(mPonyExpressApp, R.string.url_error, Toast.LENGTH_SHORT).show();
				break;
			case ReturnCodes.ALL_OK:
				//Send podcast name back to PonyExpressActivity so it can update the new feed.
				sendToMainActivity(aPodcastName);
			}
		}	

	}

	private class Backup extends AsyncTask<File,Integer,Integer>{

		/*
		 * This is carried out in the UI thread before the background tasks are started.
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgDialog = ProgressDialogFragment.newInstance(getText(R.string.backing_up_feeds));
			mProgDialog.show(getFragmentManager(), "backup prog dialog");
		}

		@Override
		protected Integer doInBackground(File... f){
			List<String> podcastlist = mPonyExpressApp.getDbHelper().getAllPodcastsUrls();
			final BackupFileWriter backupwriter = new BackupFileWriter();
			return backupwriter.writeBackupOpml(podcastlist,f[0]);
		}

		protected void onPostExecute(Integer return_code) {
			mProgDialog.dismiss();
			//Handle the return codes.
			switch (return_code) {
			case ReturnCodes.ALL_OK:
				Toast.makeText(mPonyExpressApp,
						R.string.backup_successful, Toast.LENGTH_LONG).show();
				PonyLogger.d(TAG, "Backup finished...");
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
			mProgDialog = ProgressDialogFragment.newInstance(getText(R.string.restoring));
			mProgDialog.show(getFragmentManager(),"restore dialog");

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
			mProgDialog.dismiss();
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
				PonyLogger.d(TAG , "Restore finished...");
				sendToMainActivity(PonyExpressFragment.UPDATE_ALL);
			}
		}
	}
	/**
	 * This method sends a string back to the main activity to update the new podcast
	 * and goes back from the Add_new fragment/activity.
	 * @param update_code the name of a podcast to update or UPDATE_ALL
	 */
	private void sendToMainActivity(String update_code){
		
		Intent intent = new Intent(mPonyExpressApp, PonyExpressFragsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(PodcastKeys.NAME, update_code);
		startActivity(intent);
		//Pop off the add_new fragment if it is in two pane mode(ponyepress_frag is also present).
		if (getActivity().getSupportFragmentManager().findFragmentById(R.id.ponyexpress_fragment)!= null){
			getActivity().getSupportFragmentManager().popBackStackImmediate();
		}
	}


	public void startSearch(String query) {
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
				SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
		suggestions.saveRecentQuery(query, null);
		final Intent searchIntent = new Intent(mPonyExpressApp, MiroActivity.class);
		searchIntent.setAction(Intent.ACTION_SEARCH);
		searchIntent.putExtra(SearchManager.QUERY, query);
		startActivityForResult(searchIntent, ADD_FEED);
	}
	
	static public class ClearSearchDialog extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.clear_history_dialog_message)
		       .setTitle(R.string.clear_history_dialog_title);
			// Add the buttons
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			               // User clicked OK button
			        	   SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
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
	}
}
