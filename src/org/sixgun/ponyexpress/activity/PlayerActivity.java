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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.service.PodcastPlayer;
import org.sixgun.ponyexpress.util.Utils;
import org.sixgun.ponyexpress.view.RemoteImageView;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * Handles the media player service.
 */
public class PlayerActivity extends Activity {
	
	private static final String IS_PLAYING = "is_playing";
	private static final String TAG = "PonyExpress PlayerActivity";
	private static final String CURRENT_POSITION = "current_position";
	public static final String NO_MEDIA_FILE = ".nomedia";
	private static final String IS_DOWNLOADING = "is_downloading";
	private PodcastPlayer mPodcastPlayer;
	private boolean mPodcastPlayerBound;
	private boolean mPaused = true;
	private String mAlbumArtUrl;
	private boolean mUpdateSeekBar;
	private ImageButton mPlayPauseButton;
	static private SeekBar mSeekBar;
	private TextView mElapsed; 
	private TextView mEpisodeLength;
	private int mEpisodeDuration;
	private Handler mHandler = new Handler();
	volatile private int mCurrentPosition = 0;
	private boolean mUserSeeking = false;
	private Bundle mSavedState;
	private Bundle mData;
	static protected View mDownloadButton;
	static private ProgressBar mDownloadProgress;
	private PonyExpressApp mPonyExpressApp;
	static private RelativeLayout mPlayerControls;
	private NotificationManager mNM;
	
	//These are all used by the DownloadEpisode AsyncTask
	private String mPodcastName;
	private String mPodcastPath;
	private URL mUrl;
	private Long mRow_ID; //Needed to update the db
	private int mSize;
	private File mRoot;
	private String mFilename;
	private FileOutputStream mOutFile;
	private InputStream mInFile;
	private int mTotalDownloaded;
	static private boolean mIsDownloading;
	
	
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
			if (!mData.containsKey(EpisodeKeys.URL)){
				//Only init player if episode has been downloaded
				initPlayer();
				queryPlayer();
			}
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
	
