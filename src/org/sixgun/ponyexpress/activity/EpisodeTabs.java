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

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.R;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;

/**
 * Tabbed Activity to hold the applicable IdenticaActivity (PlayerActivity or DownloadActivity),
 * the Identi.ca stream and commenter.
 *
 */
public class EpisodeTabs extends GeneralOptionsMenuActivity {

	private CharSequence mTitleText;

	/* (non-Javadoc)
	 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.episode_tabs);
		
		Intent data = getIntent();
		Bundle bundle = new Bundle();
		if (data.getExtras().getBoolean(PodcastKeys.PLAYLIST)){
			//get first episode from playlist
			final String podcast_name = mPonyExpressApp.getDbHelper().getPodcastFromPlaylist();
			final long episode_id = mPonyExpressApp.getDbHelper().getEpisodeFromPlaylist();
			bundle = Episode.packageEpisode(mPonyExpressApp, podcast_name, episode_id);
		} else {
			bundle = data.getExtras(); 
		}
		
		TextView title = (TextView) findViewById(R.id.TitleText);
		mTitleText = bundle.getString(EpisodeKeys.TITLE);
		title.setText(mTitleText);
		
		Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab
	    
	    
	    intent = new Intent(this,PlayerActivity.class);
	    intent.putExtras(bundle);
	    spec = tabHost.newTabSpec("episode").setIndicator
	    (res.getText(R.string.play),res.getDrawable(R.drawable.ic_tab_play)).setContent(intent);
	    tabHost.addTab(spec);
	    
	  //Add Episode Notes Activity
	    intent = new Intent(this,EpisodeNotesActivity.class);
	    //Pass on the Extras
	    intent.putExtras(bundle);
	    spec = tabHost.newTabSpec("notes").setIndicator
	    (res.getText(R.string.show_notes),res.getDrawable(R.drawable.ic_tab_notes)).setContent(intent);
	    tabHost.addTab(spec);
	    
	    //Add Identi.ca feed Activity if a tag has been set.
	    if (bundle.containsKey(PodcastKeys.TAG)){
	    	intent = new Intent(this,IdenticaEpisodeActivity.class);
	    	intent.putExtras(bundle);
	    	
	    	spec = tabHost.newTabSpec("identica").setIndicator
	    	(res.getText(R.string.comment),res.getDrawable(R.drawable.ic_tab_dent)).setContent(intent);
	    	tabHost.addTab(spec);
	    }
	    
	    tabHost.setCurrentTab(0);
	    
		
	}
	
}
