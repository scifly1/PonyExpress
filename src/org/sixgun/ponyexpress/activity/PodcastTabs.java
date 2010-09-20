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

import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.view.RemoteImageView;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;

/**
 * @author scifly
 *
 */
public class PodcastTabs extends TabActivity {
	private String mPodcastName;
	private PonyExpressApp mPonyExpressApp; 
	
	/* (non-Javadoc)
	 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.podcast_tabs);
		
		//Get the application context
		mPonyExpressApp = (PonyExpressApp)getApplication();
		
		TextView title = (TextView)findViewById(R.id.title);
		//Get Podcast name from bundle and set title text.
		Bundle data = getIntent().getExtras();
		mPodcastName = data.getString(PodcastKeys.NAME);
		title.setText(mPodcastName);
		//Get Album art url and set image.
		RemoteImageView albumArt = (RemoteImageView)findViewById(R.id.album_art);
		String albumArtUrl = getIntent().getExtras().getString(PodcastKeys.ALBUM_ART_URL);
		if (albumArtUrl!= null && !"".equals(albumArtUrl) && !"null".equalsIgnoreCase(albumArtUrl)){
    		albumArt.setRemoteURI(albumArtUrl);
    		albumArt.loadImage();
		}
		
		Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab
	    
	  //Add EpisodesActivity
	    intent = new Intent(this,EpisodesActivity.class);
	    intent.putExtra(PodcastKeys.NAME, mPodcastName);
	    spec = tabHost.newTabSpec("episodes").setIndicator("Episodes").setContent(intent);
	    tabHost.addTab(spec);
	    
	    //TODO The tabs need icons...
	    
	  //Add Identi.ca feed Activity if a tag has been set.
	    final String identicagroup = mPonyExpressApp.getDbHelper().getIdenticaGroup(mPodcastName);
	    final String identicatag = mPonyExpressApp.getDbHelper().getIdenticaTag(mPodcastName);
	    intent = new Intent(this,IdenticaActivity.class);
	    intent.putExtra(PodcastKeys.GROUP, identicagroup);
	    intent.putExtra(PodcastKeys.TAG, identicatag);
	   	spec = tabHost.newTabSpec("identica").setIndicator("Group Dents").setContent(intent);
	   	tabHost.addTab(spec);
	 	    
	    tabHost.setCurrentTab(0);
	}
}
