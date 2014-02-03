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

import org.sixgun.ponyexpress.DownloadingEpisode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.receiver.RemoteControlReceiver;
import org.sixgun.ponyexpress.service.DownloaderService;
import org.sixgun.ponyexpress.service.PodcastPlayer;
import org.sixgun.ponyexpress.util.PonyLogger;
import org.sixgun.ponyexpress.util.Utils;
import org.sixgun.ponyexpress.util.Bitmap.RecyclingImageView;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Handles the media player service.
 */
public class PlayerActivity extends Activity {
	
	private static final String IS_PLAYING = "is_playing";
	private static final String TAG = "PonyExpress PlayerActivity";
	private static final String CURRENT_POSITION = "current_position";
	public static final String NO_MEDIA_FILE = ".nomedia";
	private static final String IS_DOWNLOADING = "is_downloading";
	private static final int NOTIFY_ID = 3;
	private PodcastPlayer mPodcastPlayer;
	private boolean mPodcastPlayerBound;
	private DownloaderService mDownloader;
	private boolean mDownloaderBound;
	private boolean mPaused = true;
	private String mAlbumArtUrl;
	private boolean mUpdateSeekBar;
	private ImageButton mPlayPauseButton;
	private ImageButton mRewindButton;
	private ImageButton mFastForwardButton;
	static private SeekBar mSeekBar;
	private TextView mElapsed; 
	private TextView mEpisodeLength;
	private int mEpisodeDuration;
	private Handler mHandler = new Handler();
	volatile private int mCurrentPosition = 0;
	private boolean mUserSeeking = false;
	private Bundle mSavedState;
	private Bundle mData;
	static protected Button mDownloadButton;
	static private Button mCancelButton;
	private boolean mCancelDownload;
	private ProgressBar mDownloadProgress;
	private PonyExpressApp mPonyExpressApp;
	static private RelativeLayout mPlayerControls;
	private Intent mPlayerIntent;
	
	private boolean mEpisodeDownloaded;
	volatile private int mDownloadPercent = 0;
	
