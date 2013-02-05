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
package org.sixgun.ponyexpress.service;

import java.io.File;
import java.io.IOException;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.EpisodeTabs;
import org.sixgun.ponyexpress.activity.EpisodesActivity;
import org.sixgun.ponyexpress.receiver.RemoteControlReceiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * PodcastPlayer is a service that handles all interactions with the media player.
 * Min API level is 8.
 *
 */
public class PodcastPlayer extends Service implements AudioManager.OnAudioFocusChangeListener {

	private static final String TAG = "PonyExpress PodcastPlayer";
	
	//Some constants that define the requested Player action.
	public static final int REWIND = -1;
	public static final int PLAY_PAUSE = 0;
	public static final int FASTFORWARD = 1;
	public static final int INIT_PLAYER = 2;

	private static final int NOTIFY_ID = 2;
	
	private AudioManager mAudioManager;
	private ComponentName mRemoteControlReceiver;
	private final IBinder mBinder = new PodcastPlayerBinder();
	private PonyExpressApp mPonyExpressApp; 
	private MediaPlayer mPlayer1;
	private MediaPlayer mPlayer2;
	private MediaPlayer mFreePlayer;//All initialisation of the players happens through this
	private MediaPlayer mPlayer;//All playing of the players happens through this.
	private String mPodcastName;
	private String mPodcastNameQueued;
	private String mEpisodeQueued;
	private String mEpisodePlaying;
	private long mRowID;
	private long mRowIDQueued;
	private HeadPhoneReceiver mHeadPhoneReciever;
	boolean mHeadPhonesIn = false;
	private NotificationManager mNM;
	private String mEpisodeName;

	private Bundle mData;

	private boolean mIsInitialised = false;

	private boolean mQueuedIsUnlistened;

	SharedPreferences mPrefs;

	private boolean mPlayingPlaylist;



	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class PodcastPlayerBinder extends Binder {
		public PodcastPlayer getService() {
			return PodcastPlayer.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mPlayer1 = new MediaPlayer();
		mPlayer2 = new MediaPlayer();
		mPlayer = mPlayer1;
		mFreePlayer = mPlayer2;
		mPonyExpressApp = (PonyExpressApp)getApplication();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		OnCompletionListener onCompletionListener = new OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer mp) {
				abandonAudioFocus();
				//Set Listened to 0				
				if (mPodcastName != null && savePlaybackPosition(0)) {
					Log.d(TAG, "Updated listened to position to " + 0);
				}
				
				//Get next episode in playlist.
				if (mPlayingPlaylist && !mPonyExpressApp.getDbHelper().playlistEnding())
				{
					mPonyExpressApp.getDbHelper().popPlaylist();
					final String podcast_name = mPonyExpressApp.getDbHelper().getPodcastFromPlaylist();
					final long episode_id = mPonyExpressApp.getDbHelper().getEpisodeFromPlaylist();
					//TODO Check an episode has been returned, if db corrupted it will not be.
					//What do we do if it is corrupted??
					Bundle bundle = new Bundle();
					bundle = Episode.packageEpisode(mPonyExpressApp, podcast_name, episode_id);
					initPlayer(bundle);
					play();
					//Send a broadcast intent to EpisodeTabs
					//telling it to refresh with the new episode
					Intent intent = new Intent("org.sixgun.ponyexpress.PLAYBACK_COMPLETED");
					getApplicationContext().sendBroadcast(intent);
				} else { //Just stop
					hideNotification();
					unRegisterHeadPhoneReceiver();
				}
			}
			
		};
		mPlayer.setOnCompletionListener(onCompletionListener);
		mFreePlayer.setOnCompletionListener(onCompletionListener);
		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		mRemoteControlReceiver = new ComponentName(getPackageName(),
				RemoteControlReceiver.class.getName());

