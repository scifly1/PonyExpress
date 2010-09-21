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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class EpisodesActivity extends ListActivity {

	private static final String TAG = "EpisodesActivity";
	private PonyExpressApp mPonyExpressApp;
	private String mPodcastName;
	private String mAlbumArtUrl; 

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.episodes);
		
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
		//Determine if Episode has been downloaded and add required extras.
		final boolean downloaded = mPonyExpressApp.getDbHelper().isEpisodeDownloaded(id, mPodcastName);
		if (downloaded){
			intent.putExtra(PodcastKeys.ALBUM_ART_URL, mAlbumArtUrl);
			final String filename = mPonyExpressApp.getDbHelper().getEpisodeFilename(id, mPodcastName);
			intent.putExtra(EpisodeKeys.FILENAME, filename);
			final int listened = mPonyExpressApp.getDbHelper().getListened(id, mPodcastName);
			intent.putExtra(EpisodeKeys.LISTENED, listened);
		} else {
			final String url = mPonyExpressApp.getDbHelper().getEpisodeUrl(id, mPodcastName);
			intent.putExtra(EpisodeKeys.URL, url);
			final int size = mPonyExpressApp.getDbHelper().getEpisodeSize(id, mPodcastName);
			intent.putExtra(EpisodeKeys.SIZE, size);
		}
		startActivity(intent);
	}

	

	/**
	 * Query the database for all Episode titles to populate the ListView.
	 */
	private void listEpisodes(){
		Cursor c = mPonyExpressApp.getDbHelper().getAllEpisodeNames(mPodcastName);
		startManagingCursor(c);
		//Set up columns to map from, and layout to map to
		String[] from = new String[] { EpisodeKeys.TITLE };
		int[] to = new int[] { R.id.episode_text };
		
		SimpleCursorAdapter episodes = new SimpleCursorAdapter(
				this, R.layout.episode_row, c, from, to);
		setListAdapter(episodes);
		
	}
	

	
}
