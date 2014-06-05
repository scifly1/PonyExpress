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
package org.sixgun.ponyexpress.fragment;

import org.sixgun.ponyexpress.DownloadingEpisode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.PreferencesActivity;
import org.sixgun.ponyexpress.receiver.RemoteControlReceiver;
import org.sixgun.ponyexpress.service.DownloaderService;
import org.sixgun.ponyexpress.service.PodcastPlayer;
import org.sixgun.ponyexpress.util.PonyLogger;
import org.sixgun.ponyexpress.util.Utils;
import org.sixgun.ponyexpress.util.Bitmap.RecyclingImageView;

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
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class EpisodeFrag extends Fragment implements OnClickListener, OnLongClickListener{

	protected static final String TAG = "EpisodeFragment";
	private static final int NOTIFY_ID = 3;
	private static final String CURRENT_POSITION = "current_position";
	private static final String IS_PLAYING = "is_playing";
	private static final String IS_DOWNLOADING = "is_downloading";
	private Bundle mData;
	private int mCurrentPosition;
	private String mAlbumArtUrl;
	private long mRow_ID;
	private String mEpisodeTitle;
	private RelativeLayout mPlayerControls;
	private ImageButton mPlayPauseButton;
	private ImageButton mRewindButton;
	private ImageButton mFastForwardButton;
	private SeekBar mSeekBar;
	private TextView mElapsed;
	protected boolean mUserSeeking;
	private TextView mEpisodeLength;
	private Button mDownloadButton;
	private Button mCancelButton;
	private ProgressBar mDownloadProgress;
	private PonyExpressApp mPonyExpressApp;
	private boolean mEpisodeDownloaded;
	private boolean mIsDownloading;
	public int mIndex;
	private DownloadStarted mDownloadReciever;
	private boolean mDownloaderBound;
	protected DownloaderService mDownloader;
	protected int mDownloadPercent;
	protected Handler mHandler;
	private boolean mCancelDownload;
	private boolean mPaused;
	private PodcastPlayer mPodcastPlayer;
	private int mEpisodeDuration;
	private Intent mPlayerIntent;
	protected boolean mUpdateSeekBar;
	private boolean mPodcastPlayerBound;
	private Bundle mSavedState;
	private EpisodeCompletedAndDeleted mEpisodeCompletedReciever;
	private LocalBroadcastManager mLbm;
	private PlayBackStarted mPlaybackReceiver;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		mData = getActivity().getIntent().getExtras();
		mCurrentPosition = mData.getInt(EpisodeKeys.LISTENED);
		mAlbumArtUrl = mData.getString(PodcastKeys.ALBUM_ART_URL);
		mRow_ID = mData.getLong(EpisodeKeys._ID);
		mEpisodeTitle = mData.getString(EpisodeKeys.TITLE);
		
		mPonyExpressApp = (PonyExpressApp)getActivity().getApplication();
		mLbm = LocalBroadcastManager.getInstance(mPonyExpressApp);
		mDownloadReciever = new DownloadStarted();
		mEpisodeCompletedReciever = new EpisodeCompletedAndDeleted();
		mPlaybackReceiver = new PlayBackStarted();
		mHandler = new Handler();
		mPaused = true;
		
		//Create an Intent to use to start playback.
		mPlayerIntent = new Intent(mPonyExpressApp,PodcastPlayer.class);
		mPlayerIntent.putExtra(RemoteControlReceiver.ACTION, 
				PodcastPlayer.PLAY_PAUSE);
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.player, null);
		
		//Set up some listeners
		OnSeekBarChangeListener seekBarListener = new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser){
					mCurrentPosition = progress;
					mElapsed.setText(Utils.milliToTime(progress,false));
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mUserSeeking = true;
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mPodcastPlayer.SeekTo(mCurrentPosition);
				mPodcastPlayer.savePlaybackPosition(mCurrentPosition);
				mUserSeeking = false;
			}
				
				
			};
		
		//Get all controls
		mPlayerControls = (RelativeLayout)v.findViewById(R.id.player_controls);
		mPlayPauseButton = (ImageButton)v.findViewById(R.id.PlayButton);
		mPlayPauseButton.setOnClickListener(this);
		mRewindButton = (ImageButton)v.findViewById(R.id.rewind);
		mRewindButton.setOnClickListener(this);
		mFastForwardButton = (ImageButton)v.findViewById(R.id.fastforward);
		mFastForwardButton.setOnClickListener(this);
		mFastForwardButton.setOnLongClickListener(this);
		mSeekBar = (SeekBar)v.findViewById(R.id.PlayerSeekBar);	
		mSeekBar.setOnSeekBarChangeListener(seekBarListener);
		mElapsed = (TextView)v.findViewById(R.id.elapsed_time);
		mEpisodeLength = (TextView)v.findViewById(R.id.length);
		mDownloadButton = (Button)v.findViewById(R.id.DownloadButton);
		mCancelButton = (Button)v.findViewById(R.id.CancelButton);
		mDownloadProgress = (ProgressBar)v.findViewById(R.id.DownloadProgressBar);
		
		
		//Only make Download button available if we have internet connectivity.
		if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
			mDownloadButton.setOnClickListener(this);
			mCancelButton.setOnClickListener(this);
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
		//FIXME If landscape and large screen don't skip
		if (orientation != 2) {
			//Set the image
			RecyclingImageView album_art = (RecyclingImageView)v.findViewById(R.id.album_art);
			mAlbumArtUrl = getActivity().getIntent().getExtras().getString(PodcastKeys.ALBUM_ART_URL);
			if (mAlbumArtUrl != null && !"".equals(mAlbumArtUrl) 
					&& !"null".equalsIgnoreCase(mAlbumArtUrl) && album_art !=null){
				PonyExpressApp.sBitmapManager.loadImage(mAlbumArtUrl, album_art);
			}
		}
		return v;
	}
	

	@Override
	public void onStart() {
		super.onStart();
		if (mEpisodeDownloaded){
			//we also bind the player now as well as starting it in initPlayer.
			//This allows us to unbind it when destroying the activity and have it still play.
			doBindPodcastPlayer();
		} else doBindDownloaderService();
		
		if (!mPaused){
			startSeekBar();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mLbm.unregisterReceiver(mDownloadReciever);
		mLbm.unregisterReceiver(mEpisodeCompletedReciever);
		mLbm.unregisterReceiver(mPlaybackReceiver);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mIsDownloading){
			outState.putBoolean(IS_DOWNLOADING, true);
		} else {
			outState.putBoolean(IS_DOWNLOADING, false);
		}
		mSavedState = outState;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		//This allows the SeekBar or Download ProgressBar threads 
		//to die when the activity is no longer visible.
		mUpdateSeekBar = false;
		mIsDownloading = false;
	}

	@Override
	public void onResume() {
		super.onResume();
		
		if (mSavedState != null){
			mIsDownloading = mSavedState.getBoolean(IS_DOWNLOADING);
		}
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
		if (mPodcastPlayer != null){
			queryPlayer();
		}
		IntentFilter completed = new IntentFilter(PodcastPlayer.COMPLETED_DELETED);
		mLbm.registerReceiver(mEpisodeCompletedReciever, completed);
		IntentFilter filter = new IntentFilter(DownloaderService.DOWNLOADING);
		mLbm.registerReceiver(mDownloadReciever,filter);
		IntentFilter playback_started = new IntentFilter(PodcastPlayer.PLAYING);
		mLbm.registerReceiver(mPlaybackReceiver, playback_started);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		doUnbindDownloaderService();
		doUnbindPodcastPlayer();
	}
	
	@Override
	public void onClick(View v) {
		if (v == mDownloadButton){
			//Check user wants to download on the current network type
			if (!mPonyExpressApp.getInternetHelper().isDownloadAllowed()){
				Toast.makeText(mPonyExpressApp, R.string.wrong_network_type, Toast.LENGTH_SHORT).show();
			} else {
				mIsDownloading = true;
				activateDownloadCancelButton();
				startDownload();
			}	
		} else if (v == mCancelButton){
			if (mDownloader.isEpisodeQueued(mEpisodeTitle)){ 
				mDownloader.removeFromQueue(mEpisodeTitle);
				//Re-enable the download button
				mCancelButton.setVisibility(View.GONE);
				mCancelButton.setEnabled(false);
				mDownloadButton.setVisibility(View.VISIBLE);
				mDownloadButton.setEnabled(true);
			}else {
			//Episode is downloading
			mCancelDownload = true;
			} 
		}else if (v == mPlayPauseButton){
			if (!mPaused){
				mPodcastPlayer.pause();
				mPaused = true;
				mCurrentPosition = (mPodcastPlayer.getEpisodePosition());
				mPlayPauseButton.setImageResource(R.drawable.media_playback_start);
				SharedPreferences prefs = getActivity().getSharedPreferences(PodcastKeys.PLAYLIST, 0);
				final SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(PodcastKeys.PLAYLIST, false);
				editor.commit();
			} else {
				// Play episode
				getActivity().startService(mPlayerIntent);
				mPaused = false;
				mPlayPauseButton.setImageResource(R.drawable.media_playback_pause);
				mSeekBar.setMax(mEpisodeDuration);
				mSeekBar.setProgress(mCurrentPosition);
				startSeekBar();
			}
		} else if (v == mRewindButton){
			if (!mPaused){
				mPodcastPlayer.rewind();
			}
		} else if (v == mFastForwardButton){
			if (!mPaused){
				mPodcastPlayer.fastForward();
			}
		}
		
	}

	@Override
	public boolean onLongClick(View v) {
		if (v == mFastForwardButton){
			if (mPodcastPlayer.isPlayingPlaylist()){
				mPodcastPlayer.skipToNext();
				return true;
			} 	
		}
		return false;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.general_options_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.settings_menu:
	        startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
		}
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
	 * Starts the downloaderService.
	 */
	private void startDownload(){
		Intent intent = new Intent(getActivity(),DownloaderService.class);
		intent.putExtras(mData);
		intent.putExtra("action", DownloaderService.DOWNLOAD);
		getActivity().startService(intent);
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
			getActivity().finish();			
		}
		
	}
	private class PlayBackStarted extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			onClick(mPlayPauseButton);
			
		}
		
	}
	
	//This is all responsible for connecting/disconnecting to the Downloader service.
	private ServiceConnection mDownloaderConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mDownloader = null;

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
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
		getActivity().bindService(new Intent(getActivity(),DownloaderService.class),
				mDownloaderConnection, Context.BIND_AUTO_CREATE);
		mDownloaderBound = true;
	}

	protected void doUnbindDownloaderService() {
		if (mDownloaderBound) {
			getActivity().unbindService(mDownloaderConnection);
			mDownloaderBound = false;
		}
	}
		
	private int queryDownloader(){
		final int index = mDownloader.isEpisodeDownloading(mEpisodeTitle);
		return index;
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
				//If resuming shortly after download starts, the index may still read queued
				//when the download isn't really, so wait a second.
				if (index == DownloaderService.QUEUED){
					try{
						Thread.sleep(1000);
						PonyLogger.d(TAG,"Waiting while index is queued");
					} catch (InterruptedException e){
						PonyLogger.e(TAG, "DownloadPrgressBar failed to sleep while queued",e);
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
								//Over-size episode completed, break loop
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
						mHandler.post(downloadCancelled); //Re-enable button and zero progress
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
			NotificationManager notifyManager = 
					(NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
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
			//Re-enable the download button and zero the progress bar
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
							//will throw an exception.
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
		getActivity().bindService(new Intent(getActivity(), 
				PodcastPlayer.class), mPlayerConnection, Context.BIND_AUTO_CREATE);
		mPodcastPlayerBound = true;
	}

	protected void doUnbindPodcastPlayer() {
		if (mPodcastPlayerBound) {
			getActivity().unbindService(mPlayerConnection);
			mPodcastPlayerBound = false;
		}
	}

	private void initPlayer() {
		Intent intent = new Intent(getActivity(),PodcastPlayer.class);
		intent.putExtras(mData);
		intent.putExtra("action", PodcastPlayer.INIT_PLAYER);
		getActivity().startService(intent);
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
			//if playing from a play-list auto start playback.
			SharedPreferences prefs = getActivity().getSharedPreferences(PodcastKeys.PLAYLIST, 0);
			if (prefs.getBoolean(PodcastKeys.PLAYLIST, false)){
				mPlayPauseButton.performClick();
				final SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(PodcastKeys.PLAYLIST, false);
				editor.commit();
			}
		}
	}

}
