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
package org.sixgun.ponyexpress.activity;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.fragment.EpisodeFrag;
import org.sixgun.ponyexpress.fragment.ShowNotesFrag;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.widget.TabHost;
import android.widget.TextView;


public class EpisodeTabsFragActivity extends FragmentActivity {

	/**
	 * Shows the Player/Downloader and the Show notes in tabs on small screens
	 * or side by side on wide screens.
	 */
	public static final String TAG = "EpisodeTabsFragActivity";
	private FragmentTabHost mTabHost;
	private CharSequence mTitleText;
	private Resources mRes;
	private boolean mPlayingPlaylist;
	private String mPodcastName;
	private PonyExpressApp mPonyExpressApp;
	private long mEpisodeId;
	

			@Override
			protected void onCreate(Bundle arg0) {
				super.onCreate(arg0);
				
				setContentView(R.layout.episode_tabs);
				mPonyExpressApp = (PonyExpressApp) getApplication();
				Intent data = getIntent();
				Bundle bundle = new Bundle();
				if (data.getExtras().getBoolean(PodcastKeys.PLAYLIST)){
					mPlayingPlaylist = true;
					//get first episode from playlist
					mPodcastName = mPonyExpressApp.getDbHelper().getPodcastFromPlaylist();
					mEpisodeId = mPonyExpressApp.getDbHelper().getEpisodeFromPlaylist();
					//TODO Check an episode has been returned, if db corrupted it will not be.
					bundle = Episode.packageEpisode(mPonyExpressApp, mPodcastName, mEpisodeId);
					bundle.putBoolean(PodcastKeys.PLAYLIST, true);
					data.replaceExtras(bundle);
				} else {
					bundle = data.getExtras();
					bundle.putBoolean(PodcastKeys.PLAYLIST, false);
				}
				mRes = getResources(); // Resource object to get Drawables
				
				mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
		        mTabHost.setup(this, getSupportFragmentManager(), R.id.realTabContent);
		        TextView title = (TextView) findViewById(R.id.TitleText);
				mTitleText = bundle.getString(EpisodeKeys.TITLE);
				title.setText(mTitleText);
				
				TabHost.TabSpec spec;  // Resusable TabSpec for each tab			    
			   
			    spec = mTabHost.newTabSpec("episode").setIndicator
			    (mRes.getText(R.string.play),mRes.getDrawable(R.drawable.ic_tab_play));
			    mTabHost.addTab(spec,EpisodeFrag.class,null);
			    
			  //Add Episode Notes Activity
			    spec = mTabHost.newTabSpec("notes").setIndicator
			    (mRes.getText(R.string.show_notes),mRes.getDrawable(R.drawable.ic_tab_notes));
			    mTabHost.addTab(spec, ShowNotesFrag.class,null);
			}

			
			
}
