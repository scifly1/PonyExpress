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
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;


public class PlaylistEpisodesActivity extends EpisodesActivity implements PlaylistInterface{

	
	private static final int START_PLAYBACK = 0;
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
		if (!mPonyExpressApp.getDbHelper().playlistEmpty()){
			//TODO Check if all episodes in list are downloaded.

			//Start EpisodeTabs as happens from EpisodeActivity
			// but hand over a flag to indicate to play from the playlist.
			Intent intent = new Intent(this,EpisodeTabs.class);
			intent.putExtra(PodcastKeys.PLAYLIST, true);
			startActivityForResult(intent, START_PLAYBACK);
		}
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
		//TODO Add more entries.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.playlist_options_menu, menu);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (mPonyExpressApp.getDbHelper().playlistEmpty()){
			menu.removeItem(R.id.clear_playlist);
		}
		return true;
	}

	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_menu:
	    	startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
	        return true;
		case R.id.clear_playlist:
			mPonyExpressApp.getDbHelper().clearPlaylist();
			listPlaylist();
			return true;
		default: 
			return super.onOptionsItemSelected(item);
		}
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.playlist_list){
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.playlist_context, menu);
			
			//Set the title of the menu
			AdapterView.AdapterContextMenuInfo item = (AdapterContextMenuInfo) menuInfo;
			TextView episode_name = (TextView) item.targetView.findViewById(R.id.episode_text);
			menu.setHeaderTitle(episode_name.getText());
		}else{
			super.onCreateContextMenu(menu, v, menuInfo);
		}		
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		//Log.e("TAG",Integer.toString(item.getItemId()));
		switch (item.getItemId()){
		case R.id.move_top:
			mPonyExpressApp.getDbHelper().moveToTop(info.position);
			listPlaylist();
			return true;
		case R.id.move_bottom:
			mPonyExpressApp.getDbHelper().moveToBottom(info.position);
			listPlaylist();
		//TODO move_up, move_down, and remove
		}
		return true;
		//return super.onContextItemSelected(item);
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		//This detects clicks on the ListActivity list which is the episode list.
		mPonyExpressApp.getDbHelper().addEpisodeToPlaylist(mPodcastName, id);			
		listPlaylist();
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
				final int row_id_column_index = cursor.getColumnIndex(EpisodeKeys.ROW_ID);
				final int podcast_name_column_index = cursor.getColumnIndex(PodcastKeys.NAME);
				final long row_id = cursor.getLong(row_id_column_index);
				final String podcast_name = cursor.getString(podcast_name_column_index);
				episode_title = mPonyExpressApp.getDbHelper().
						getEpisodeTitle(row_id, podcast_name);
			}
			
			TextView episodeName = (TextView) view.findViewById(R.id.episode_text);
			
			if (episode_title.equals("")) {
				episodeName.setText(R.string.no_episodes);
			} else {
				episodeName.setText(episode_title);
			}
			
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//TODO handle clicks on the playlist item
					
				}
			});
			
			//TODO add long click listener to the row so context menus work.
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

			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			v = vi.inflate(R.layout.episode_row_playlist, parent, false);
			return v;
		}
		
	}
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == START_PLAYBACK){
			if (resultCode == RESULT_OK) {
				//Playback completed
				//Pop top episode off list.
				mPonyExpressApp.getDbHelper().popPlaylist();
				
				startPlaylist(null);
			}
		}
	}
	
}
