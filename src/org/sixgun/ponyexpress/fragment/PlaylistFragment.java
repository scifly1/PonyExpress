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

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PlaylistInterface;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.util.Utils;
import org.sixgun.ponyexpress.util.Bitmap.RecyclingImageView;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class PlaylistFragment extends Fragment implements PlaylistInterface{

	
	
	private PonyExpressApp mPonyExpressApp;
	private ListView mPlaylist;
	private TextView mNoPlaylist;
	private boolean mAutoPlaylistsOn;
	private TextView mPlaylistTime;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mPonyExpressApp = (PonyExpressApp) getActivity().getApplication();
		setHasOptionsMenu(true);
		
		//TODO use this properly
		mAutoPlaylistsOn = false;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.playlist_list, container, false);
		//Get the listviews as we need to manage them.
		mPlaylist = (ListView) v.findViewById(R.id.playlist_list);
		mNoPlaylist = (TextView) v.findViewById(R.id.no_list);
		
		return v;
	}

	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mPlaylistTime = (TextView) getActivity().findViewById(R.id.playlist_time);
		listPlaylist();
	}

	@Override
	public void goBack(View v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startPlaylist(View v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void listPlaylist() {
		Cursor c = mPonyExpressApp.getDbHelper().getPlaylist();
		if (c.getCount() > 0){
			mPlaylist.setVisibility(View.VISIBLE);
			mNoPlaylist.setVisibility(View.GONE);
			
			//Show the running time of the playlist.
			mPlaylistTime.setVisibility(View.VISIBLE);
			mPlaylistTime.setText(Utils.milliToTime(mPonyExpressApp.getDbHelper().
					getPlaylistTime(),true));

			getActivity().startManagingCursor(c);
			//Create a cursor adaptor to populate the ListView
			PlaylistCursorAdapter adapter = new PlaylistCursorAdapter(mPonyExpressApp, c);

			mPlaylist.setAdapter(adapter);

			registerForContextMenu(mPlaylist);
		} else {
			mPlaylist.setVisibility(View.GONE);
			mNoPlaylist.setVisibility(View.VISIBLE);
			mPlaylistTime.setVisibility(View.GONE);
		}
		
	}

	@Override
	public void openDownloadOverview(View v) {
		// TODO Auto-generated method stub
		
	}
	
	private void openContextMenu(View v) {
		// TODO Auto-generated method stub
		
	}
	
	private void viewShowNotes(String podcast_name, long row_id){
		// TODO stub
	}

	/**
	 * We subclass CursorAdapter to handle our display the results from our Playlist cursor. 
	 * Overide newView to create/inflate a view to bind the data to.
	 * Overide bindView to determine how the data is bound to the view.
	 */
	private class PlaylistCursorAdapter extends CursorAdapter{

		public PlaylistCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			if (cursor == null){
				return;
			}
			String episode_title = "";
			int listened = -1;
			final int row_id_column_index = cursor.getColumnIndex(EpisodeKeys.ROW_ID);
			final long row_id = cursor.getLong(row_id_column_index);
			episode_title = mPonyExpressApp.getDbHelper().
					getEpisodeTitle(row_id);
			listened = mPonyExpressApp.getDbHelper().getListened(row_id);
			
			
			TextView episodeName = (TextView) view.findViewById(R.id.episode_text);
			episodeName.setText(episode_title);
			if (listened == -1){ //not listened == -1
				episodeName.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
			} else episodeName.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);

			final int podcast_name_index = cursor.getColumnIndex(PodcastKeys.NAME);
			final String podcast_name = cursor.getString(podcast_name_index);
			
			if (mAutoPlaylistsOn){
				String albumArtUrl = mPonyExpressApp.getDbHelper().getAlbumArtUrl(podcast_name);
				RecyclingImageView albumArt = (RecyclingImageView)view.findViewById(R.id.album_art);
				
				if (albumArtUrl!= null && !"".equals(albumArtUrl) && !"null".equalsIgnoreCase(albumArtUrl)){
					PonyExpressApp.sBitmapManager.loadImage(albumArtUrl, albumArt);
				}
			}
			
			view.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					viewShowNotes(podcast_name, row_id);
					
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

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			LayoutInflater vi = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			if (mAutoPlaylistsOn){
				v = vi.inflate(R.layout.episode_row_auto_playlist, parent, false);
			} else {
				v = vi.inflate(R.layout.episode_row_playlist, parent, false);
			}
			
			return v;
		}
		
	}
	
}
