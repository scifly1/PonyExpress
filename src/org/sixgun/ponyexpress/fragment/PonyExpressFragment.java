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


import org.sixgun.ponyexpress.PodcastCursorAdapter;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.AddNewPodcastFragActivity;
import org.sixgun.ponyexpress.activity.EpisodesFragActivity;
import org.sixgun.ponyexpress.activity.PlaylistActivity;
import org.sixgun.ponyexpress.activity.PreferencesActivity;
import org.sixgun.ponyexpress.service.UpdaterService;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class PonyExpressFragment extends ListFragment implements OnClickListener{

	private static final String TAG = "PonyExpressFragment";
	
	//Update codes
	public static final String UPDATE_SIXGUN_SHOW_LIST = "Update_Sixgun";
	public static final String UPDATE_ALL = "Update_all";
	public static final String UPDATE_SINGLE = "Update_single";
	public static final String SET_ALARM_ONLY = "Set_alarm_only";
	public static final int ADD_FEED = 0;
	
	private PonyExpressApp mPonyExpressApp;
	private boolean mListingPodcasts;
	private int mListSize;
	private ViewGroup mListFooter;
	private boolean mDualPane;
	private int mCurrentPodcastPosition;
	private EpisodesFragment mEpisodes;
	private ProgressDialogFragment mProgDialog;

	private BroadcastReceiver mPodcastDeletedReceiver;

	private ViewGroup mFooter_layout;

	private AddNewPodcastsFragment mAddNew;

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		mProgDialog = new ProgressDialogFragment();
		mPodcastDeletedReceiver = new PodcastDeleted();
		mPonyExpressApp = (PonyExpressApp) getActivity().getApplication();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.main, container, false);
		
		mListFooter = (ViewGroup) inflater.inflate(R.layout.main_footer, null);
		mFooter_layout = (ViewGroup) v.findViewById(R.id.footer_layout);
		ViewGroup list_root = (ViewGroup) v.findViewById(R.id.podcast_list_root);
		list_root.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		
			@Override
			public void onGlobalLayout() {
				//Determine if we need to add a footer and hide the base footer.
				if (mListingPodcasts){ //onGlobalLayout is called frequently, we only want this to run
					//when listing the podcasts.
					ListView list = getListView();
					int last_pos_visible = list.getLastVisiblePosition();
					if (last_pos_visible == -1){
						PonyLogger.w(TAG, "We should not be here!!");
						return; //we are being called when exiting activity which should not happen as mListingPodcasts should be false.
					}
					
					if (last_pos_visible < mListSize - 1 && mFooter_layout.getVisibility() == View.VISIBLE && list.getFooterViewsCount() < 1){
						//The last position is not visible and we don't have a footer already
						//so add footer to list and hide the 'other' footer.
						listPodcasts(true);
					} else if (last_pos_visible == mListSize && mFooter_layout.getVisibility() == View.GONE){
						//last Position is visible so remove footer if present.
						list.removeFooterView(mListFooter);
						mFooter_layout.setVisibility(View.VISIBLE);
					}
					mListingPodcasts = false;
				}			
			}
		});
		
		//Set up click listeners
		Button footer_button_list = ((Button)mListFooter.findViewById(R.id.footer_button));
		footer_button_list.setOnClickListener(this);
		Button footer_button_layout = ((Button)mFooter_layout.findViewById(R.id.footer_button));
		footer_button_layout.setOnClickListener(this);
		TextView app_name =  (TextView) v.findViewById(R.id.app_name);
		if (app_name != null){//not present on android > 3
			app_name.setOnClickListener(this);
		}
		TextView subtitle = (TextView) v.findViewById(R.id.sixgun_subtitle);
		subtitle.setOnClickListener(this);
		ImageButton add_feeds_button = (ImageButton) v.findViewById(R.id.add_feeds_button);
		if (add_feeds_button != null){ //not present on android > 3
			add_feeds_button.setOnClickListener(this);
		}
		ImageButton playlist_button = (ImageButton) v.findViewById(R.id.playlist_button);
		if (playlist_button != null){ //not present on android > 3
			playlist_button.setOnClickListener(this);
		}
		
		return v;
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.footer_button:
			//fallthrough
		case R.id.app_name:
			//fallthrough
		case R.id.sixgun_subtitle:
			showAbout(v);
			break;
		case R.id.add_feeds_button:
			addPodcast(v,"");
			break;
		case R.id.playlist_button:
			showPlaylist(v);
			break;
		default:
			break;
		}
		
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		listPodcasts(false);
		
		// Check to see if we have a frame in which to embed the EpisodesFragment
        // directly in the containing UI.
        View secondFrame = getActivity().findViewById(R.id.second_pane);
        mDualPane = secondFrame != null && secondFrame.getVisibility() == View.VISIBLE;

        if (savedInstanceState != null) {
            // Restore last state for checked podcast.
            mCurrentPodcastPosition = savedInstanceState.getInt("curPodcast", 
            		(int) getListView().getItemIdAtPosition(0));
        }else {
        	mCurrentPodcastPosition = (int) getListView().getItemIdAtPosition(0);
        }
        
        if (mDualPane) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            // Make sure our UI is in the correct state.
            selectPodcast(mCurrentPodcastPosition, mCurrentPodcastPosition);
        }
	}
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("curPodcast", mCurrentPodcastPosition);

        super.onSaveInstanceState(outState);
    }
	
	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter("org.sixgun.ponyexpress.PODCAST_DELETED");
		getActivity().getApplicationContext().registerReceiver(mPodcastDeletedReceiver, filter);
		
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {
		super.onPause();
		getActivity().getApplicationContext().unregisterReceiver(mPodcastDeletedReceiver);
	}

	/**
	 * This method lists the podcasts found in the database in the ListView.
	 * @param addFooter This is a bit of a hack where we draw the list without 
	 * a footer, determine with the globalLayoutListener if we need a footer and then
	 * re-call this to add a footer to the adapter.
	 */
	private void listPodcasts(boolean addFooter) {
		Cursor c = mPonyExpressApp.getDbHelper().getAllPodcastNamesAndArt();
		getActivity().startManagingCursor(c);
		//Create a CursorAdapter to map podcast title and art to the ListView.
		PodcastCursorAdapter adapter = new PodcastCursorAdapter(mPonyExpressApp, c);
		mListingPodcasts = true;
		//Add footer to the listview if required.
		if (addFooter){
			ListView list = getListView();
			//prevent more than one footer being added each time we pass through here.
			if (list.getFooterViewsCount() == 0){
				list.addFooterView(mListFooter);
				mFooter_layout.setVisibility(View.GONE);
			}
		}
		mListSize = adapter.getCount();
		setListAdapter(adapter);
		
		registerForContextMenu(getListView());
		
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()){
		case R.id.view_eps:
			selectPodcast(info.id, info.position);
			return true;
		case R.id.refresh_feeds:
			final String podcast_name = mPonyExpressApp.getDbHelper().getPodcastName(info.id);
			updateFeed(podcast_name);
			return true;
		case R.id.remove_podcast:
			//FIXME Deletion should happen in an async task with a progress dialog, its too slow
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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);
			
			MenuInflater inflater = getActivity().getMenuInflater();
			inflater.inflate(R.menu.podcast_context, menu);
			PonyLogger.d(TAG, "Creating context menu");
			//Set the title of the menu
			AdapterView.AdapterContextMenuInfo item = (AdapterContextMenuInfo) menuInfo;
			TextView podcast_name = (TextView) item.targetView.findViewById(R.id.podcast_text);
			menu.setHeaderTitle(podcast_name.getText());
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		selectPodcast(id, position);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main_options_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.playlist_menu:
			showPlaylist(null);
			return true;
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
	    case R.id.about:
	    	showAbout(null);
	    	return true;
	    case R.id.add_sixgun:
	        updateFeed(UPDATE_SIXGUN_SHOW_LIST);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Embeds the correct EpsiodesFragment in the UI if enough room or
	 * starts the EpisodesActivity with the selected podcast.
	 * @param id row_id of the podcast in the database
	 */
	protected void selectPodcast(long id,  int position) {
		PonyLogger.d(TAG, "Selecting Podcast");
		
		mCurrentPodcastPosition = (int) id;
		final String name = mPonyExpressApp.getDbHelper().getPodcastName(id);
		final String url = mPonyExpressApp.getDbHelper().getAlbumArtUrl(id);
		
		if (mDualPane) {
            mEpisodes = (EpisodesFragment)
                    getFragmentManager().findFragmentByTag("episodes");
            if (mEpisodes == null || !mEpisodes.getShownPodcast().equals(name)) {
                // Make new fragment to show this selection.
                mEpisodes = EpisodesFragment.newInstance(name,url);

                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.second_pane, mEpisodes);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        } else {
            // Otherwise we need to launch a new activity to display
            // the episodes fragment.
            Intent intent = new Intent();
            intent.setClass(getActivity(), EpisodesFragActivity.class);
            intent.putExtra(PodcastKeys.NAME, name);
    		intent.putExtra(PodcastKeys.ALBUM_ART_URL, url);
            startActivity(intent);
        }
	}
	public void updateFeed(String podcastName){
		PonyLogger.d(TAG, "UpdateFeed called");
		if (mPonyExpressApp.isUpdaterServiceRunning() && podcastName != SET_ALARM_ONLY){
        	Toast.makeText(mPonyExpressApp, 
					R.string.please_wait, Toast.LENGTH_LONG).show();
		}else{
			boolean connectivity_required = true;
			if (podcastName == SET_ALARM_ONLY){
				connectivity_required = false;
			}
			UpdateEpisodes task = (UpdateEpisodes) new UpdateEpisodes(connectivity_required)
			.execute(podcastName);
			if (task.isCancelled()){
				PonyLogger.d(TAG, "Cancelled Update, No Connectivity");
			}
		}
	}
	
	/**
	 * Bring up the About dialog via a button click.
	 * @param v, a reference to the button that was clicked to call this.
	 */
	public void showAbout(View v){
		FragmentManager fm = getFragmentManager();
		AboutDialogFragment about = new AboutDialogFragment();
		about.show(fm, "AboutDialog");

	}
	
	/**
	 * Show the playlist
	 */
	public void showPlaylist(View v) {
		//FIXME doesn't start fragment yet
		startActivity(new Intent(mPonyExpressApp, PlaylistActivity.class));
	}
	
	/**
	 * Embeds the AddPodcastsFragment in the UI if enough room or
	 * starts the AddPodcastsActivity.
	 * @param v,url
	 */
	public void addPodcast(View v, String url) {
		PonyLogger.d(TAG, "Starting AddNewPodcasts");

		if (mDualPane) {
			if (mAddNew == null) {
				mAddNew = AddNewPodcastsFragment.newInstance(url);
			}
			// Execute a transaction, replacing any existing fragment
			// with this one inside the frame.
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.second_pane, mAddNew);
			ft.addToBackStack(null);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}else {
			// Otherwise we need to launch a new activity to display
			//AddNewPodcastsFragment
			Intent intent = new Intent(mPonyExpressApp, AddNewPodcastFragActivity.class);
			intent.putExtra(PodcastKeys.FEED_URL, url);
			startActivity(intent);
		}
	}
	/** 
	* Parse the RSS feed and update the database with new episodes in a background thread.
	* 
	*/
	private class UpdateEpisodes extends AsyncTask <String,Void,Void>{
		
		private boolean connectivity_required;
		/*
		 * This is carried out in the UI thread before the background tasks are started.
		 */
		public UpdateEpisodes(boolean connectivity_required){
			this.connectivity_required = connectivity_required; 
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (connectivity_required && !mPonyExpressApp.getInternetHelper().checkConnectivity() ){
				Toast.makeText(mPonyExpressApp, 
						R.string.no_internet_connection, Toast.LENGTH_LONG).show();
				cancel(true);
			}
			if (mPonyExpressApp.isUpdaterServiceRunning()){
				cancel(true);
			}
			if (connectivity_required){ //Set alarm only doesn't need connectivity, is quick
				// and so doesn't need progDialog.
				mProgDialog.show(getFragmentManager(), "update Progress Dialog");
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
			getActivity().startService(intent);
						
			//Pause until all UpdaterServices are done
			while (mPonyExpressApp.isUpdaterServiceRunning()){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					PonyLogger.e(TAG, "UpdateEpisodes failed to sleep");
				}
			}
			return null;
		}
		
		/* 
		 */
		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (mProgDialog.isAdded()){
				mProgDialog.dismiss();
			}
		}
		/* 
		 */
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (mProgDialog.isAdded()){
				mProgDialog.dismiss();
			}
			//re-list podcasts to update new episode counts
			listPodcasts(false);
		}
		
	};
	
	/**
	 * Receiver that takes a broadcast sent by the DbHandler when 
	 * the database has been changed by the deletion of a podcast, so the list 
	 * can be updated.
	 */
	public class PodcastDeleted extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			//need to recheck the ListSize to see if the footer needs to be moved
			listPodcasts(false);
		}
		
	}

}
