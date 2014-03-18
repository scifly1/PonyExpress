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
import org.sixgun.ponyexpress.service.PodcastPlayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TabHost;
import android.widget.TextView;


/**
 * Tabbed Activity to hold the Player/Downloader and the show notes.
 *
 */
public class EpisodeTabs extends GeneralOptionsMenuActivity {

	public static final String TAG = "EpisodeTabs";
	private CharSequence mTitleText;
	private PlaybackCompleted mPlaybackCompletedReceiver;
	private String mPodcastName;
	private long mEpisodeId;
	protected PodcastPlayer mPodcastPlayer;
	private boolean mPodcastPlayerBound;
	private boolean mPlayingPlaylist = false;
	private TabHost mTabHost;
	private Resources mRes;
	
	//This is all responsible for connecting/disconnecting to the PodcastPlayer service.
		private ServiceConnection mPlayerConnection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
		        mPodcastPlayer = null;
				
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mPodcastPlayer = ((PodcastPlayer.PodcastPlayerBinder)service).getService();
				queryPlayer();
			}
		};
	
		
		protected void doBindPodcastPlayer() {
		    // Establish a connection with the service.  We use an explicit
		    // class name because we want a specific service implementation that
		    // we know will be running in our own process (and thus won't be
		    // supporting component replacement by other applications).
			
		    bindService(new Intent(this, 
		            PodcastPlayer.class), mPlayerConnection, Context.BIND_AUTO_CREATE);
		    mPodcastPlayerBound = true;
		}

		protected void doUnbindPodcastPlayer() {
		    if (mPodcastPlayerBound) {
		        // Detach our existing connection.
		        unbindService(mPlayerConnection);
		        mPodcastPlayerBound = false;
		    }
		}

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
			mPlayingPlaylist = true;
			//get first episode from playlist
			mPodcastName = mPonyExpressApp.getDbHelper().getPodcastFromPlaylist();
			mEpisodeId = mPonyExpressApp.getDbHelper().getEpisodeFromPlaylist();
			//TODO Check an episode has been returned, if db corrupted it will not be.
			bundle = Episode.packageEpisode(mPonyExpressApp, mPodcastName, mEpisodeId);
			bundle.putBoolean(PodcastKeys.PLAYLIST, true);
		} else {
			bundle = data.getExtras();
			bundle.putBoolean(PodcastKeys.PLAYLIST, false);
		}
		
		mRes = getResources(); // Resource object to get Drawables
	    mTabHost = getTabHost();  // The activity TabHost
	    populateTabs(bundle);
	    
		mPlaybackCompletedReceiver = new PlaybackCompleted();
	}

	private void populateTabs(Bundle bundle) {
		TextView title = (TextView) findViewById(R.id.TitleText);
		mTitleText = bundle.getString(EpisodeKeys.TITLE);
		title.setText(mTitleText);
		
		TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab
	    
	    intent = new Intent(this,PlayerActivity.class);
	    intent.putExtras(bundle);
	    spec = mTabHost.newTabSpec("episode").setIndicator
	    (mRes.getText(R.string.play),mRes.getDrawable(R.drawable.ic_tab_play)).setContent(intent);
	    mTabHost.addTab(spec);
	    
//	  //Add Episode Notes Activity
//	    intent = new Intent(this,EpisodeNotesActivity.class);
//	    //Pass on the Extras
//	    intent.putExtras(bundle);
//	    spec = mTabHost.newTabSpec("notes").setIndicator
//	    (mRes.getText(R.string.show_notes),mRes.getDrawable(R.drawable.ic_tab_notes)).setContent(intent);
//	    mTabHost.addTab(spec);
	    
	    mTabHost.setCurrentTab(0);
	    
	}

	/* (non-Javadoc)
	 * @see android.app.ActivityGroup#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter("org.sixgun.ponyexpress.PLAYBACK_COMPLETED");
		registerReceiver(mPlaybackCompletedReceiver, filter);
		doBindPodcastPlayer();
	}

	/* (non-Javadoc)
	 * @see android.app.ActivityGroup#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mPlaybackCompletedReceiver);
		doUnbindPodcastPlayer();
	}



	/**
	 * This broadcast receiver receives the intent sent by PodcastPlayer
	 * to signal playback of the playlist episode has completed.	 *
	 */
	public class PlaybackCompleted extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			EpisodeTabs.this.setResult(RESULT_OK);
			SharedPreferences prefs = getSharedPreferences(PodcastKeys.PLAYLIST, 0);
			final SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PodcastKeys.PLAYLIST, true);
			editor.commit();
			finish();
		}
		
	}

	protected void queryPlayer() {
		if (mPodcastPlayer != null && mPlayingPlaylist){
			final String podcast_name = mPodcastPlayer.getPodcastName();
			final long episode_id = mPodcastPlayer.getEpisodeRow();
			
			if (podcast_name != null) {
				if (mPodcastName.equals(podcast_name)
						&& mEpisodeId == episode_id) {
					return;
				} else {
					//This is not a great way to do this, but it works.
					//Refresh the EpisodeTabs
					EpisodeTabs.this.setResult(RESULT_OK);
					finish();
				}
			}
		}
	}
}
