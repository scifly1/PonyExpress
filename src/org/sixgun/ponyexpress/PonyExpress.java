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
package org.sixgun.ponyexpress;

import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

/*
 * Launch Activity for PonyExpress.  In a VERY DEBUG state at present.
 */
public class PonyExpress extends ListActivity {

	
	private PonyExpressDbAdaptor mDbHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//Create/open database
		mDbHelper = new PonyExpressDbAdaptor(this);
		mDbHelper.open();
		//Hook up reload button with updateEpisodes()
		ImageView reload_button =  (ImageView)findViewById(R.id.view_refresh);
		reload_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				updateEpisodes();
				listEpisodes();
			}
		});
		
	}

	/** 
	 * (Re-)list the episodes.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		listEpisodes();
	}

	/**
	 *  Close the database when activity destroyed.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
	}

	/**
	 * Parse the RSS feed and update the database with new episodes.
	 */
	private void updateEpisodes(){
		//TODO Get a service to do this, so it doesn't hold us up to much.
		String feed = "http://feeds.feedburner.com/linuxoutlaws-ogg";
		
		SaxFeedParser parser = new SaxFeedParser(this,feed);
		List<Episode> Episodes = parser.parse();
		
		Date date = mDbHelper.getLatestEpisodeDate();
				
		for (Episode episode: Episodes){
			if (episode.getDate().compareTo(date) > 0) {
				mDbHelper.insertEpisode(episode);
			}	
		}
	}
	
	/**
	 * Query the database for all Episode titles to populate the ListView.
	 */
	private void listEpisodes(){
		Cursor c = mDbHelper.getAllEpisodeNames();
		startManagingCursor(c);
		//Set up columns to map from, and layout to map to
		String[] from = new String[] { EpisodeKeys.TITLE };
		int[] to = new int[] { R.id.episode_text };
		
		SimpleCursorAdapter episodes = new SimpleCursorAdapter(
				this, R.layout.episode_row, c, from, to);
		setListAdapter(episodes);
		
	}
	
}
