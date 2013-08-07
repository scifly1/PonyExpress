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
import org.sixgun.ponyexpress.activity.EpisodesActivity;
import org.sixgun.ponyexpress.activity.PonyExpressFragsActivity;
import org.sixgun.ponyexpress.activity.PreferencesActivity;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class PonyExpressFragment extends ListFragment {

	private static final String TAG = "PonyExpressFragment";
	private PonyExpressApp mPonyExpressApp;
	private boolean mListingPodcasts;
	private int mListSize;
	private ViewGroup mListFooter;
	private boolean mDualPane;
	private int mCurrentPodcastPosition;
	private EpisodesFragment mEpisodes;

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.main, container, false);
		
		mListFooter = (ViewGroup) inflater.inflate(R.layout.main_footer, null);
		
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
					ViewGroup footer_layout = (ViewGroup) v.findViewById(R.id.footer_layout);
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
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mPonyExpressApp = (PonyExpressApp) getActivity().getApplication();
		listPodcasts(false);
		
		// Check to see if we have a frame in which to embed the EpisodesFragment
        // directly in the containing UI.
        View episodesFrame = getActivity().findViewById(R.id.episodes);
        mDualPane = episodesFrame != null && episodesFrame.getVisibility() == View.VISIBLE;

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

	/**
	 * This method lists the podcasts found in the database in the ListView.
	 * @param addFooter This is a bit of a hack where we draw the list without 
	 * a footer, determine with the globalLayoutListener if we need a footer and then
	 * re-call this to add a footer to the adapter.
	 */
	protected void listPodcasts(boolean addFooter) {
		Cursor c = mPonyExpressApp.getDbHelper().getAllPodcastNamesAndArt();
		getActivity().startManagingCursor(c);
		//Create a CursorAdapter to map podcast title and art to the ListView.
		PodcastCursorAdapter adapter = new PodcastCursorAdapter(mPonyExpressApp, c);
		mListingPodcasts = true;
		//Add footer to the listview if required.
		if (addFooter){
			ViewGroup footer_layout = (ViewGroup) getActivity().findViewById(R.id.footer_layout);
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
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()){
		case R.id.view_eps:
			selectPodcast(info.id, info.position);
			return true;
			//FIXME
		case R.id.refresh_feeds:
			final String podcast_name = mPonyExpressApp.getDbHelper().getPodcastName(info.id);
//			updateFeed(podcast_name);
			return true;
		case R.id.remove_podcast:
			boolean deleted = mPonyExpressApp.getDbHelper().removePodcast(info.id);
			if(!deleted) {
				Toast.makeText(mPonyExpressApp, R.string.delete_failed, Toast.LENGTH_SHORT)
					.show();
			}
			listPodcasts(false);
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
		//FIXME
		case R.id.playlist_menu:
			//showPlaylist(null);
			return true;
	    case R.id.update_feeds:
	        //updateFeed(UPDATE_ALL);
	        return true;
	    case R.id.settings_menu:
	    	startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
	    	return true;
	    case R.id.add_podcast:
	    	//addPodcast(null, "");
	    	return true;
	    case R.id.about:
	    	getActivity().showDialog(PonyExpressFragsActivity.ABOUT_DIALOG);
	    	return true;
	    case R.id.add_sixgun:
	        //updateFeed(UPDATE_SIXGUN_SHOW_LIST);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Embeds the correct EpsiodesFragment in the UI is enough room or
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
                    getFragmentManager().findFragmentById(R.id.episodes);
            if (mEpisodes == null || !mEpisodes.getShownPodcast().equals(name)) {
                // Make new fragment to show this selection.
                mEpisodes = EpisodesFragment.newInstance(name,url);

                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.episodes, mEpisodes);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        } else {
            // Otherwise we need to launch a new activity to display
            // the episodes fragment.
            Intent intent = new Intent();
            //FIXME doesn't use the fragment
            intent.setClass(getActivity(), EpisodesActivity.class);
            intent.putExtra(PodcastKeys.NAME, name);
    		intent.putExtra(PodcastKeys.ALBUM_ART_URL, url);
            startActivity(intent);
        }
	}
	
	
}
