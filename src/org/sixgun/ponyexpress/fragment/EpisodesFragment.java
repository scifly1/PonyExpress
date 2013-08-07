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

import org.sixgun.ponyexpress.EpisodeCursorAdapter;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.util.Utils;

import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;


public class EpisodesFragment extends ListFragment {

	public static EpisodesFragment newInstance(String podcast_name, String art_url) {
        EpisodesFragment epis = new EpisodesFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putString(PodcastKeys.NAME, podcast_name);
        args.putString(PodcastKeys.ALBUM_ART_URL, art_url);
        epis.setArguments(args);

        return epis;
    }

	private PonyExpressApp mPonyExpressApp;
	private int mNumberUnlistened;
	private String mPodcastName;
	private TextView mUnlistenedText;
	private String mAlbumArtUrl;
	private ViewGroup mBackground;

	public String getShownPodcast() {
		return getArguments().getString(PodcastKeys.NAME, "");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		//Important to always return a view here, unlike what the docs say.
		//If not then other methods called later eg:onResume() may try to use 
		// the listview which won't have been created. The fragment will still
		//be recreated after oriention change even when not needed.
		View v = inflater.inflate(R.layout.episodes, container, false);
		
		TextView title = (TextView) v.findViewById(R.id.title);
		mUnlistenedText = (TextView)v.findViewById(R.id.unlistened_eps);
		
		//Set title.
		title.setText(mPodcastName);
		//Set the background
				mBackground = (ViewGroup) v.findViewById(R.id.episodes_body);
				mBackground.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					
					@Override
					public void onGlobalLayout() {
						if (!mAlbumArtUrl.equals(null) && isAdded()){
							Resources res = getResources();
							int new_height = mBackground.getHeight();
							int new_width = mBackground.getWidth();
							BitmapDrawable new_background = PonyExpressApp.
									sBitmapManager.createBackgroundFromAlbumArt
									(res, mAlbumArtUrl, new_height, new_width);
							mBackground.setBackgroundDrawable(new_background);
						}
					}
				});
		return v;
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Get Podcast name and album art url from bundle or saved instance.
		Bundle data;
		if (savedInstanceState != null){
			data = savedInstanceState;
		} else {
			data = getArguments();
		}
		mPodcastName = data.getString(PodcastKeys.NAME);
		mAlbumArtUrl = data.getString(PodcastKeys.ALBUM_ART_URL);

		mPonyExpressApp = (PonyExpressApp) getActivity().getApplication();
	}
	

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PodcastKeys.NAME, mPodcastName);
		outState.putString(PodcastKeys.ALBUM_ART_URL, mAlbumArtUrl);
	}

	/** 
	 * (Re-)list the episodes.
	 */
	@Override
	public void onResume() {
		super.onResume();
		listEpisodes();
	}

	/**
	 * Query the database for all Episode titles to populate the ListView.
	 */
	private void listEpisodes(){
		Cursor c = mPonyExpressApp.getDbHelper().getAllEpisodeNamesDescriptionsAndLinks(mPodcastName);
		getActivity().startManagingCursor(c);		
		
		EpisodeCursorAdapter episodes = new EpisodeCursorAdapter(getActivity(), c);
		setListAdapter(episodes);
		//enable the context menu
		registerForContextMenu(getListView());
		//Also update the unlistened text at the same time.
		mNumberUnlistened = mPonyExpressApp.getDbHelper().countUnlistened(mPodcastName);
		final String unListenedString = Utils.formUnlistenedString(mPonyExpressApp, mNumberUnlistened);		
		mUnlistenedText.setText(unListenedString);
		
	}
}
