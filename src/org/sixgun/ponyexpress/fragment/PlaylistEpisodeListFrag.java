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

import org.sixgun.ponyexpress.EpisodeCursorAdapter;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class PlaylistEpisodeListFrag extends Fragment {

	private ListView mEpisodesList;
	private PonyExpressApp mPonyExpressApp;
	private String mPodcastName;
	private TextView mPlaylistSubtitle;
	private View mBackground;
	private String mAlbumArtUrl;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		listEpisodes();
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mPonyExpressApp = (PonyExpressApp) getActivity().getApplication();
		mPlaylistSubtitle = (TextView) getActivity().findViewById(R.id.playlist_subtitle);
		mPodcastName = getArguments().getString(PodcastKeys.NAME);
		mAlbumArtUrl = mPonyExpressApp.getDbHelper().getAlbumArtUrl(mPodcastName);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.podcast_and_episode_lists, container, false);
		mEpisodesList = (ListView) v.findViewById(R.id.podcasts_episodes_list);
		mBackground = v.findViewById(R.id.playlist_episodes_body);
		mBackground.getViewTreeObserver().addOnGlobalLayoutListener(background);

		return v;
	}

	OnGlobalLayoutListener background = new OnGlobalLayoutListener() {
		
		@Override
		public void onGlobalLayout() {
			if (mAlbumArtUrl != null){
				Resources res = getResources();
				int new_height = mBackground.getHeight();
				int new_width = mBackground.getWidth();
				BitmapDrawable new_background = PonyExpressApp.
						sBitmapManager.createBackgroundFromAlbumArt(res, mAlbumArtUrl, new_height, new_width);
				mBackground.setBackgroundDrawable(new_background);
				mBackground.getViewTreeObserver().removeGlobalOnLayoutListener(background);
			}
			
		}
	}; 
	
	private void listEpisodes() {
		Cursor c = mPonyExpressApp.getDbHelper().getAllEpisodeNamesDescriptionsAndLinks(mPodcastName);
		getActivity().startManagingCursor(c);		

		EpisodeCursorAdapter episodes = new PlaylistEpisodeCursorAdapter(mPonyExpressApp, c);
		mEpisodesList.setAdapter(episodes);
		registerForContextMenu(mEpisodesList);
		mPlaylistSubtitle.setText(R.string.playlist_subtitle_eps);

	}
	
	private void selectEpisode(long id){
		//TODO
	}
	
	private class PlaylistEpisodeCursorAdapter extends EpisodeCursorAdapter {

		public PlaylistEpisodeCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			//Call super method to set out layout
			super.bindView(view, context, cursor);
			
			//Add Click listener's for each row.
			final int id_index = cursor.getColumnIndex(EpisodeKeys._ID);
			final long id = cursor.getLong(id_index);
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//Check if a Youtube video or regular episode
					if (v.getTag().equals(EpisodeCursorAdapter.REGULAR_EPISODE)){
						selectEpisode(id);
					} else {
						Toast.makeText(mPonyExpressApp, R.string.youtube_to_playlist, Toast.LENGTH_SHORT).show();
					}
				}
			});
			
			view.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					getActivity().openContextMenu(v);
					return true;
				}
			});
		}
		
		
	}
}
