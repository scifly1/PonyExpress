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
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.PonyExpressActivity.PodcastCursorAdapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
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
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;


public class PlaylistActivity extends PonyExpressActivity implements PlaylistInterface {

	private static final int START_PLAYBACK = 0;
	private ListView mPlaylist;
	private View mNoPlaylist;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playlist);
		
		//Get the playlist listview as we need to manage it.
		mPlaylist = (ListView) findViewById(R.id.playlist_list);
		mNoPlaylist = (TextView) findViewById(R.id.no_list);
		listPlaylist();
	}
		
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		//Re-list playlist to ensure it is updated if it was empty before 
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
	public void listPlaylist() {
		Cursor c = mPonyExpressApp.getDbHelper().getPlaylist();
		if (c.getCount() > 0){
			mPlaylist.setVisibility(View.VISIBLE);
			mNoPlaylist.setVisibility(View.GONE);
			startManagingCursor(c);
			//Create a cursor adaptor to populate the ListView
			PlaylistCursorAdapter adapter = new PlaylistCursorAdapter(mPonyExpressApp, c);

			mPlaylist.setAdapter(adapter);

			registerForContextMenu(mPlaylist);
		} else {
			mPlaylist.setVisibility(View.GONE);
			mNoPlaylist.setVisibility(View.VISIBLE);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity#listPodcasts(boolean)
	 */
	@Override
	protected void listPodcasts(boolean addFooter) {
		Cursor c = mPonyExpressApp.getDbHelper().getAllPodcastNamesAndArt();
		startManagingCursor(c);
		//Create a CursorAdapter to map podcast title and art to the ListView.
		PlaylistPodcastCursorAdapter adapter = new PlaylistPodcastCursorAdapter(mPonyExpressApp, c);
		
		setListAdapter(adapter);
		
		registerForContextMenu(getListView());
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//TODO Add more entries.
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.playlist_options_menu, menu);
	    return true;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
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
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
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
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
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
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()){
		case R.id.move_top:
			mPonyExpressApp.getDbHelper().moveToTop(info.position);
			listPlaylist();
			return true;
		case R.id.move_bottom:
			mPonyExpressApp.getDbHelper().moveToBottom(info.position);
			listPlaylist();
			return true;
		case R.id.remove_from_playlist:
			mPonyExpressApp.getDbHelper().moveToTop(info.position);
			mPonyExpressApp.getDbHelper().popPlaylist();
			listPlaylist();
			return true;
		case R.id.move_up:
			mPonyExpressApp.getDbHelper().moveUpPlaylist(info.position);
			listPlaylist();
			return true;
		case R.id.move_down:
			mPonyExpressApp.getDbHelper().moveDownPlaylist(info.position);
			listPlaylist();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
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
			int listened = -1;
			if (cursor != null){
				final int row_id_column_index = cursor.getColumnIndex(EpisodeKeys.ROW_ID);
				final int podcast_name_column_index = cursor.getColumnIndex(PodcastKeys.NAME);
				final long row_id = cursor.getLong(row_id_column_index);
				final String podcast_name = cursor.getString(podcast_name_column_index);
				episode_title = mPonyExpressApp.getDbHelper().
						getEpisodeTitle(row_id, podcast_name);
				listened = mPonyExpressApp.getDbHelper().getListened(row_id, podcast_name);
			}
			
			TextView episodeName = (TextView) view.findViewById(R.id.episode_text);
			episodeName.setText(episode_title);
			if (listened == -1){ //not listened == -1
				episodeName.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
			} else episodeName.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);

			
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//TODO handle clicks on the playlist item
					
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

			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			v = vi.inflate(R.layout.episode_row_playlist, parent, false);
			return v;
		}
		
	}
	
	private class PlaylistPodcastCursorAdapter extends PodcastCursorAdapter{

		public PlaylistPodcastCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}

		/* (non-Javadoc)
		 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity.PodcastCursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
		 */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			v = vi.inflate(R.layout.playlist_podcast_row, parent, false);
			return v;
		}
	}
	
	
	/* 
	 * Starts the PlaylistEpisodesActivity to select the required episodes
	 * @param id row_id of the podcast in the database
	 */
	@Override
	protected void selectPodcast(View v, long id) {
		//Get the podcast name and album art url and number of unlistened episodes.
		final String name = mPonyExpressApp.getDbHelper().getPodcastName(id);
		final String url = mPonyExpressApp.getDbHelper().getAlbumArtUrl(id);
		//Store in an intent and send to PlaylistEpisodesActivity
		Intent intent = new Intent(this,PlaylistEpisodesActivity.class);
		intent.putExtra(PodcastKeys.NAME, name);
		intent.putExtra(PodcastKeys.ALBUM_ART_URL, url);
		startActivity(intent);
	}


	/**
	 * Starts EpisdodeTabs with the Player etc.. with a playlist
	 * @param v
	 */
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


	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == START_PLAYBACK){
			if (resultCode == RESULT_OK) {
				//Playback completed get next episode
				startPlaylist(null);
			}
		}
	}
	

}