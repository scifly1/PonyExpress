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
import org.sixgun.ponyexpress.R;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class PlaylistActivity extends PonyExpressActivity {

	private ListView mPlaylist;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playlist);
		
		//Get the playlist listview as we need to manage it.
		mPlaylist = (ListView) findViewById(R.id.playlist_list);
		
		listPlaylist();
	}
	
	
	/**
	 * Return to the standard view from the Playlist view
	 * @param v
	 */
	public void goBack(View v) {
		finish();
	}
		
	/**
	 * This method lists the podcasts currently in the playlist.
	 */
	private void listPlaylist() {
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
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//TODO
		return false;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//TODO
		return true;
	}
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		//TODO uncomment call to super when this is implemented
		//super.onCreateContextMenu(menu, v, menuInfo);
		
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//TODO
		return false;
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
			
			//TODO add long click listener to the row.
		
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			v = vi.inflate(R.layout.episode_row_playlist, parent, false);
			return v;
		}
		
	}
	
	/**
	 * Starts EpisdodeTabs with the Player etc.. with a playlist
	 * @param v
	 */
	public void startPlaylist(View v) {
		//TODO stub
	}
	

}
