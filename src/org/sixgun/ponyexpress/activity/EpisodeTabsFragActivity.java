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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.LocalBroadcastManager;
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
	private String mPodcastName;
	private PonyExpressApp mPonyExpressApp;
	private long mEpisodeId;
	private TextView mTitleView;
	private boolean mPlayingPlaylist;
	private PodcastPlayer mPodcastPlayer;
	private BroadcastReceiver mPlaybackCompletedReceiver;
	private boolean mPodcastPlayerBound;
	private LocalBroadcastManager mLbm;
	

			@Override
			protected void onCreate(Bundle arg0) {
				super.onCreate(arg0);
				
				setContentView(R.layout.episode_tabs);
				mPonyExpressApp = (PonyExpressApp) getApplication();
				mLbm = LocalBroadcastManager.getInstance(mPonyExpressApp);
				mPlaybackCompletedReceiver = new PlaybackCompleted();
				
				mRes = getResources(); // Resource object to get Drawables
				
				mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
		        mTabHost.setup(this, getSupportFragmentManager(), R.id.realTabContent);
		        mTitleView = (TextView) findViewById(R.id.TitleText);

		        populateTabs();
			}

			private void populateTabs(){
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

				mTitleText = bundle.getString(EpisodeKeys.TITLE);
				mTitleView.setText(mTitleText);

				TabHost.TabSpec spec;  // Re-usable TabSpec for each tab			    

				spec = mTabHost.newTabSpec("episode").setIndicator
						(mRes.getText(R.string.play),mRes.getDrawable(R.drawable.ic_tab_play));
				mTabHost.addTab(spec,EpisodeFrag.class,null);

				//Add Episode Notes Activity
				spec = mTabHost.newTabSpec("notes").setIndicator
						(mRes.getText(R.string.show_notes),mRes.getDrawable(R.drawable.ic_tab_notes));
				mTabHost.addTab(spec, ShowNotesFrag.class,null);
			}
			
			/* (non-Javadoc)
			 * @see android.app.ActivityGroup#onResume()
			 */
			@Override
			protected void onResume() {
				super.onResume();
				IntentFilter filter = new IntentFilter("org.sixgun.ponyexpress.PLAYBACK_COMPLETED");
				mLbm.registerReceiver(mPlaybackCompletedReceiver, filter);
				doBindPodcastPlayer();
			}

			/* (non-Javadoc)
			 * @see android.app.ActivityGroup#onPause()
			 */
			@Override
			protected void onPause() {
				super.onPause();
				mLbm.unregisterReceiver(mPlaybackCompletedReceiver);
				doUnbindPodcastPlayer();
			}
			
			/**
			 * Save the listened to position when going back to the playlist activity
			 * when the player is still playing a playlist. (Happens anyway when paused). 
			 * Used to show a more accurate remaining time in the playlist duration.
			 */
			@Override
			public void onBackPressed() {
				if (mPodcastPlayer != null && mPlayingPlaylist && mPodcastPlayer.isPlaying()){
					mPodcastPlayer.savePlaybackPosition(mPodcastPlayer.getEpisodePosition());
				}
				super.onBackPressed();
			}


			/**
			 * This broadcast receiver receives the intent sent by PodcastPlayer
			 * to signal playback of the playlist episode has completed.	 *
			 */
			public class PlaybackCompleted extends BroadcastReceiver{

				@Override
				public void onReceive(Context context, Intent intent) {
					EpisodeTabsFragActivity.this.setResult(RESULT_OK);
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
							EpisodeTabsFragActivity.this.setResult(RESULT_OK);
							finish();
						}
					}
				}
			}
			
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

}