	private void initPlayer() {
		mPodcastPlayer.initPlayer(mCurrentPosition, mData);
	}


	
	private void queryPlayer() {
		Bundle state = new Bundle();
		mEpisodeDuration = mPodcastPlayer.getEpisodeLength();
		state.putInt(CURRENT_POSITION, mPodcastPlayer.getEpisodePosition());
		//if activity is restarted after a call, isPlaying() may not
		// be set true yet, as it is asynchronous. 
		if (mPodcastPlayer.isPlaying() || mPodcastPlayer.isResumeAfterCall()){
			state.putBoolean(IS_PLAYING, true);
		} else {
			state.putBoolean(IS_PLAYING, false);
		}
		restoreSeekBar(state);
		
		//Set text of episode duration
		mEpisodeLength.setText(Utils.milliToTime(mEpisodeDuration));
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mData = getIntent().getExtras();
		mCurrentPosition = mData.getInt(EpisodeKeys.LISTENED);
		mAlbumArtUrl = mData.getString(PodcastKeys.ALBUM_ART_URL);
		mPodcastName = (mData.getString(PodcastKeys.NAME));
		mRow_ID = mData.getLong(EpisodeKeys._ID);
		setContentView(R.layout.player);

		mPonyExpressApp = (PonyExpressApp)getApplication();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		//Set up click listeners for all player butttons and seek bar
		OnClickListener mPlayButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mPaused){
					mPodcastPlayer.pause();
					mPaused = true;
					mPlayPauseButton.setImageResource(R.drawable.media_playback_start);
					
				} else {
					// Play episdode
					mPodcastPlayer.play();
					mPaused = false;
					mPlayPauseButton.setImageResource(R.drawable.media_playback_pause);
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
					mCurrentPosition = progress;
					mElapsed.setText(Utils.milliToTime(progress));
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
				mUserSeeking = false;
				
			}
			
		};
		
		//Set up listener for download button
		OnClickListener mDownloadButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				new DownloadEpisode().execute();
				mDownloadButton.setEnabled(false);
				
			}
		};
		
		//Link all listeners with the correct widgets
		mPlayerControls = (RelativeLayout)findViewById(R.id.player_controls);
		mPlayPauseButton = (ImageButton)findViewById(R.id.PlayButton);
		mPlayPauseButton.setOnClickListener(mPlayButtonListener);
		ImageButton rewindButton = (ImageButton)findViewById(R.id.rewind);
		rewindButton.setOnClickListener(mRewindButtonListener);
		ImageButton fastForwardButton = (ImageButton)findViewById(R.id.fastforward);
		fastForwardButton.setOnClickListener(mFastForwardButtonListener);
		mSeekBar = (SeekBar)findViewById(R.id.PlayerSeekBar);	
		mSeekBar.setOnSeekBarChangeListener(mSeekBarListener);
		mElapsed = (TextView)findViewById(R.id.elapsed_time);
		mEpisodeLength = (TextView)findViewById(R.id.length);
		mDownloadButton = (Button)findViewById(R.id.DownloadButton);
		mDownloadProgress = (ProgressBar)findViewById(R.id.DownloadProgressBar);

		//Only make Download button available if we have internet connectivity.
		if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
			mDownloadButton.setOnClickListener(mDownloadButtonListener);
		}else{
			mDownloadButton.setEnabled(false);
		}
		
		/**Check if episode is downloaded, show player buttons
		*if it is or download button if not.
		*/
		if (!mPonyExpressApp.getDbHelper().isEpisodeDownloaded(mRow_ID, mPodcastName)){
			mPlayerControls.setVisibility(View.GONE);
			mSeekBar.setVisibility(View.GONE);
			mDownloadProgress.setVisibility(View.VISIBLE);
			mDownloadButton.setVisibility(View.VISIBLE);
			
		}
		
		//Get Album art url and set image.
		RemoteImageView album_art = (RemoteImageView)findViewById(R.id.album_art);
		mAlbumArtUrl = getIntent().getExtras().getString(PodcastKeys.ALBUM_ART_URL);
		if (mAlbumArtUrl!= null && !"".equals(mAlbumArtUrl) 
				&& !"null".equalsIgnoreCase(mAlbumArtUrl) && album_art!=null){
    		album_art.setRemoteURI(mAlbumArtUrl);
    		album_art.loadImage();
		}
		
				
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
		if (savedInstanceState.getBoolean(IS_DOWNLOADING)){
			mDownloadButton.setEnabled(false);
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
		mElapsed.setText(Utils.milliToTime(mCurrentPosition));
		
		if (savedInstanceState.getBoolean(IS_PLAYING)){
			mPaused = false;
			mPlayPauseButton.setImageResource(R.drawable.media_playback_pause);
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
		if (mIsDownloading){
			outState.putBoolean(IS_DOWNLOADING, true);
		} else {
			outState.putBoolean(IS_DOWNLOADING, false);
		}
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
				mSeekBar.setMax(mEpisodeDuration);
				while (mUpdateSeekBar && !mPaused){
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
								mElapsed.setText(Utils.milliToTime(mCurrentPosition));
								//Poll player to see if it has been paused by completing playback
								if (!mPodcastPlayer.isPlaying()){
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
	
	private class DownloadEpisode extends AsyncTask <Void,Double,Void>{


		protected static final int NOTIFY_ID = 1;

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.d(TAG,"Downloader started");
			mPodcastPath = PonyExpressApp.PODCAST_PATH + mPodcastName;
			mUrl = getURL(mData.getString(EpisodeKeys.URL));
			mSize = mData.getInt(EpisodeKeys.SIZE);
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			mIsDownloading = true;
			if (mUrl != null && isSDCardWritable()){
				prepareForDownload();
				createNoMediaFile();
				if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
					// update progress and notification
					showNotification();
					downloadFile();
				} else {
					Log.d(TAG, "No Internet Connection.");
				}
			}
			return null;
		}
		
		/**
		 * Parse the url string to a URL type.
		 * @param _url string from the Intent.
		 * @return URL object.
		 */
		private URL getURL(String _url) {
			URL url;
			try {
				url = new URL(_url);
			} catch (MalformedURLException e) {
				Log.e(TAG, "Episode URL badly formed.", e);
				return null;
			}
			return url;
		}
		
		/**
		 * Checks that the SD card is mounted with read/write access and that 
		 * we can write to the correct path. 
		 * @return true if writable.
		 */
		private boolean isSDCardWritable() {
			final String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)){
				Log.d(TAG, "SD Card is mounted");
				mRoot = Environment.getExternalStorageDirectory();
				if (mRoot.canWrite()){
					Log.d(TAG,"Can Write to SD card.");
					return true;
				} else {
					Log.d(TAG, "SD Card is not writable.");	
				}	
			}
			return false;
		}
		
		/**
		 * Creates the path needed to save the files.
		 */
		private void prepareForDownload() {
			File path = new File(mRoot, mPodcastPath);
			path.mkdirs();
			
			//Split filename from path url.
			final String filename_path = mUrl.getFile();
			mFilename = filename_path.substring(filename_path.lastIndexOf('/'));
			try {
				mOutFile = new FileOutputStream(new File(path,mFilename));
			} catch (FileNotFoundException e) {
				// TODO Improve Error handling.
				Log.e(TAG, "Cannot open FileOutputStream for writing.",e);
			}
		}
		
		/**
		 * Creates a nomedia file if it doesn't exist. This file causes 
		 * the media scanner to ignore the podcast files. 
		 */
		private void createNoMediaFile() {
			final String path = mRoot + mPodcastPath + "/";
			File noMedia = new File(path,NO_MEDIA_FILE);
			if (!noMedia.exists()){
				FileOutputStream writeFile = null;
				try {
					writeFile = new FileOutputStream(noMedia);
				} catch (FileNotFoundException e) {
					Log.e(TAG, "Cannot create .nomedia file", e);
				}
				try {
					writeFile.write(new byte[1]);
				} catch (IOException e) {
					Log.e(TAG, "Cannot create .nomedia file", e);
				}
			}
			
		}
		
		/**
		 * Downloads the file mFilename to mOutFile from mUrl.
		 */
		private void downloadFile() {
			try {
				mInFile = mUrl.openStream();
			} catch (IOException e) {
				// TODO Improve this error handling.  Check network up etc.. 
				throw new RuntimeException(e);
			}
			byte[] buffer = new byte[1024];
			int size = 0;
					
			Log.d(TAG,"Writing " + mFilename);
			try {
				while ((size = mInFile.read(buffer)) > 0 ) {
					mOutFile.write(buffer,0, size);
					mTotalDownloaded  += size;
				}
				Log.d(TAG,"Podcast written to SD card.");
				mPonyExpressApp.getDbHelper().update(mPodcastName, mRow_ID, EpisodeKeys.DOWNLOADED,"true");
			} catch (IOException e) {
				Log.e(TAG, "Error reading/writing to file.", e);
			}
		}
		/**
		 * Shows a notification in the status bar when downloading an episode
		 * and updates the download progress bar.
		 */
		private void showNotification() {
			new Thread(new Runnable() {
				CharSequence text = "";
				//This uses an empty intent because there is no new activity to start.
				PendingIntent intent = PendingIntent.getActivity(mPonyExpressApp, 
						0, new Intent(), 0);
				
				@Override
				public void run() {
					int icon = R.drawable.sixgunicon0;
					String progress = "";
					double percent = 0.0;
					do {
						percent = mTotalDownloaded/(double)mSize * 100;
						progress = String.format("%.0f", percent);
						text = progress + "% " + getText(R.string.downloading_episode);
						if (percent > 15.0 && percent < 30.0) {
							icon = R.drawable.sixgunicon1;
						} else if (percent > 30.0 && percent < 45.0) {
							icon = R.drawable.sixgunicon2;
						} else if (percent > 45.0 && percent < 60.0) {
							icon = R.drawable.sixgunicon3;
						} else if (percent > 60.0 && percent < 75.0) {
							icon = R.drawable.sixgunicon4;
						} else if (percent > 75.0 && percent < 90.0) {
							icon = R.drawable.sixgunicon5;
						} else if (percent > 90.0) {
							icon = R.drawable.sixgunicon6;
						}
						
						Notification notification = new Notification(
								icon, null,
								System.currentTimeMillis());
						notification.flags |= Notification.FLAG_ONGOING_EVENT;
						notification.setLatestEventInfo(mPonyExpressApp, 
								getText(R.string.app_name), text, intent);
						
						mNM.notify(NOTIFY_ID, notification);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							return;
						}
						
						publishProgress(percent);
					} while (mTotalDownloaded < mSize);
				}
			}).start();
		}

		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(Double... percent) {
			mDownloadProgress.setProgress(percent[0].intValue());
		}
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			//Change views in UI and init and query player.
			initPlayer();
			queryPlayer();
			mNM.cancel(NOTIFY_ID);
			mIsDownloading = false;
			mDownloadProgress.setVisibility(View.GONE);
			mDownloadButton.setVisibility(View.GONE);
			mSeekBar.setVisibility(View.VISIBLE);
			mPlayerControls.setVisibility(View.VISIBLE);
			
		}
		
	}
}
