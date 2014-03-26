/*
 * Copyright 2010,2013 Paul Elms
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.sixgun.ponyexpress.DownloadingEpisode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.DownloadOverviewActivity;
import org.sixgun.ponyexpress.util.PonyExpressDbAdaptor;
import org.sixgun.ponyexpress.util.PonyLogger;
import org.sixgun.ponyexpress.util.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;


public class DownloaderService extends Service {


    private final IBinder mBinder = new DownloaderServiceBinder();
	private static final String TAG = "PonyExpress Downloader";
	private static final String NO_MEDIA_FILE = ".nomedia";
	public static final String DOWNLOADING = "org.sixgun.ponyexpress.DOWNLOADING";
	public static final int NOT_DOWNLOADING = -2;
	public static final int QUEUED = -1;
	private static final int NOTIFY_ID = 1;
	private static final int NOTIFY_ERROR_ID = NOTIFY_ID +1;
	public static final int DOWNLOAD = 0;
	public static final int INIT = DOWNLOAD + 1 ;
	//TODO make MAX_CONCURRENT_DOWNLOADS a preference?
	private static final int MAX_CONCURRENT_DOWNLOADS = 3;
				
	private PonyExpressApp mPonyExpressApp;
	//Do not remove episodes from mEpisodes as you'll change the index 
	//of other episodes that may be being accessed.
	private ArrayList<DownloadingEpisode> mEpisodes;
	private File mRoot;
	protected NotificationManager mNM;
	volatile private int mCurrentDownloads;
	private boolean mDownloaderAwake = false;
	private Handler mHandler = new Handler();
	private LinkedList<DownloadingEpisode> mQueue;
	private List<DownloadingEpisode> mCurrentDownloadsList;
	
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class DownloaderServiceBinder extends Binder {
        public DownloaderService getService() {
            return DownloaderService.this;
        }
    }
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		PonyLogger.d(TAG, "Downloader bound.");
		return mBinder;
	}
	

	/* (non-Javadoc)
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		PonyLogger.d(TAG, "Downloader unbound.");
		return super.onUnbind(intent);
	}


	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		PonyLogger.d(TAG, "Downloader Service Started");
		mPonyExpressApp = (PonyExpressApp)getApplication();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mEpisodes = new ArrayList<DownloadingEpisode>();
		mQueue = new LinkedList<DownloadingEpisode>();
		mDownloaderAwake = true;
		beginLooperThread();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PonyLogger.d(TAG, "Downloader Service Killed/Stopped");
		mDownloaderAwake = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    handleCommand(intent);
	    return START_STICKY;
	}
	
	private void handleCommand(Intent intent){
		if (intent == null){
			PonyLogger.w(TAG, "Null intent passed to handleCommand");
			return;
		}
		int action = intent.getIntExtra("action", -1);
		switch (action){
		case DOWNLOAD:
			initDownload(intent.getExtras());
			break;
		default:
			PonyLogger.e(TAG, "unknown action received by DownloaderService: " + action);
			break;
		}
	}
	
	/**
	 * Inititiase the DownloadService with the episode to be downloaded.
	 * @param _data the Bundle with the URL, name etc in.
	 * @return The index in the downloadServices array that the episode is in.
	 */
	private void initDownload(Bundle _data){
		//Get all the data needed for the download.
		final Bundle data = _data; 
		DownloadingEpisode newEpisode = new DownloadingEpisode();
		
		newEpisode.setRowID(data.getLong(EpisodeKeys._ID));
		newEpisode.setPodcastName(data.getString(PodcastKeys.NAME));
		newEpisode.setTitle(data.getString(EpisodeKeys.TITLE));
		newEpisode.setPodcastPath(PonyExpressApp.PODCAST_PATH + newEpisode.getPodcastName());
		newEpisode.setLink(data.getString(EpisodeKeys.URL));
		newEpisode.setSize(data.getInt(EpisodeKeys.SIZE));
		
		mQueue.add(newEpisode);
		PonyLogger.i(TAG, newEpisode.getTitle() + " queued");
		if (!mDownloaderAwake){
			mDownloaderAwake = true;
			beginLooperThread();
		}
		notifyEpisodeFragOfStart(QUEUED);
	}
	
	/**
	 * Starts a new thread to download the episode specified.
	 * @param index the index in the DownloadServices array that holds the 
	 * particular DownloadingEpisode.
	 */
	public void downloadEpisode(final int index) {
		
		//A new thread carrieds out each download.
		new Thread(new Runnable() {
			
			FileOutputStream outFile;			
			DownloadingEpisode episode = mEpisodes.get(index);
			
			@Override
			public void run() {
				PonyLogger.d(TAG, "Starting download thread");
				boolean IOe = false;
				final URL url = episode.getLink();
				final String podcastPath = episode.getPodcastPath();
				if (url != null && isSDCardWritable()){
					outFile = prepareForDownload(podcastPath, url);
					createNoMediaFile(podcastPath);

					if (mPonyExpressApp.getInternetHelper().checkConnectivity() 
							&& outFile != null){
						//Begin download
						mCurrentDownloads++;
						InputStream inFile = null;
						int totalDownloaded = 0;
						try {
							HttpURLConnection conn = Utils.openConnection(url);
							if (conn != null){
								inFile = conn.getInputStream();
							} else {
								IOe = true;
							}
						} catch (IOException e) {
							IOe = true;
						}
						if (!IOe){
							byte[] buffer = new byte[1024];
							int size = 0;

							PonyLogger.d(TAG, "Writing " + url.getFile());
							try {
								while ((size = inFile.read(buffer)) > 0 && !episode.downloadCancelled()) {
									outFile.write(buffer,0, size);
									totalDownloaded  += size;
									mEpisodes.get(index).setDownloadProgress(totalDownloaded);
								}
								if (episode.downloadCancelled()){
									PonyLogger.d(TAG, "Podcast download cancelled.");
									//Delete partial download.
									Utils.deleteFile(mPonyExpressApp, episode.getRowID(), episode.getPodcastName());
								}
								else {
									PonyLogger.i(TAG,"Podcast written to SD card.");
									episode.setDownloadCompleted(true);
									PonyExpressDbAdaptor dbHelper = mPonyExpressApp.getDbHelper();
									final long row_id = episode.getRowID();
									dbHelper.update(row_id, EpisodeKeys.DOWNLOADED,"true");
									try{
										dbHelper.update(row_id, EpisodeKeys.DURATION,
												dbHelper.getDuration(row_id));
									} catch (SQLiteException e) {
										PonyLogger.e(TAG, "Error recording duration", e);
									}
									
								}
								//Decrease mCurrentDownloads which will kill 
								//the notifications of it getes to <1
								mCurrentDownloads--;
							
							} catch (IOException e) {
								//Error downloading so reset the Activity
								PonyLogger.e(TAG, "Error reading/writing to file.", e);
								setDownloadFailed(index);
								clearEpisodeFromPlaylist(index);
							}

						} else {
							PonyLogger.e(TAG, "No Internet Connection or outFile error.");
							setDownloadFailed(index);
							clearEpisodeFromPlaylist(index);
							mHandler.post(new Runnable(){

								@Override
								public void run() {
									Toast.makeText(getApplicationContext(), R.string.no_internet_connection,
										Toast.LENGTH_SHORT).show();
								
								}
							
							});
						}
					}
				}
			}			
		}).start();		
	}
	
	/**
	 * Notify the EpisodeFrag that the download has commenced and that it can start 
	 * displaying the progress.
	 * @param index the index in the DownloadServices array that holds the 
	 * particular DownloadingEpisode.
	 */
	private void notifyEpisodeFragOfStart(final int index){
		Intent intent = new Intent(DOWNLOADING);
		intent.putExtra("index", index);
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mPonyExpressApp);
		lbm.sendBroadcast(intent);
	}
	
	/**
	 * If the download fails due to no connectivity or some other error calling 
	 * this allows the PlayerActivity to react to the error and reset the UI.
	 */
	private void setDownloadFailed(int index){
		mEpisodes.get(index).setDownloadFailed();
		mCurrentDownloads--;
	}

	/**
	 * Checks that the SD card is mounted with read/write access and that 
	 * we can write to the correct path. 
	 * @return true if writable.
	 */
	private boolean isSDCardWritable() {
		final String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)){
			PonyLogger.d(TAG, "SD Card is mounted");
			mRoot = Environment.getExternalStorageDirectory();
			if (mRoot.canWrite()){
				PonyLogger.d(TAG, "Can Write to SD card.");
				return true;
			} else {
				PonyLogger.d(TAG, "SD Card is not writable.");
				PendingIntent intent = PendingIntent.getActivity(mPonyExpressApp, 
						0, new Intent(), 0);
				int icon = R.drawable.stat_notify_error;
				Notification notification = new Notification(
						icon, null,
						System.currentTimeMillis());
				notification.setLatestEventInfo(mPonyExpressApp, 
						getText(R.string.app_name), getString(R.string.cannot_write), intent);
				//FIXME should this use a handler to notify the UI thread?
				mNM.notify(NOTIFY_ERROR_ID, notification);
			}	
		}
		return false;
	}
	
	/**
	 * Creates the path needed to save the files.
	 */
	private FileOutputStream prepareForDownload(String podcastPath, URL url) {
		File path = new File(mRoot, podcastPath);
		path.mkdirs();
		
		//Split filename from path url.
		final String filename_path = url.getFile();
		final String filename = filename_path.substring(filename_path.lastIndexOf('/'));
		FileOutputStream outFile = null;
		try {
			outFile = new FileOutputStream(new File(path,filename));
		} catch (FileNotFoundException e) {
			PonyLogger.e(TAG, "Cannot open FileOutputStream for writing.",e);
		}
		return outFile;
	}
	
	/**
	 * Creates a nomedia file if it doesn't exist. This file causes 
	 * the media scanner to ignore the podcast files. 
	 */
	private void createNoMediaFile(String podcastPath) {
		final String path = mRoot + podcastPath + "/";
		File noMedia = new File(path,NO_MEDIA_FILE);
		if (!noMedia.exists()){
			FileOutputStream writeFile = null;
			try {
				writeFile = new FileOutputStream(noMedia);
			} catch (FileNotFoundException e) {
				PonyLogger.e(TAG, "Cannot create .nomedia file", e);
			}
			try {
				writeFile.write(new byte[1]);
			} catch (IOException e) {
				PonyLogger.e(TAG, "Cannot create .nomedia file", e);
			}
		}
		
	}
	
	/**This thread follows mCurrentDownloads and mQueue while the 
	* service is active. It moves episodes from the queue to 
	* mCurrentDownloads when necessary.
	* It also displays notifications while downloading.  
	*/
	private void beginLooperThread() {
		new Thread(new Runnable() {
			@Override
			public void run() {				
				while (mDownloaderAwake){
					//if room in mCurrentDownloads and items in mQueue
					//move to mEpisodes.
					if (mCurrentDownloads < MAX_CONCURRENT_DOWNLOADS && !mQueue.isEmpty()){
						final DownloadingEpisode episode = mQueue.poll();
						mEpisodes.add(episode);
						final int index = mEpisodes.indexOf(episode);
						downloadEpisode(index);
						PonyLogger.i(TAG, episode.getTitle() + " downloading");
						notifyEpisodeFragOfStart(index);
					}
					try {
						//Sleep here to give mCurrentDownloads a chance to increment
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						mNM.cancel(NOTIFY_ID);
						return;
					}
					
					if (mCurrentDownloads + mQueue.size() > 0){
						updateNotification();
					} else {
						//If auto-playlist is on recompile the playlist with new downloads
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						boolean auto_playlist = prefs.getBoolean(
								getString(R.string.auto_playlist_key), false);
						if (auto_playlist){
							mPonyExpressApp.getDbHelper().recompileAutoPlaylist();
						}

						mNM.cancel(NOTIFY_ID);
						PonyLogger.d(TAG, "Downloader going to sleep");
						mDownloaderAwake = false;
						stopSelf();
					}

				}
			}
		}).start();
	}
	
	private void updateNotification(){
		//Start the download overview when selecting the notification
		Intent i = new Intent(mPonyExpressApp, DownloadOverviewActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent intent = PendingIntent.getActivity(mPonyExpressApp, 0, i, 0);
		
		CharSequence text = "";
		
		if (mCurrentDownloads == 1){
			text = getText(R.string.downloading_episode);
		} else {
			text = Integer.toString(mCurrentDownloads) + " " 
			+ getText(R.string.downloading_episodes);
		}
		
		Notification notification = new Notification(
				R.drawable.pony_icon, null,
				System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.number = mCurrentDownloads + mQueue.size();
		notification.setLatestEventInfo(mPonyExpressApp, 
				getText(R.string.app_name), text, intent);
		
		mNM.notify(NOTIFY_ID, notification);
	}
	
	public void cancelDownload(int index) {
		mEpisodes.get(index).setDownloadCancelled();
	}
		

	public int isEpisodeDownloading(String PodcastTitle) {
		// Iterate through the array to find podcastname and return index or
		// NOT_DOWNLOADING or QUEUED
		int index = NOT_DOWNLOADING;
		if (mEpisodes.isEmpty()) return index;
		if (isEpisodeQueued(PodcastTitle)){
			index = QUEUED;
			return index;
		}
		
		for (DownloadingEpisode episode:mEpisodes){
			if (episode.getTitle().equals(PodcastTitle)){
				if (episode.isEpisodeDownloading()){
					index = mEpisodes.indexOf(episode);
				}else{
					PonyLogger.d(TAG, "Episode: " + PodcastTitle + " not downloading!");
				}
			}
		}
		return index;
	}
	public boolean isEpisodeQueued(String podcastTitle){
		if (mQueue.isEmpty()) return false;
		
		for (DownloadingEpisode episode:mQueue){
			if (episode.getTitle().equals(podcastTitle)){
				return true;
			}
		}
		return false;
	}

	public double getProgress(final int index) {
		// look up podcast in array and return percent progress
		final DownloadingEpisode episode = mEpisodes.get(index);
		final boolean downloaded = mPonyExpressApp.getDbHelper().isEpisodeDownloaded(episode.getRowID());
		if (downloaded){
			return 100.0;
		}else{
			return episode.getDownloadPercent();
		}
	}

	public boolean checkForDownloadError(final int index) {
		return mEpisodes.get(index).getDownloadFailed();
	}

	public void resetDownloadError(final int index) {
		mEpisodes.get(index).resetDownloadFailed();	
	}


	public void removeFromQueue(String episode_title) {
		Iterator<DownloadingEpisode> iterator = mQueue.iterator();
		while (iterator.hasNext()){
			final DownloadingEpisode episode = iterator.next();
			if (episode.getTitle().equals(episode_title)){
				iterator.remove();
			}
		}
		
	}


	public boolean isDownloading() {
		if (mCurrentDownloads + mQueue.size() > 0) return true;
		else return false;
	}


	public List<DownloadingEpisode> getDownloadingEpisodes() {
		if (mCurrentDownloadsList == null){
			mCurrentDownloadsList = new ArrayList<DownloadingEpisode>();
		}
		mCurrentDownloadsList.clear();

		for (DownloadingEpisode episode:mEpisodes){
			if (episode.isEpisodeDownloading() && !episode.downloadCancelled()){
				mCurrentDownloadsList.add(episode);
			}
		}
		for (DownloadingEpisode episode:mQueue){
			mCurrentDownloadsList.add(episode);
		}
		return mCurrentDownloadsList;
	}


	public void cancelDownload(String title) {
		//Remove from queue if in it
		removeFromQueue(title);
		//Cancel download if downloading
		for (DownloadingEpisode episode : mEpisodes){
			if (episode.getTitle().equals(title)){
				episode.setDownloadCancelled();
			}
		}
		
	}
	
	private void clearEpisodeFromPlaylist(int index){
		final String podcast_name = mEpisodes.get(index).getPodcastName();
		final String episode_title = mEpisodes.get(index).getTitle();
		if (mPonyExpressApp.getDbHelper().episodeInPlaylist(podcast_name, episode_title)){
			mPonyExpressApp .getDbHelper().removeEpisodeFromPlaylist(podcast_name, episode_title);
		}
	}
}