		Log.d(TAG, "PodcastPlayer started");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int action = intent.getIntExtra("action", -2);
		switch (action){
		case INIT_PLAYER:
			initPlayer(intent.getExtras());
			break;
		case REWIND:
			if (isPlaying()){
				rewind();
			}
			break;
		case PLAY_PAUSE:
			if (isPlaying()){
				pause();
			}else if (mIsInitialised){
				play();	
			}else if (!mIsInitialised){
				//TODO Add functionality to resume if a remote play is press
				//when the service is not running.
			}

			break;
		case FASTFORWARD:
			if (isPlaying()){
				fastForward();
			}
			break;
			//TODO Should recieve a skip from the reciever too, but can't test at present.
		case -2:
			Log.e(TAG, "no action received from RemoteControlReceiver!");
			break;
		default:
			Log.e(TAG, "unknown action received from RemoteControlReceiver: " + action);
			break;
		}
		//The receiver may restart the service if media buttons are pressed after
		//the service has been stopped.  It will not be init at that point so should 
		//be stopped again.
		if (!mIsInitialised){
			stopSelf();
		}
		return START_NOT_STICKY;
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mIsInitialised){
			pause();
		}

		if (mFreePlayer != null){
			mFreePlayer.release();
			mFreePlayer = null;
		}
		if (mPlayer != null){
			mPlayer.release();
			mPlayer = null;
		}
		mNM.cancel(NOTIFY_ID);
		Log.d(TAG, "PodcastPlayer stopped");
	}

	/** Initilises the free Media Player with the correct title and sets 
	 * it up for playback.
	 * The rowID is used to update the position
	 * that has been listened to in the database.
	 * @param file
	 * @param position
	 * @param rowID
	 */
	public void initPlayer(Bundle data){
		registerRemoteControl();
		mData = data;
		mPlayingPlaylist = mData.getBoolean(PodcastKeys.PLAYLIST);
		mEpisodeName = mData.getString(EpisodeKeys.TITLE);
		mPodcastNameQueued = mData.getString(PodcastKeys.NAME);
		final String file = mData.getString(EpisodeKeys.FILENAME);
		String path = PonyExpressApp.PODCAST_PATH + mPodcastNameQueued + file;
		mRowIDQueued = mData.getLong(EpisodeKeys._ID);
		boolean isError = false;
		int startPosition = getPlaybackPosition();

		if (!file.equals(mEpisodeQueued)){
			mFreePlayer.reset();
			//Set podcast as data source and prepare the player
			File podcast = new File(Environment.getExternalStorageDirectory(),path);
			try {
				mFreePlayer.setDataSource(podcast.getAbsolutePath());
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Illegal path supplied to player", e);
				isError = true;
			} catch (IllegalStateException e) {
				Log.e(TAG, "Player is not set up correctly", e);
				isError = true;
			} catch (IOException e) {
				Log.e(TAG,"Player cannot access path",e);
				isError = true;
			}
			if (!isError) {
				try {
					mFreePlayer.prepare();
				} catch (IllegalStateException e) {
					Log.e(TAG,"Cannot prepare Player. Incorrect state",e);
					isError = true;
				} catch (IOException e) {
					Log.e(TAG,"Player cannot access path",e);
					isError = true;
				}
			}
			//SeekTo last listened position
			if (startPosition != -1){
				mFreePlayer.seekTo(startPosition);
				Log.d(TAG,"Seeking to " + startPosition);
				mQueuedIsUnlistened = false;
			} else {
				mQueuedIsUnlistened = true;
			}

			mEpisodeQueued = file;
		}
		if (isError){
			mIsInitialised = false;
			mEpisodeQueued = ""; //this ensures that if the user re-enters 
			//the player that the init is carried out.
			showErrorNotification();
		} else {
			mIsInitialised = true;
		}
	}

	/**
	 * Swaps the queued episode on the free player
	 * to the mPlayer and begins playback.  
	 */
	public void play() {

		int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);

		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Log.d(TAG, "Audio focus granted");
		}
		registerHeadPhoneReceiver();
		registerRemoteControl();
		if (!mEpisodeQueued.equals(mEpisodePlaying)) {
			//We want to play a different episode so stop any currently playing
			// and record the playback position
			if (mPlayer.isPlaying()){
				this.pause();
			}
			mPlayer.reset();

			//Swap mPlayer and mFreePlayer
			MediaPlayer swap = null;
			swap = mPlayer;
			mPlayer = mFreePlayer;
			mFreePlayer = swap;
			mRowID = mRowIDQueued;
			mPodcastName = mPodcastNameQueued;

			//Mark the episode listened if previously unlistened.
			if (mQueuedIsUnlistened){
				savePlaybackPosition(0);
			}
		}

		mPlayer.start();

		int playbackPosition = getPlaybackPosition();
		//Fix for android 2.2 HTC phones that 
		//don't seek to the current position with mp3 and instead go to 0
		if (playbackPosition != 0){
			mPlayer.seekTo(playbackPosition - 10); // This extra seek is needed!
		} else {
			mPlayer.seekTo(playbackPosition + 10);
		}
		mPlayer.seekTo(playbackPosition);

		showNotification();
		mEpisodePlaying = mEpisodeQueued;
		Log.d(TAG,"Playing " + mEpisodePlaying);

	}

	public void pause() {

		abandonAudioFocus();
		unRegisterHeadPhoneReceiver();
		mPlayer.pause();
		hideNotification();
		if (mPodcastName != null){ //null if playback hasn't started yet, and we are quitting
			//Record last listened position in database
			final int playbackPosition = mPlayer.getCurrentPosition();
			boolean save = savePlaybackPosition(playbackPosition);
			if (save){
				Log.d(TAG, "Updated listened to position to " + playbackPosition);
			}else{
				Log.d(TAG, "Error saving playback position at " + playbackPosition);
			}
		}

		stopSelf();
	}

	public void fastForward() {
		//Fast forwards a certain number of seconds based on the Seek Time user setting
		final int playbackPosition = mPlayer.getCurrentPosition();
		final int seekDelta = Integer.parseInt(mPrefs.getString(getString(R.string.ff_seek_time_key), "30000"));
		final int newPosition = playbackPosition + seekDelta;
		mPlayer.seekTo(newPosition);
	}

	public void rewind() {
		//Rewinds a certain number of seconds based on the Seek Time user setting
		final int playbackPosition = mPlayer.getCurrentPosition();
		final int seekDelta = Integer.parseInt(mPrefs.getString(getString(R.string.r_seek_time_key), "30000"));
		final int newPosition = playbackPosition - seekDelta;

		//This check is needed because seeking to a negative position can cause errors
		//on some implementations of MediaPlayer.
		if(newPosition < 0){
			mPlayer.seekTo(0);
		}else{
			mPlayer.seekTo(newPosition);
		}
	}
	/** This Method is called from PlayerActivity to Seek using the SeekBar  
	 * 
	 * @param progress
	 */
	public void SeekTo(int progress) {
		if (!mEpisodeQueued.equals(mEpisodePlaying)) {
			//The queued title is the one needed for the current PlayerActivity
			// before playback begins
			mFreePlayer.seekTo(progress);
		} else {
			//Playback has been started now, so the mPlayer is correct#
			mPlayer.seekTo(progress);
		}
	}

	public int getEpisodeLength(){
		if (!mEpisodeQueued.equals(mEpisodePlaying)) {
			//The queued title is the one needed for the current PlayerActivity
			// before playback begins
			return mFreePlayer.getDuration();
		} else {
			//Playback has been started now, so the mPlayer duration is correct
			return mPlayer.getDuration();
		}
	}

	//This method returns the saved current position from the DB for PodcastPlayer.java.
	//This is needed because MediaPlayer.getCurrentPosition() can be unreliable in it's
	//position returns.
	private int getPlaybackPosition(){
		int playbackPosition = mPonyExpressApp.getDbHelper().getListened(mRowIDQueued, mPodcastNameQueued);

		//Adds a 10sec recap on resume if that option is marked true in the prefs,
		//and then return playbackPosition. Otherwise, simply return playbackPosition.
		if (mPrefs.getBoolean(getString(R.string.recap_on_resume_key), false)){
			if (playbackPosition < 10000){
				return 0;
			}else{
				return (playbackPosition - 10000);
			}
		}else{
			return playbackPosition;
		}
	}

	//Simple save the current play back position to the DB, and
	//return a boolean of success.
	public boolean savePlaybackPosition(int playbackPosition){
		String podcast_name = mPodcastName;
		long row_id = mRowID;
		if (podcast_name == null){ //if playback hasn't started yet
			//Moving the progress bar before play starts needs to update position
			//for the queued podcast.
			podcast_name = mPodcastNameQueued;
			row_id = mRowIDQueued;
		}
		return mPonyExpressApp.getDbHelper().update(podcast_name,row_id,
				EpisodeKeys.LISTENED, playbackPosition);
	}

	//This method returns the current position from MediaPlayer
	//for the seekBar in PlayerActivity.java.  MediaPlayer.getCurrentPosition()
	//can be unreliable, but it's ok for the seekbar. Also, we don't
	//want to hammer the DB with reads and writes.
	public int getEpisodePosition(){
		if (!mEpisodeQueued.equals(mEpisodePlaying)) {
			//The queued title is the one needed for the current PlayerActivity
			// before playback begins
			if (mFreePlayer != null){
				return mFreePlayer.getCurrentPosition();
			} else return 0; //When player is stopped quickly, the seekbar update thread may still 
			//try and access the current position before it is stopped.
		} else {
			//Playback has been started now, so the mPlayer duration is correct
			if (mPlayer != null){
				return mPlayer.getCurrentPosition();
			} else return 0;
		}
	}

	public String getEpisodeTitle(){
		return mEpisodePlaying;
	}

	public String getPodcastName(){
		return mPodcastName;
	}

	public long getEpisodeRow(){
		return mRowID;
	}

	public boolean isPlaying() {
		if (mPlayer != null && mPlayer.isPlaying()){
			return true;
		} else return false;
	}

	/**
	 * HeadPhoneReceiver handles callbacks from when headphones are unplugged.
	 * 
	 */
	private class HeadPhoneReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG,"Headphone unplugged");
			pause();
		}
		
	}

	private void registerHeadPhoneReceiver() {
		if (mHeadPhoneReciever == null){
			mHeadPhoneReciever = new HeadPhoneReceiver();
			IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
			registerReceiver(mHeadPhoneReciever, filter);
		}
	}

	private void unRegisterHeadPhoneReceiver(){
		//unregister HeadPhone reciever
		if (mHeadPhoneReciever != null){
			unregisterReceiver(mHeadPhoneReciever);
			mHeadPhoneReciever = null;
		} 
	}

	private void showNotification() {
		//Episode tabs launchmode is 'singletop' so only one instance can 
		//exist when it is top of the stack. Thus starting it with this intent 
		//we get the original activity not a new one whenit is top of the stack.
		//For occasions when it is not top we still need to supply the data bundle.
		Intent notificationIntent = new Intent(this,EpisodeTabs.class);
		notificationIntent.putExtras(mData);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent intent = PendingIntent.getActivity(mPonyExpressApp, 
				0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification = new Notification(
				R.drawable.playicon, null,
				System.currentTimeMillis());
		String text = "Playing " + mEpisodeName;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(mPonyExpressApp, 
				getText(R.string.app_name), text, intent);
		
		mNM.notify(NOTIFY_ID, notification);
	}

	private void hideNotification() {
		mNM.cancel(NOTIFY_ID);
	}

	private void showErrorNotification(){
		//Shows a notification that there is an error with an episode file 
		//and suggests the user long press to re-download.
		Intent notificationIntent = new Intent(this,EpisodesActivity.class);
		notificationIntent.putExtras(mData);
		PendingIntent contentIntent = PendingIntent.getActivity(mPonyExpressApp, 
				0, notificationIntent, 0);
		Notification notification = new Notification(
				R.drawable.stat_notify_error, null,
				System.currentTimeMillis());

		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_LIGHTS;

		RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.wrappable_notification_layout);
		contentView.setImageViewResource(R.id.notification_image, R.drawable.stat_notify_error);
		contentView.setTextViewText(R.id.notification_title,getText(R.string.app_name));
		contentView.setTextViewText(R.id.notification_text, getText(R.string.file_error));
		notification.contentView = contentView;
		notification.contentIntent = contentIntent;
		
		mNM.notify(NOTIFY_ID, notification);
		
	}

	/**
	 * Registering media buttons tells the android system which app to control on 
	 * media button presses.  It uses a "last on top" approach.  If we register Pony,
	 * that means we want the media buttons to control Pony until it's unregistered. 
	 * Once Pony is unregistered, the previous app registered will be controlled.
	 * Registration can be given several times, but unregistered only once.
	 */
	private void registerRemoteControl() {
		mAudioManager.registerMediaButtonEventReceiver(mRemoteControlReceiver); 
	}

	/** 
	 * Sister method to registerRemoteControl().
	 */
	private void unRegisterRemoteControl() {
		mAudioManager.unregisterMediaButtonEventReceiver(mRemoteControlReceiver); 
	}

	
	/**
	 * This method abandons audio focus.
	 */
	private void abandonAudioFocus() {
		//Abandon audio focus
		int result = mAudioManager.abandonAudioFocus(this);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
			Log.d(TAG, "Audio focus abandoned");
		}else{
			Log.e(TAG, "Audio focus failed to abandon");
		}
	}

	/** Implemented from AudioManager.OnAudioFocusChangeListener.  This is used to
	* regulate audio apps so that two apps don't play audio at the same time.  We also
	* use it to handle phone rings. Also, media button registration can take place 
	* on focus changes.
	*
	* Note: Other apps must also respect focus and call for focus changes or this
	* will not work!  All api>=8 "incoming call apps" seem to respect focus.
	*/
	@Override
	public void onAudioFocusChange(int focus) {
		switch (focus) {

		case AudioManager.AUDIOFOCUS_GAIN:
			// resume play back when something else gives focus back
			Log.d(TAG, "Audio focus has been returned from another app");
			registerRemoteControl();
			if (!mPlayer.isPlaying()) {
				mPlayer.seekTo(getPlaybackPosition());
				mPlayer.start();
			}
			break;

		case AudioManager.AUDIOFOCUS_LOSS:
			// focus will be taken for an extend period of time, so we
			// need to pause and stop the service
			Log.d(TAG, "Audio focus lost-permanent");
			unRegisterRemoteControl();
			pause();
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			// focus will be given back soon so we just need to pause for a moment
			Log.d(TAG, "Audio focus lost-transient");
			if (mPlayer.isPlaying()) {
				mPlayer.pause();
				savePlaybackPosition(mPlayer.getCurrentPosition());
			}
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			// focus will be given back soon and we can continue playing,
			// if we so choose, but we will not
			Log.d(TAG, "Audio focus lost-transient and can duck");
			if (mPlayer.isPlaying()) {
				mPlayer.pause();
				savePlaybackPosition(mPlayer.getCurrentPosition());
			}
			break;
		}
	}

	public boolean isPlayingPlaylist() {
		return mPlayingPlaylist;
	}

	public void skipToNext() {
		mPlayer.seekTo(getEpisodeLength());
		
	}
}