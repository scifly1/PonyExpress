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

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
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
	private static final String PODCAST_PATH = "/Android/data/org.sixgun.PonyExpress/files";

	private final IBinder mBinder = new PodcastPlayerBinder();
	
	private MediaPlayer mPlayer;
	private String mTitlePlaying;
	private boolean mResumeAfterCall = false; 
	private int mSeekDelta = 30000; // 30 seconds
	
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
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		Log.d(TAG, "PodcastPlayer started");
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		//TODO Stop any running threads /Tidy up
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
		
		mPlayer.release();
		mPlayer = null;
		Log.d(TAG, "PodcastPlayer stopped");
	}
	
	public void play(String file) {
		String path = PODCAST_PATH + file;
		
		if (file.equals(mTitlePlaying) && mPlayer != null){
			//We are probably resuming after pause
			mPlayer.start();
			Log.d(TAG,"Playing " + path);
		} else {
			//Reset player as it might be playing or paused on another episode
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
				mPlayer.start();
				mTitlePlaying = file;
			Log.d(TAG,"Playing " + path);
			} else Log.e(TAG, "Player is null");
		}
		
	}
	
	public void pause() {
		mPlayer.pause();
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

}
