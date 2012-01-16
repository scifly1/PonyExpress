/*
 * Copyright 2012 Paul Elms
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

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PlaylistInterface;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.util.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class PlaylistEpisodesActivity extends EpisodesActivity implements PlaylistInterface{

	
	
	private ListView mPlaylist;

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playlist_episodes);
		
		//Get the playlist listview as we need to manage it.
		mPlaylist = (ListView) findViewById(R.id.playlist_list);
				
		listPlaylist();
				
		registerForContextMenu(getListView());
		
		((TextView) findViewById(R.id.playlist_subtitle)).setText(R.string.playlist_subtitle_eps);
		
		//Set the background of the episode list
		mBackground = (ViewGroup) findViewById(R.id.episode_list);
		mBackground.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				Resources res = getResources();
				Bitmap image = PonyExpressApp.sImageManager.get(mAlbumArtUrl);
				if (image != null){
					int new_height = mBackground.getHeight();
					int new_width = mBackground.getWidth();
					BitmapDrawable new_background = Utils.createBackgroundFromAlbumArt
							(res, image, new_height, new_width);
					mBackground.setBackgroundDrawable(new_background);
				}
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.Playlist#goBack(android.view.View)
	 */
	@Override
	public void goBack(View v) {
		finish();
	}
	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.Playlist#startPlaylist(android.view.View)
	 */
	@Override
	public void startPlaylist(View v) {
		//TODO Start EpisodeTabs as happens from EpisodeActivity
		// but hand over a flag to indicate to play from the playlist.
	}

	/**
	 * This method lists the podcasts currently in the playlist.
	 */
	public void listPlaylist() {
		Cursor c = mPonyExpressApp.getDbHelper().getPlaylist();
		startManagingCursor(c);
		//Create a cursor adaptor to populate the ListView
		PlaylistCursorAdapter adapter = new PlaylistCursorAdapter(mPonyExpressApp, c);
		
		mPlaylist.setAdapter(adapter);
		//register the playlist to have a context menu
		//TODO There is only one context menu per activity, so some logic
		// will be reqired to determine which list has been long pressed.
		registerForContextMenu(mPlaylist);	
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onCreateOptionsMenu(menu);
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onPrepareOptionsMenu(menu);
	}

	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return super.onOptionsItemSelected(item);
	}
	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return super.onContextItemSelected(item);
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
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
			String episode_title = "";
			if (cursor != null){
				final int columnIndex = cursor.getColumnIndex(EpisodeKeys.TITLE);
				episode_title = cursor.getString(columnIndex);
			}
			
			TextView episodeName = (TextView) view.findViewById(R.id.episode_text);
			
			if (episode_title.equals("")) {
				episodeName.setText(R.string.no_episodes);
			} else {
				episodeName.setText(episode_title);
			}
			
			//TODO add long click listener to the row so context menus work.
		
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			v = vi.inflate(R.layout.episode_row_playlist, parent, false);
			return v;
		}
		
	}
	
}
