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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.EpisodeTabs;
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
import android.media.AudioManager;
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
	
	//Some constants that define the requested Player action.
	public static final int REWIND = -1;
	public static final int PLAY_PAUSE = 0;
	public static final int FASTFORWARD = 1;
	public static final int INIT_PLAYER = 2;

	private static final int NOTIFY_ID = 2;
	//These method are used if Android 2.2 is available, via reflection.
	private static Method mRegisterMediaButtonEventReceiver;
    private static Method mUnregisterMediaButtonEventReceiver;
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
	private boolean mResumeAfterCall = false; 
	private boolean mBeenResumedAfterCall = false; 
	private int mSeekDelta = 30000; // 30 seconds
	private long mRowID;
	private long mRowIDQueued;
	HeadPhoneReceiver mHeadPhoneReciever;
	boolean mHeadPhonesIn = false;
	private NotificationManager mNM;
	private String mEpisodeName;

	private Bundle mData;

	private boolean mIsInitialised;

	private boolean mQueuedIsUnlistened;

	
	
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
	//This static method initialises out custom media button registration methods
	//if on android 2.2 or greater.
	private static void initializeRemoteControlRegistrationMethods() {
		   try {
		      if (mRegisterMediaButtonEventReceiver == null) {
		         mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
		               "registerMediaButtonEventReceiver",
		               new Class[] { ComponentName.class } );
		      }
		      if (mUnregisterMediaButtonEventReceiver == null) {
		         mUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
		               "unregisterMediaButtonEventReceiver",
		               new Class[] { ComponentName.class } );
		      }
		      /* success, this device will take advantage of better remote */
		      /* control event handling in Android 2.2                                    */
		   } catch (NoSuchMethodException nsme) {
		      /* failure, still using the legacy behavior, but this app    */
		      /* is future-proof!                                          */
		   }
		}
	
	static {
        initializeRemoteControlRegistrationMethods();
    }
	
	@Override
	public void onCreate() {
		super.onCreate();
		mPlayer1 = new MediaPlayer();
		mPlayer2 = new MediaPlayer();
		mPlayer = mPlayer1;
		mFreePlayer = mPlayer2;
		mPonyExpressApp = (PonyExpressApp)getApplication();
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		OnCompletionListener onCompletionListener = new OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer mp) {
				mp.start();
				Log.d(TAG,"Playback re-started");
				mp.pause();
				hideNotification();
				//Set Listened to 0
				boolean res = mPonyExpressApp.getDbHelper().update(mPodcastName, mRowID, 
						EpisodeKeys.LISTENED, 0);
				if (res) {
					Log.d(TAG, "Updated listened to position to 0");
				}
			}
			
		};
		mPlayer.setOnCompletionListener(onCompletionListener);
		mFreePlayer.setOnCompletionListener(onCompletionListener);
		
		//Create MediaButton Broadcast reciever for Android2.2
		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	    mRemoteControlReceiver = new ComponentName(getPackageName(),
	                RemoteControlReceiver.class.getName());
		
		
		Log.d(TAG, "PodcastPlayer started");
	}
	
	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
	    handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    handleCommand(intent);
	    return START_NOT_STICKY;
	}
	
	private void handleCommand(Intent intent){
		int action = intent.getIntExtra("action", -2);
		switch (action){
		case INIT_PLAYER:
			initPlayer(intent.getExtras());
			mIsInitialised = true;
			break;
		case REWIND:
			if (isPlaying()){
				rewind();
			}
			break;
		case PLAY_PAUSE:
			if (isPlaying()){
				pause();
			} else if (mIsInitialised) {
				play();
			}
			break;
		case FASTFORWARD:
			if (isPlaying()){
				fastForward();
			}
			break;
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
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
		
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
		mEpisodeName = mData.getString(EpisodeKeys.TITLE);
		mPodcastNameQueued = mData.getString(PodcastKeys.NAME);
		final String file = mData.getString(EpisodeKeys.FILENAME);
		String path = PonyExpressApp.PODCAST_PATH + mPodcastNameQueued + file;
		mRowIDQueued = mData.getLong(EpisodeKeys._ID);
		int position = mPonyExpressApp.getDbHelper().getListened(mRowIDQueued, mPodcastNameQueued);
	
		if (!file.equals(mEpisodeQueued)){
			mFreePlayer.reset();
			//Set podcast as data source and prepare the player
			File podcast = new File(Environment.getExternalStorageDirectory(),path);
			try {
				mFreePlayer.setDataSource(podcast.getAbsolutePath());
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Illegal path supplied to player", e);
			} catch (IllegalStateException e) {
				Log.e(TAG, "Player is not set up correctly", e);
			} catch (IOException e) {
				Log.e(TAG,"Player cannot access path",e);
			}
			try {
				mFreePlayer.prepare();
			} catch (IllegalStateException e) {
				Log.e(TAG,"Cannot prepare Player. Incorrect state",e);
			} catch (IOException e) {
				Log.e(TAG,"Player cannot access path",e);
			}
			//SeekTo last listened position
			if (position != -1){
				mFreePlayer.seekTo(position);
				Log.d(TAG,"Seeking to " + position);
				mQueuedIsUnlistened = false;
			} else {
				mQueuedIsUnlistened = true;
			}
			
			mEpisodeQueued = file;
		}
		mIsInitialised = true;
	}
	
	/**
	 * Swaps the queued episode on the free player
	 * to the mPlayer and begins playback.  
	 */
	public void play() {
		
		//Register HeadPhone receiver
		mHeadPhoneReciever = new HeadPhoneReceiver();
		IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		registerReceiver(mHeadPhoneReciever, filter);
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
				mPonyExpressApp.getDbHelper().update(mPodcastName, mRowID, 
						EpisodeKeys.LISTENED, 0);
			}
		}
		
		mPlayer.start();
		showNotification();
		mEpisodePlaying = mEpisodeQueued;
		Log.d(TAG,"Playing " + mEpisodePlaying);
		
	}
	
	public void pause() {
		//unregister HeadPhone reciever
		unregisterReceiver(mHeadPhoneReciever);
		mHeadPhoneReciever = null;
		
		mPlayer.pause();
		hideNotification();
		//Record last listened position in database
		final int playbackPosition = mPlayer.getCurrentPosition();
		boolean res =mPonyExpressApp.getDbHelper().update(mPodcastName,mRowID, 
				EpisodeKeys.LISTENED, playbackPosition);
		if (res){
			Log.d(TAG, "Updated listened to position to " + playbackPosition);
		}
		
		stopSelf();
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
	
	public int getEpisodePosition(){
		if (!mEpisodeQueued.equals(mEpisodePlaying)) {
			//The queued title is the one needed for the current PlayerActivity
			// before playback begins
			return mFreePlayer.getCurrentPosition();
		} else {
			//Playback has been started now, so the mPlayer duration is correct
			return mPlayer.getCurrentPosition();
		}
	}
	
	public String getEpisodeTitle(){
		return mEpisodePlaying;
	}
	
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}
	
	public boolean isResumeAfterCall() {
		final boolean ret = mBeenResumedAfterCall;
		mBeenResumedAfterCall = false;
		return ret;
	}
	
	private PhoneStateListener mPhoneListener = new PhoneStateListener(){

		/**
		 * Pauses playback if a call is recieved or if a call is to be made.
		 */
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			//TODO The media buttons still work the player when a call arrives, 
			//you can't pick up with the pickup key
			switch (state)
			{
			case TelephonyManager.CALL_STATE_RINGING:
				//Fall through
			case TelephonyManager.CALL_STATE_OFFHOOK:
				if (mPlayer.isPlaying()){
					pause();
					mResumeAfterCall  = true;
				}
				//Not sure if this is necessary.. ?
				unregisterRemoteControl();
				break;
			case TelephonyManager.CALL_STATE_IDLE:
				if (mResumeAfterCall){
					//Don't automatically restart playback, let user initiate it.
					//play();
					registerRemoteControl();
					mResumeAfterCall = false;
					mBeenResumedAfterCall = true;
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
	
	private void registerRemoteControl() {
        try {
            if (mRegisterMediaButtonEventReceiver == null) {
            	//Running on < android2.2
            	Log.d(TAG,"register media button receiver < 2.2");
                return;
            }
            //Running on > android 2.2
            Log.d(TAG,"register media button receiver => 2.2");
            mRegisterMediaButtonEventReceiver.invoke(mAudioManager,
                    mRemoteControlReceiver);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected " + ie);
        }
    }
    
    private void unregisterRemoteControl() {
        try {
            if (mUnregisterMediaButtonEventReceiver == null) {
            	//Running on < android2.2
                return;
            }
            //Running on > android 2.2
            mUnregisterMediaButtonEventReceiver.invoke(mAudioManager,
                    mRemoteControlReceiver);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            System.err.println("unexpected " + ie);  
        }
    }
	


}
