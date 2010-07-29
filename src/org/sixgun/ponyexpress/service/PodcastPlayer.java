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

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PonyExpressApp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * PodcastPlayer is a service that handles all interactions with the media player.
 *
 */
public class PodcastPlayer extends Service {

	private static final String TAG = "PonyExpress PodcastPlayer";

	private final IBinder mBinder = new PodcastPlayerBinder();
	private PonyExpressApp mPonyExpressApp; 
	private MediaPlayer mPlayer;
	private String mTitlePlaying;
	private boolean mResumeAfterCall = false; 
	private int mSeekDelta = 30000; // 30 seconds
	private long mRowID;
	HeadPhoneReceiver mHeadPhoneReciever;
	boolean mHeadPhonesIn = false;
	
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
		mPlayer = new MediaPlayer();
		mPonyExpressApp = (PonyExpressApp)getApplication();
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		OnCompletionListener onCompletionListener = new OnCompletionListener(){

			@Override
			public void onCompletion(MediaPlayer mp) {
				mp.start();
				Log.d(TAG,"Playback re-started");
				mp.pause();
				//Set Listened to 0
				boolean res = mPonyExpressApp.getDbHelper().update(mRowID, 
						EpisodeKeys.LISTENED, 0);
				if (res) {
					Log.d(TAG, "Updated listened to position to 0");
				}
			}
			
		};
		mPlayer.setOnCompletionListener(onCompletionListener);
		
		Log.d(TAG, "PodcastPlayer started");
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
		
		mPlayer.release();
		mPlayer = null;
		Log.d(TAG, "PodcastPlayer stopped");
	}
	/**
	 * Plays the file starting from position.  The rowID is used to update the position
	 * that has been listened to in the database.
	 * @param file
	 * @param position
	 * @param rowID
	 */
	public void play(String file, int position, long rowID) {
		String path = PonyExpressApp.PODCAST_PATH + file;
		
		//Register HeadPhone receiver
		mHeadPhoneReciever = new HeadPhoneReceiver();
		IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		registerReceiver(mHeadPhoneReciever, filter);
		
		if (file.equals(mTitlePlaying) && mPlayer != null){
			//We are probably resuming after pause
			mPlayer.start();
			Log.d(TAG,"Playing " + path);
		} else {
			//If player is playing then pause playback which also records the
			//playback position in the database.
			if (mPlayer.isPlaying()){
				this.pause(); 
			}
			//Set mRowID now, after previously playing episode
			//has been paused and playback position recorded.
			mRowID = rowID;
			//Reset player as it might well be paused on another episode
			mPlayer.reset();
			//Set podcast as data source and prepare the player
			File podcast = new File(Environment.getExternalStorageDirectory(),path);
			if (mPlayer != null){
				try {
					mPlayer.setDataSource(podcast.getAbsolutePath());
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "Illegal path supplied to player", e);
				} catch (IllegalStateException e) {
					Log.e(TAG, "Player is not set up correctly", e);
				} catch (IOException e) {
					Log.e(TAG,"Player cannot access path",e);
				}
				try {
					mPlayer.prepare();
				} catch (IllegalStateException e) {
					Log.e(TAG,"Cannot prepare Player. Incorrect state",e);
				} catch (IOException e) {
					Log.e(TAG,"Player cannot access path",e);
				}
				//SeekTo last listened position
				if (position != -1){
					SeekTo(position);
					Log.d(TAG,"Seeking to " + position);
				}
				mPlayer.start();
				mTitlePlaying = file;
			Log.d(TAG,"Playing " + path);
			} else Log.e(TAG, "Player is null");
		}
		
	}
	
	public void pause() {
		//unregister HeadPhone reciever
		unregisterReceiver(mHeadPhoneReciever);
		mHeadPhoneReciever = null;
		
		mPlayer.pause();
		//Record last listened position in database
		final int playbackPosition = mPlayer.getCurrentPosition();
		boolean res =mPonyExpressApp.getDbHelper().update(mRowID, 
				EpisodeKeys.LISTENED, playbackPosition);
		if (res){
			Log.d(TAG, "Updated listened to position to " + playbackPosition);
		}
	}
		
	
	public void fastForward() {
		final int playbackPosition = mPlayer.getCurrentPosition();
		final int newPosition = mSeekDelta + playbackPosition;
		mPlayer.seekTo(newPosition);
	}
	
	public void rewind() {
		final int playbackPosition = mPlayer.getCurrentPosition();
		final int newPosition = playbackPosition - mSeekDelta;
		mPlayer.seekTo(newPosition);
	}
	
	public void SeekTo(int progress) {
		mPlayer.seekTo(progress);		
	}
	
	public int getEpisodeLength(){
		return mPlayer.getDuration();
	}
	
	public int getEpisodePosition(){
		return mPlayer.getCurrentPosition();
	}
	
	public String getEpisodeTitle(){
		return mTitlePlaying;
	}
	
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}
	
	private PhoneStateListener mPhoneListener = new PhoneStateListener(){

		/**
		 * Pauses playback if a call is recieved or if a call is to be made.
		 */
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state)
			{
			case TelephonyManager.CALL_STATE_RINGING:
				//Fall through
			case TelephonyManager.CALL_STATE_OFFHOOK:
				if (mPlayer.isPlaying()){
					mPlayer.pause();
					mResumeAfterCall  = true;
				break;
				}
			case TelephonyManager.CALL_STATE_IDLE:
				if (mResumeAfterCall){
					mPlayer.start();
					mResumeAfterCall = false;
					break;
				}
			default:
				Log.d(TAG, "Unknown phone state: " + state);
			}
		}
	};
	
	private class HeadPhoneReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			//FIXME This caching of the previous headphone state could be avoided 
			//if we used Android 2.0 (API level 5) as we could 
			//use isInitialStickyBroadcast()
			boolean prevHeadPhonesIn = mHeadPhonesIn;
			Bundle data = intent.getExtras();
			final int state = data.getInt("state");
			switch (state) {
			case 0:
				mHeadPhonesIn = false;
				break;
			case 1:
				//Fall through.  Some headsets cause state 1 some 2..
			case 2:
				mHeadPhonesIn = true;
				break;
			default:
				Log.w(TAG, "Headphone state unknown: " + state);
				break;
			}
			if (prevHeadPhonesIn && !mHeadPhonesIn){
				pause();
			}
		}
		
	}

}
