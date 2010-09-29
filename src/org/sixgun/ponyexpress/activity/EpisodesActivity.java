/*
 * Copyright 2010 Paul Elms
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;


public class EpisodesActivity extends ListActivity {

	private static final String TAG = "EpisodesActivity";
	private static final int MARK_ALL_LISTENED = 0;
	private static final int MARK_ALL_NOT_LISTENED = MARK_ALL_LISTENED +1;
	private PonyExpressApp mPonyExpressApp;
	private String mPodcastName;
	private String mAlbumArtUrl; 

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.episodes);
		
		//enable the context menu
		registerForContextMenu(getListView());
		
		//Get Podcast name and album art url from bundle.
		//The album art url is not used by this activity but is passed to the player by intent
		final Bundle data = getIntent().getExtras();
		mPodcastName = data.getString(PodcastKeys.NAME);
		mAlbumArtUrl = data.getString(PodcastKeys.ALBUM_ART_URL);
		//Get the application context.
		mPonyExpressApp = (PonyExpressApp)getApplication();
	}

	/** 
	 * (Re-)list the episodes.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		listEpisodes();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Call super to create the general menu
		super.onCreateOptionsMenu(menu);
		//Add other specific menu items
		menu.add(Menu.NONE,MARK_ALL_LISTENED,Menu.NONE,R.string.mark_all_listened);
		menu.add(Menu.NONE,MARK_ALL_NOT_LISTENED,Menu.NONE,R.string.mark_all_not_listened);
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.episode_context, menu);		
	}

	/* (non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		//Get all info from database and put it in an Intent for EpisodeTabs
		final String title = mPonyExpressApp.getDbHelper().getEpisodeTitle(id, mPodcastName);
		final String description = mPonyExpressApp.getDbHelper().getDescription(id, mPodcastName);
		final String identicaTag = mPonyExpressApp.getDbHelper().getIdenticaTag(mPodcastName);
		//Seperate episode number from filename for hashtag.
		//FIXME This only works for filename with xxxxxxnn format not 
		//for others such as xxxxxxxsnnenn
		//If cannot be determined don't do ep specific dents, only group dents
		Pattern digits = Pattern.compile("[0-9]+");
		Matcher m = digits.matcher(title);
		m.find();
		String epNumber = m.group(); 
		Log.d(TAG, "Episode number: " + epNumber);

		Intent intent = new Intent(this,EpisodeTabs.class);
		intent.putExtra(PodcastKeys.NAME, mPodcastName);
		intent.putExtra(EpisodeKeys.TITLE, title);
		intent.putExtra(EpisodeKeys.DESCRIPTION, description);
		if (!identicaTag.equals("")){
			intent.putExtra(PodcastKeys.TAG, identicaTag);
		}
		intent.putExtra(EpisodeKeys.EP_NUMBER, epNumber);
		intent.putExtra(EpisodeKeys._ID, id);
		intent.putExtra(PodcastKeys.ALBUM_ART_URL, mAlbumArtUrl);
		final String filename = mPonyExpressApp.getDbHelper().getEpisodeFilename(id, mPodcastName);
		intent.putExtra(EpisodeKeys.FILENAME, filename);
		final int listened = mPonyExpressApp.getDbHelper().getListened(id, mPodcastName);
		intent.putExtra(EpisodeKeys.LISTENED, listened);
		//Determine if Episode has been downloaded and add required extras.
		final boolean downloaded = mPonyExpressApp.getDbHelper().isEpisodeDownloaded(id, mPodcastName);
		if (!downloaded){
			final String url = mPonyExpressApp.getDbHelper().getEpisodeUrl(id, mPodcastName);
			intent.putExtra(EpisodeKeys.URL, url);
			final int size = mPonyExpressApp.getDbHelper().getEpisodeSize(id, mPodcastName);
			intent.putExtra(EpisodeKeys.SIZE, size);
		}
		startActivity(intent);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//handle specific options
		switch (item.getItemId()){
		case MARK_ALL_LISTENED:
			markAllListened();
			listEpisodes();
			return true;
		case MARK_ALL_NOT_LISTENED:
			markAllNotListened();
			listEpisodes();
			return true;
		default:	
			return super.onOptionsItemSelected(item);
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()){
		case R.id.mark_listened:
			markListened(info.id);
			listEpisodes();
			return true;
		case R.id.mark_not_listened:
			markNotListened(info.id);
			listEpisodes();
			return true;
		default:
			return super.onContextItemSelected(item);
		}	
	}


	private class EpisodeAdapter extends CursorAdapter {

		public EpisodeAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final int titleIndex = cursor.getColumnIndex(EpisodeKeys.TITLE);
			final int listenedIndex = cursor.getColumnIndex(EpisodeKeys.LISTENED);
			TextView episodeText = (TextView) view.findViewById(R.id.episode_text);
			String title = cursor.getString(titleIndex);
			int listened = cursor.getInt(listenedIndex);
			episodeText.setText(title);
			if (listened == -1){ //not listened == -1
				episodeText.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
			} else episodeText.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			v = vi.inflate(R.layout.episode_row, null);
			return v;
		}
		
	}

	/**
	 * Query the database for all Episode titles to populate the ListView.
	 */
	private void listEpisodes(){
		Cursor c = mPonyExpressApp.getDbHelper().getAllEpisodeNames(mPodcastName);
		startManagingCursor(c);		
		
		EpisodeAdapter episodes = new EpisodeAdapter(this, c);
		setListAdapter(episodes);
		
	}
	
	private void markListened(Long rowID) {
		//Sets listened to 0 ie: the start of the episode
		mPonyExpressApp.getDbHelper().update(mPodcastName, rowID, 
				EpisodeKeys.LISTENED, 0);
	}
	
	private void markAllListened() {
		Cursor c = mPonyExpressApp.getDbHelper().getAllNotListened(mPodcastName);
		int columnIndex = c.getColumnIndex(EpisodeKeys._ID);
		startManagingCursor(c);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++){
			long rowID = c.getLong(columnIndex);
			markListened(rowID);
			c.moveToNext();
		}
	}
	
	private void markNotListened(long rowID) {
		//Sets listened to -1, which flags as not listened
		mPonyExpressApp.getDbHelper().update(mPodcastName, rowID, 
				EpisodeKeys.LISTENED, -1);
	}
	
	private void markAllNotListened() {
		Cursor c = mPonyExpressApp.getDbHelper().getAllListened(mPodcastName);
		int columnIndex = c.getColumnIndex(EpisodeKeys._ID);
		startManagingCursor(c);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++){
			long rowID = c.getLong(columnIndex);
			markNotListened(rowID);
			c.moveToNext();
		}
	}
	
}
