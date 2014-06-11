/*
 * Copyright 2014 Paul Elms
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

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;


public class PlaylistPodcastListFragment extends Fragment {

	private PonyExpressApp mPonyExpressApp;
	private TextView mPlaylistSubtitle;
	private ListView mPodcastsList;
	private String mPodcastName;
	private PlaylistEpisodeListFrag mEpsFrag;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mPonyExpressApp = (PonyExpressApp) getActivity().getApplication();
		mPlaylistSubtitle = (TextView) getActivity().findViewById(R.id.playlist_subtitle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.podcast_and_episode_lists, container, false);
		mPodcastsList = (ListView) v.findViewById(R.id.podcasts_episodes_list);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		listPodcasts();
	}

	protected void listPodcasts() {
		Cursor c = mPonyExpressApp.getDbHelper().getAllPodcastNamesAndArt();
		getActivity().startManagingCursor(c);
		//Create a CursorAdapter to map podcast title and art to the ListView.
		PlaylistPodcastCursorAdapter adapter = new PlaylistPodcastCursorAdapter(mPonyExpressApp, c);
		
		mPodcastsList.setAdapter(adapter);
		mPlaylistSubtitle.setText(R.string.playlist_subtitle_pod);
	}

	private void selectPodcast(long id) {
		//Get the podcast name and album art url.
		mPodcastName = mPonyExpressApp.getDbHelper().getPodcastName(id);

		//Change fragment to episode list fragment. 
		mEpsFrag = (PlaylistEpisodeListFrag)
                getFragmentManager().findFragmentByTag("eps_list");
        if (mEpsFrag == null) {
            // Make new fragment to show this selection.
            mEpsFrag = new PlaylistEpisodeListFrag();
            Bundle args = new Bundle();
            args.putString(PodcastKeys.NAME, mPodcastName);
            mEpsFrag.setArguments(args);
            // Execute a transaction, replacing this fragment
            // with the episodes one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(((View) getView().getParent()).getId(), mEpsFrag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack("eps_list");
            ft.commit();
        }
		
		
	}
	
	private class PlaylistPodcastCursorAdapter extends PodcastCursorAdapter{

		public PlaylistPodcastCursorAdapter(Context context, Cursor c) {
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
			
		}

		/* (non-Javadoc)
		 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity.PodcastCursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
		 */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater vi = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			v = vi.inflate(R.layout.playlist_podcast_row, parent, false);
			return v;
		}
	}
}
