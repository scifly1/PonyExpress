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

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.service.PodcastPlayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * Handles the media player service.
 */
public class PlayerActivity extends Activity {
	
	private static final String IS_PLAYING = "is_playing";
	private static final String TAG = "PonyExpress PlayerActivity";
	private static final String CURRENT_POSITION = "current_position";
	private static final String EPISODE_LENGTH = "episode_length";
	private PodcastPlayer mPodcastPlayer;
	private boolean mPodcastPlayerBound;
	private boolean mPaused = true;
	private String mEpisodeTitle;
	private boolean mUpdateSeekBar;
	private Button mPlayPauseButton;
	private SeekBar mSeekBar;
	private Handler mHandler = new Handler();
	private int mCurrentPosition = 0;
	private boolean mUserSeeking = false;
	private Bundle mSavedState;
	
	//This is all responsible for connecting/disconnecting to the PodcastPlayer service.
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mPodcastPlayer = null;
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to an explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
			mPodcastPlayer = ((PodcastPlayer.PodcastPlayerBinder)service).getService();
			queryPlayer();
		}
	};
	
	protected void doBindPodcastPlayer() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
		
		//getApplicationContext().bindService() called instead of bindService(), as
		//bindService() does not work when called from the child Activity of an ActivityGroup
		//ie:TabActivity
	    getApplicationContext().bindService(new Intent(this, 
	            PodcastPlayer.class), mConnection, Context.BIND_AUTO_CREATE);
	    mPodcastPlayerBound = true;
	}


	protected void doUnbindPodcastPlayer() {
	    if (mPodcastPlayerBound) {
	        // Detach our existing connection.
	    	//Must use getApplicationContext.unbindService() as 
	    	//getApplicationContext().bindService was used to bind initially.
	        getApplicationContext().unbindService(mConnection);
	        mPodcastPlayerBound = false;
	    }
	}
	
	private void queryPlayer() {
		final String title = mPodcastPlayer.getEpisodeTitle();
		if (title != null && title.equals(mEpisodeTitle)){
			Bundle state = new Bundle();
			state.putInt(EPISODE_LENGTH, mPodcastPlayer.getEpisodeLength());
			state.putInt(CURRENT_POSITION, mPodcastPlayer.getEpisodePosition());
			state.putBoolean(IS_PLAYING, mPodcastPlayer.isPlaying());
			restoreSeekBar(state);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Bundle data = getIntent().getExtras();
		mEpisodeTitle = data.getString(EpisodeKeys.FILENAME);
		setContentView(R.layout.player);

		
		OnClickListener mPlayButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mPaused){
					mPodcastPlayer.pause();
					mPaused = true;
					mPlayPauseButton.setText(R.string.play);
					
				} else {
					// Play episdode
					mCurrentPosition = data.getInt(EpisodeKeys.LISTENED);
					final long row_ID = data.getLong(EpisodeKeys._ID);
					mPodcastPlayer.play(mEpisodeTitle,mCurrentPosition,row_ID);
					mPaused = false;
					mPlayPauseButton.setText(R.string.pause);
					mSeekBar.setMax(mPodcastPlayer.getEpisodeLength());
					mSeekBar.setProgress(mCurrentPosition);
					startSeekBar();
					
				}
			}
		};
		
		OnClickListener mRewindButtonListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mPaused){
					mPodcastPlayer.rewind();
				}
				
			}
		};
		
		OnClickListener mFastForwardButtonListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mPaused){
					mPodcastPlayer.fastForward();
				}
				
			}
		};
		
		OnSeekBarChangeListener mSeekBarListener = new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser){
					mPodcastPlayer.SeekTo(progress);
				}
				
			}
			
			/**
			 * Stops the progrommatic update of the progess bar. 
			 */
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				mUserSeeking = true;
				
			}
			
			/**
			 * Re-starts the progrommatic update of the progess bar. 
			 */
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				mUserSeeking = false;
				
			}
			
		};
		
		mPlayPauseButton = (Button)findViewById(R.id.PlayButton);
		mPlayPauseButton.setOnClickListener(mPlayButtonListener);
		Button rewindButton = (Button)findViewById(R.id.rewind);
		rewindButton.setOnClickListener(mRewindButtonListener);
		Button fastForwardButton = (Button)findViewById(R.id.fastforward);
		fastForwardButton.setOnClickListener(mFastForwardButtonListener);
		mSeekBar = (SeekBar)findViewById(R.id.PlayerSeekBar);	
		mSeekBar.setOnSeekBarChangeListener(mSeekBarListener);
				
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		doBindPodcastPlayer();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (mSavedState != null){
			restoreSeekBar(mSavedState);	
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		//This allows the SeekBar thread to die when the activity is no longer visable.
		mUpdateSeekBar = false;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	protected void onRestart() {
		super.onRestart();
		if (!mPaused){
			startSeekBar();
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		restoreSeekBar(savedInstanceState);
		mSavedState = null;
	}


	private void restoreSeekBar(Bundle savedInstanceState) {
		//The saved current position will not be accurate as play has continued in the meantime
		//but it will be a good enough approximation.
		mCurrentPosition = savedInstanceState.getInt(CURRENT_POSITION);
		//Must set max before progress if progress is > 100 (default Max)
		mSeekBar.setMax(savedInstanceState.getInt(EPISODE_LENGTH));
		mSeekBar.setProgress(mCurrentPosition);
		if (savedInstanceState.getBoolean(IS_PLAYING)){
			mPaused = false;
			mPlayPauseButton.setText(R.string.pause);
			startSeekBar();
		}
		
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (!mPaused){
			outState.putBoolean(IS_PLAYING, true);
		} else {
			outState.putBoolean(IS_PLAYING, false);
		}
		outState.putInt(CURRENT_POSITION, mCurrentPosition);
		outState.putInt(EPISODE_LENGTH, mPodcastPlayer.getEpisodeLength());
		mSavedState = outState;
	}


	/**
	 * Starts a thread to poll the episodes progress and updates the seek bar
	 * via a handler. 
	 */
	private void startSeekBar() {
		new Thread(new Runnable(){
			@Override
			public void run() {
				mUpdateSeekBar = true;
				//When resuming the activity, the PodcastPlayer needs to be 
				// rebound.  So sleep before trying to access it.
				while (mPodcastPlayer == null){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.e(TAG, 
								"SeekBar thread failed to sleep while waiting for podcast player to bind", e);
					}
				}
				mCurrentPosition = mPodcastPlayer.getEpisodePosition();
				int length = mPodcastPlayer.getEpisodeLength();
				mSeekBar.setMax(length);
				while (mUpdateSeekBar && !mPaused && mCurrentPosition < length){
					if (!mUserSeeking){
						try {
							Thread.sleep(1000);
							mCurrentPosition = mPodcastPlayer.getEpisodePosition();
						} catch (InterruptedException e) {
							return;
						}
						
						mHandler.post(new Runnable(){
							@Override
							public void run() {
								mSeekBar.setProgress(mCurrentPosition);	
							}
						});
					}
				}	
			}	
		}).start();
		
	}
}