	private Long mRow_ID;
	static private boolean mIsDownloading;
	private int mIndex;
	private DownloadStarted mDownloadReciever;
	private EpisodeCompletedAndDeleted mEpisodeCompletedReciever;
	private String mEpisodeTitle;
	
	
	//This is all responsible for connecting/disconnecting to the Downloader service.
	private ServiceConnection mDownloaderConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mDownloader = null;
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to an explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
			mDownloader = ((DownloaderService.DownloaderServiceBinder)service).getService();
			mIndex = queryDownloader();
			//mIndex can also be QUEUED (-1) or downloading (>=0) 
			if (mIndex > DownloaderService.NOT_DOWNLOADING){
				mIsDownloading = true;
				activateDownloadCancelButton();
				startDownloadProgressBar(mIndex);
			}
		}
	};
	
	protected void doBindDownloaderService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
		
		//getApplicationContext().bindService() called instead of bindService(), as
		//bindService() does not work when called from the child Activity of an ActivityGroup
		//ie:TabActivity
	    getApplicationContext().bindService(new Intent(this, 
	            DownloaderService.class), mDownloaderConnection, Context.BIND_AUTO_CREATE);
	    mDownloaderBound = true;
	}

	protected void doUnbindDownloaderService() {
	    if (mDownloaderBound) {
	        // Detach our existing connection.
	    	//Must use getApplicationContext.unbindService() as 
	    	//getApplicationContext().bindService was used to bind initially.
	        getApplicationContext().unbindService(mDownloaderConnection);
	        mDownloaderBound = false;
	    }
	}
	
	
	//This is all responsible for connecting/disconnecting to the PodcastPlayer service.
	private ServiceConnection mPlayerConnection = new ServiceConnection() {
		
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
	            PodcastPlayer.class), mPlayerConnection, Context.BIND_AUTO_CREATE);
	    mPodcastPlayerBound = true;
	}

	protected void doUnbindPodcastPlayer() {
	    if (mPodcastPlayerBound) {
	        // Detach our existing connection.
	    	//Must use getApplicationContext.unbindService() as 
	    	//getApplicationContext().bindService was used to bind initially.
	        getApplicationContext().unbindService(mPlayerConnection);
	        mPodcastPlayerBound = false;
	    }
	}
	
	private void initPlayer() {
		Intent intent = new Intent(this,PodcastPlayer.class);
		intent.putExtras(mData);
		intent.putExtra("action", PodcastPlayer.INIT_PLAYER);
		startService(intent);
	}


	
	private void queryPlayer() {
		Bundle state = new Bundle();
		mEpisodeDuration = mPodcastPlayer.getEpisodeLength();
		state.putInt(CURRENT_POSITION, mPodcastPlayer.getEpisodePosition());

		if (mPodcastPlayer.isPlaying()){
			state.putBoolean(IS_PLAYING, true);
		} else {
			state.putBoolean(IS_PLAYING, false);
		}
		restoreSeekBar(state);
		
		//Set text of episode duration
		mEpisodeLength.setText(Utils.milliToTime(mEpisodeDuration,false));
	}
	
	private int queryDownloader(){
		final int index = mDownloader.isEpisodeDownloading(mEpisodeTitle);
		return index;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mData = getIntent().getExtras();
		mCurrentPosition = mData.getInt(EpisodeKeys.LISTENED);
		mAlbumArtUrl = mData.getString(PodcastKeys.ALBUM_ART_URL);
		mRow_ID = mData.getLong(EpisodeKeys._ID);
		mEpisodeTitle = mData.getString(EpisodeKeys.TITLE);
		setContentView(R.layout.player);

		mPonyExpressApp = (PonyExpressApp)getApplication();
		
		if (mRow_ID != mPonyExpressApp.getDbHelper().getEpisodeFromPlaylist()){
			mData.putBoolean(PodcastKeys.PLAYLIST, false);
		}
		
		mDownloadReciever = new DownloadStarted();
		mEpisodeCompletedReciever = new EpisodeCompletedAndDeleted();
		
		//Set up click listeners for all player butttons and seek bar
		OnClickListener playButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mPaused){
					mPodcastPlayer.pause();
					mPaused = true;
					mCurrentPosition = (mPodcastPlayer.getEpisodePosition());
					mPlayPauseButton.setImageResource(R.drawable.media_playback_start);
					SharedPreferences prefs = getSharedPreferences(PodcastKeys.PLAYLIST, 0);
					final SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean(PodcastKeys.PLAYLIST, false);
					editor.commit();
				} else {
					// Play episode
					startService(mPlayerIntent);
					mPaused = false;
					mPlayPauseButton.setImageResource(R.drawable.media_playback_pause);
					mSeekBar.setMax(mEpisodeDuration);
					mSeekBar.setProgress(mCurrentPosition);
					startSeekBar();
				}
			}
		};

		OnClickListener rewindButtonListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mPaused){
					mPodcastPlayer.rewind();
				}
				
			}
		};
		
		OnClickListener fastForwardButtonListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mPaused){
					mPodcastPlayer.fastForward();
				}
				
			}
		};
		
		OnLongClickListener fastForwardLongClickListener = new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				if (mPodcastPlayer.isPlayingPlaylist()){
					mPodcastPlayer.skipToNext();
					return true;
				} else return false;
				
			}
		};
		
		OnSeekBarChangeListener seekBarListener = new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser){
					mCurrentPosition = progress;
					mElapsed.setText(Utils.milliToTime(progress,false));
				}
				
			}
			
			/**
			 * Stops the programmatic update of the progess bar. 
			 */
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				mUserSeeking = true;
				
			}
			
			/**
			 * Re-starts the programmatic update of the progess bar. 
			 */
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				mPodcastPlayer.SeekTo(mCurrentPosition);
				mPodcastPlayer.savePlaybackPosition(mCurrentPosition);
				mUserSeeking = false;
				
			}
			
		};
		
		OnClickListener cancelDownloadButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mDownloader.isEpisodeQueued(mEpisodeTitle)){ 
					mDownloader.removeFromQueue(mEpisodeTitle);
					//Reenable the download button
					mCancelButton.setVisibility(View.GONE);
					mCancelButton.setEnabled(false);
					mDownloadButton.setVisibility(View.VISIBLE);
					mDownloadButton.setEnabled(true);
				}else {
				//Episode is downloading
				mCancelDownload = true;
				}
			}
			
		};
		
		//Set up listener for download button
		OnClickListener downloadButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Check user wants to download on the current network type
				if (!mPonyExpressApp.getInternetHelper().isDownloadAllowed()){
					Toast.makeText(mPonyExpressApp, R.string.wrong_network_type, Toast.LENGTH_SHORT).show();
				} else {
					mIsDownloading = true;
					activateDownloadCancelButton();
					startDownload();
				}			
			}
		};
		
		
		//Link all listeners with the correct widgets
		mPlayerControls = (RelativeLayout)findViewById(R.id.player_controls);
		mPlayPauseButton = (ImageButton)findViewById(R.id.PlayButton);
		mPlayPauseButton.setOnClickListener(playButtonListener);
		mRewindButton = (ImageButton)findViewById(R.id.rewind);
		mRewindButton.setOnClickListener(rewindButtonListener);
		mFastForwardButton = (ImageButton)findViewById(R.id.fastforward);
		mFastForwardButton.setOnClickListener(fastForwardButtonListener);
		mFastForwardButton.setOnLongClickListener(fastForwardLongClickListener);
		mSeekBar = (SeekBar)findViewById(R.id.PlayerSeekBar);	
		mSeekBar.setOnSeekBarChangeListener(seekBarListener);
		mElapsed = (TextView)findViewById(R.id.elapsed_time);
		mEpisodeLength = (TextView)findViewById(R.id.length);
		mDownloadButton = (Button)findViewById(R.id.DownloadButton);
		mCancelButton = (Button)findViewById(R.id.CancelButton);
		mDownloadProgress = (ProgressBar)findViewById(R.id.DownloadProgressBar);

		//Only make Download button available if we have internet connectivity.
		if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
			mDownloadButton.setOnClickListener(downloadButtonListener);
			mCancelButton.setOnClickListener(cancelDownloadButtonListener);
		}else{
			mDownloadButton.setEnabled(false);
		}
		
		
		/**Check if episode is downloaded, show player buttons
		*if it is or download button if not.
		*/
		mEpisodeDownloaded = mPonyExpressApp.getDbHelper().isEpisodeDownloaded(mRow_ID);
		if (!mEpisodeDownloaded){
			mPlayerControls.setVisibility(View.GONE);
			mSeekBar.setVisibility(View.GONE);
			mDownloadProgress.setVisibility(View.VISIBLE);
			mDownloadButton.setVisibility(View.VISIBLE);
		} else {
			initPlayer();
		}
		
		//Get Album art url and set image.
		//Get the orientation
		final int orientation = getResources().getConfiguration().orientation;
		//Skip if orientation is landscape
		if (orientation != 2) {
			//Set the image
			RecyclingImageView album_art = (RecyclingImageView)findViewById(R.id.album_art);
			mAlbumArtUrl = getIntent().getExtras().getString(PodcastKeys.ALBUM_ART_URL);
			if (mAlbumArtUrl != null && !"".equals(mAlbumArtUrl) 
					&& !"null".equalsIgnoreCase(mAlbumArtUrl) && album_art !=null){
				PonyExpressApp.sBitmapManager.loadImage(mAlbumArtUrl, album_art);
			}
		}
		//Create an Intent to use to start playback.
		mPlayerIntent = new Intent(mPonyExpressApp,PodcastPlayer.class);
		mPlayerIntent.putExtra(RemoteControlReceiver.ACTION, 
				PodcastPlayer.PLAY_PAUSE);
		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//Detect media button presses when activity has focus and playercontrols are visible.
		if (mPlayerControls.getVisibility() != View.GONE){
			switch (keyCode){
			// **BUGWATCH** Different headsets may use different button codes,
			case KeyEvent.KEYCODE_MEDIA_REWIND:
				//Fallthrough
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				mRewindButton.performClick();
				return true;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				//Fallthrough
			case KeyEvent.KEYCODE_HEADSETHOOK:
				mPlayPauseButton.performClick();
				return true;
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				//Fallthrough
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				mFastForwardButton.performClick();
				return true;
			default:
				return false;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		if (mEpisodeDownloaded){
			//we also bind the player now as well as starting it in initPlayer.
			//This allows us to unbind it when destroying the activity and have it still play.
			doBindPodcastPlayer();
		} else doBindDownloaderService();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (mSavedState != null){
			restoreSeekBar(mSavedState);
			mIsDownloading = mSavedState.getBoolean(IS_DOWNLOADING);
		}
		if (mPodcastPlayer != null && mPodcastPlayer.isPlaying()){
			queryPlayer();
		}

		IntentFilter filter = new IntentFilter("org.sixgun.ponyexpress.DOWNLOADING");
		registerReceiver(mDownloadReciever,filter);
		IntentFilter completed = new IntentFilter("org.sixgun.ponyexpress.COMPLETED");
		registerReceiver(mEpisodeCompletedReciever, completed);
		
		if(mIsDownloading){
			activateDownloadCancelButton();
			PonyLogger.d(TAG, "Player resuming..");
			startDownloadProgressBar(mIndex);
		} else if (!mEpisodeDownloaded){
			//Check remaining space on SD card and warn if < 100Mbytes.
			double freeSpace = Utils.checkSdCardSpace();
			if (freeSpace < 100.0){
				String text = mPonyExpressApp.getString(R.string.low_space);
				text = (int)freeSpace + text;
				Toast.makeText(mPonyExpressApp, text, Toast.LENGTH_SHORT).show();
			}
		} 
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mDownloadReciever);
		unregisterReceiver(mEpisodeCompletedReciever);
		//If PodcastPlayer has been started and is not playing stop it.
		if (mEpisodeDownloaded && mPodcastPlayer != null && !mPodcastPlayer.isPlaying()){
			mPodcastPlayer.stopSelf();
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		//This allows the SeekBar or Download ProgressBar threads 
		//to die when the activity is no longer visible.
		mUpdateSeekBar = false;
		mIsDownloading = false;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindPodcastPlayer();
		doUnbindDownloaderService();
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
		if (savedInstanceState.getBoolean(IS_DOWNLOADING)){
			activateDownloadCancelButton();
			mIsDownloading = true;
		}
		mSavedState = null;
	}


	private void restoreSeekBar(Bundle savedInstanceState) {
		//The saved current position will not be accurate as play has continued in the meantime
		//but it will be a good enough approximation.
		mCurrentPosition = savedInstanceState.getInt(CURRENT_POSITION);
		//Must set max before progress if progress is > 100 (default Max)
		mSeekBar.setMax(mEpisodeDuration);
		mSeekBar.setProgress(mCurrentPosition);
		//Set text of elapsed text view
		mElapsed.setText(Utils.milliToTime(mCurrentPosition,false));
		
		if (savedInstanceState.getBoolean(IS_PLAYING)){
			mPaused = false;
			mPlayPauseButton.setImageResource(R.drawable.media_playback_pause);
			startSeekBar();
		} else if (mPaused && mEpisodeDownloaded){
			//if playing from a playlist auto start playback.
			SharedPreferences prefs = getSharedPreferences(PodcastKeys.PLAYLIST, 0);
			if (prefs.getBoolean(PodcastKeys.PLAYLIST, false)){
				mPlayPauseButton.performClick();
				final SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(PodcastKeys.PLAYLIST, false);
				editor.commit();
			}
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
		if (mIsDownloading){
			outState.putBoolean(IS_DOWNLOADING, true);
		} else {
			outState.putBoolean(IS_DOWNLOADING, false);
		}
		mSavedState = outState;
	}

	/**
	 * Deactivates the download button and replaces it with the cancel download button.
	 */
	private void activateDownloadCancelButton() {
		mDownloadButton.setVisibility(View.GONE);
		mCancelButton.setVisibility(View.VISIBLE);
		mCancelButton.setEnabled(true);
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
						PonyLogger.e(TAG, 
								"SeekBar thread failed to sleep while waiting for podcast player to bind", e);
					}
				}
				mCurrentPosition = mPodcastPlayer.getEpisodePosition();
				mSeekBar.setMax(mEpisodeDuration);
				while (mUpdateSeekBar && !mPaused){
					if (!mUserSeeking){
						try {
							Thread.sleep(1000);
							mCurrentPosition = mPodcastPlayer.getEpisodePosition();
						} catch (InterruptedException e) {
							return;
						} catch (IllegalStateException e){
							//It is possible that the player may complete 
							//while the thread is asleep and getEpisodePosition()
							//will through an exception.
							return;
						}
						
						mHandler.post(new Runnable(){
							@Override
							public void run() {
								mSeekBar.setProgress(mCurrentPosition);
								mElapsed.setText(Utils.milliToTime(mCurrentPosition,false));
								//Poll player to see if it has been paused by completing playback
								//Check for null first as sometimes the player may have been stopped before this thread is.
								if (mPodcastPlayer == null || !mPodcastPlayer.isPlaying()){
									mPaused = true;
									mPlayPauseButton.setImageResource(R.drawable.media_playback_start);
								}
							}
						});
					}
				}	
			}	
		}).start();
		
	}
	
	/**
	 * Starts the downloaderService and then starts the downloadprogress bar.
	 */
	private void startDownload(){
		Intent intent = new Intent(this,DownloaderService.class);
		intent.putExtras(mData);
		intent.putExtra("action", DownloaderService.DOWNLOAD);
		startService(intent);
	}
	
	private class DownloadStarted extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			mIndex  = intent.getExtras().getInt("index");
			startDownloadProgressBar(mIndex);
		}
		
	}
	
	private class EpisodeCompletedAndDeleted extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			finish();			
		}
		
	}
	
	/**
	 * Starts a thread to poll the episodes download progress and updates the seek bar
	 * via a handler. 
	 */
	private void startDownloadProgressBar(final int index){
		new Thread(new Runnable(){
			@Override
			public void run() {
				mHandler.post(disableDownloadButton);
				
				//When resuming the activity, the Downloader needs to be 
				// rebound.  So sleep before trying to access it.
				while (mDownloader == null){
					try {
						PonyLogger.d(TAG, "Sleeping while Downloader rebinds");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						PonyLogger.e(TAG, 
								"DownloadProgressBar thread failed to sleep while " +
								"waiting for downloaderservice to bind", e);
					}
				}
				if (index != DownloaderService.QUEUED){  //Episode is not queued
					mDownloadProgress.setMax(100);
					boolean downloadError = false;
					while (mIsDownloading && mDownloadPercent < 100 ){
						try {
							downloadError = mDownloader.checkForDownloadError(index);
							if (downloadError || mCancelDownload){
								mIsDownloading = false;
								mDownloadPercent= 0;
							}
							Thread.sleep(1000);
							mDownloadPercent = (int) mDownloader.getProgress(index);
							if (mDownloadPercent == DownloadingEpisode.OVERSIZE_EPISODE_DOWNLOADED){
								//Oversize episode completed, break loop
								mDownloadPercent = 100;
							}
						} catch (InterruptedException e) {
							PonyLogger.w(TAG, "Download thread interupted while sleeping!", e);
						}
						if (mDownloadPercent == DownloadingEpisode.OVERSIZE_EPISODE){
							//Real progress is unknown, so set to indeterminate.
							PonyLogger.d(TAG, "Set Indeterminate");
							mHandler.post(setIndeterminate);
						} else {
							//Post progress to mHandler in UI thread
							mHandler.post(setProgress);
						}
					}
					//Download completed
					if (mDownloadPercent == 100){
						//Post download completion runnable to mHandler in UI thread
						mHandler.post(downloadCompleted);
					}//Download failed
					else if (downloadError){
						//Post downloadFailed runnable to mHandler to reset UI
						mHandler.post(downloadCancelled); //Reenable button and zero progress
						mHandler.post(downloadFailed); //Post notification
						mDownloader.resetDownloadError(index);

					}
					//download cancelled
					else if (mCancelDownload){
						mDownloader.cancelDownload(index);
						mHandler.post(downloadCancelled);
						mCancelDownload = false;
					}
				}//episode is queued
			}
		}).start();
	}
	
	
	
	
	Runnable disableDownloadButton = new Runnable() {
		
		@Override
		public void run() {
			mDownloadButton.setEnabled(false);
		}
	};
	
	Runnable setProgress = new Runnable() {
		
		@Override
		public void run() {
			mDownloadProgress.setProgress(mDownloadPercent);
		}
	};
	
	Runnable setIndeterminate = new Runnable() {

		@Override
		public void run() {
			mDownloadProgress.setIndeterminate(true);
			//invalidate forces the view to be redrawn now it has changed.
			mDownloadProgress.invalidate();
		}
		
	};
	
	Runnable downloadCompleted = new Runnable() {
		
		@Override
		public void run() {
			//Change views in UI and bind player.
			mIsDownloading = false;
			mEpisodeDownloaded = true;
						
			mDownloadProgress.setVisibility(View.GONE);
			mCancelButton.setVisibility(View.GONE);
			mSeekBar.setVisibility(View.VISIBLE);
			mPlayerControls.setVisibility(View.VISIBLE);
			initPlayer();
			//we also bind the player now as well as starting it in initPlayer.
			//This allows us to unbind it when destroying the activity and have it still play.
			doBindPodcastPlayer();
			
		}
	};
	
	Runnable downloadFailed = new Runnable() {

		@Override
		public void run() {
			//Send a notification to the user telling them of the failure
			//This uses an empty intent because there is no new activity to start.
			PendingIntent intent = PendingIntent.getActivity(mPonyExpressApp, 
					0, new Intent(), 0);
			NotificationManager notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			int icon = R.drawable.stat_notify_error;
			CharSequence text = getText(R.string.download_failed);
			Notification notification = new Notification(
					icon, null,
					System.currentTimeMillis());
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(mPonyExpressApp, 
					getText(R.string.app_name), text, intent);
			notifyManager.notify(NOTIFY_ID,notification);
		}
		
	};
	
	Runnable downloadCancelled = new Runnable() {
		@Override
		public void run() {
			//Reenable the download button and zero the progress bar
			mCancelButton.setVisibility(View.GONE);
			mCancelButton.setEnabled(false);
			mDownloadButton.setVisibility(View.VISIBLE);
			mDownloadButton.setEnabled(true);
			mDownloadProgress.setProgress(0);
			//Ensure indeterminate set to false
			mDownloadProgress.setIndeterminate(false);
			mDownloadProgress.invalidate();
		}
		
	};
}
	
