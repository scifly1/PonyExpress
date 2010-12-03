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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import org.sixgun.ponyexpress.DownloadingEpisode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;


public class DownloaderService extends Service {


    private final IBinder mBinder = new DownloaderServiceBinder();
	private static final String TAG = "PonyExpress Downloader";
	private static final String NO_MEDIA_FILE = ".nomedia";
	private static final int notifyID = 1;
	private PonyExpressApp mPonyExpressApp;
	//Do not remove episodes from mEpisodes as you'll change the index 
	//of other episodes that may be being accessed.
	private ArrayList<DownloadingEpisode> mEpisodes;
	private File mRoot;
	protected NotificationManager mNM;
	volatile private int mCurrentDownloads;
	private boolean mDownloaderAwake = false;
	
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
		return mBinder;
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Downloader Service Started");
		mPonyExpressApp = (PonyExpressApp)getApplication();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mEpisodes = new ArrayList<DownloadingEpisode>();
		mDownloaderAwake = true;
		beginNotifications();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDownloaderAwake = false;
	}
	
	public void downloadEpisode(final Bundle _data) {
		//Get all the data needed for the download.
		final Bundle data = _data; 
		DownloadingEpisode newEpisode = new DownloadingEpisode();
		
		newEpisode.setRowID(data.getLong(EpisodeKeys._ID));
		newEpisode.setPodcastName(data.getString(PodcastKeys.NAME));
		newEpisode.setTitle(data.getString(EpisodeKeys.TITLE));
		newEpisode.setPodcastPath(PonyExpressApp.PODCAST_PATH + newEpisode.getPodcastName());
		newEpisode.setLink(data.getString(EpisodeKeys.URL));
		newEpisode.setSize(data.getInt(EpisodeKeys.SIZE));
		
		mEpisodes.add(newEpisode);
		
		final int index = mEpisodes.indexOf(newEpisode);
		//A new thread carrieds out each download.
		new Thread(new Runnable() {
			
			FileOutputStream outFile;			
			DownloadingEpisode episode = mEpisodes.get(index);
			
			@Override
			public void run() {
				final URL url = episode.getLink();
				final String podcastPath = episode.getPodcastPath();
				if (url != null && isSDCardWritable()){
					outFile = prepareForDownload(podcastPath, url);
					createNoMediaFile(podcastPath);

					if (mPonyExpressApp.getInternetHelper().checkConnectivity() 
							&& outFile != null){
						//Begin download
						mCurrentDownloads++;
						InputStream inFile;
						int totalDownloaded = 0;
						try {
							inFile = url.openStream();
						} catch (IOException e) {
							// TODO Improve this error handling.  Check network up etc.. 
							throw new RuntimeException(e);
						}
						byte[] buffer = new byte[1024];
						int size = 0;

						Log.d(TAG,"Writing " + url.getFile());
						try {
							while ((size = inFile.read(buffer)) > 0 ) {
								outFile.write(buffer,0, size);
								totalDownloaded  += size;
								mEpisodes.get(index).setDownloadProgress(totalDownloaded);
							}
							Log.d(TAG,"Podcast written to SD card.");
							//Decrease mCurrentDownloads which will kill 
							//the notifications of it getes to <1
							mCurrentDownloads--;
							mPonyExpressApp.getDbHelper().update(episode.getPodcastName(), 
									episode.getRowID(), EpisodeKeys.DOWNLOADED,"true");
						} catch (IOException e) {
							//Error downloading so reset the Activity
							Log.e(TAG, "Error reading/writing to file.", e);
							mEpisodes.get(index).setDownloadFailed();
							mCurrentDownloads--;
						}

					} else {
						Log.d(TAG, "No Internet Connection or outFile error.");
					}
				}
			}			
		}).start();		
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
			// TODO Improve Error handling.
			Log.e(TAG, "Cannot open FileOutputStream for writing.",e);
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
				Log.e(TAG, "Cannot create .nomedia file", e);
			}
			try {
				writeFile.write(new byte[1]);
			} catch (IOException e) {
				Log.e(TAG, "Cannot create .nomedia file", e);
			}
		}
		
	}
	/**This thread  follows mCurrentDownloads while the 
	* service is active. It Displays notifications while > 1.  
	*/
	private void beginNotifications() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				//This uses an empty intent because there is no new activity to start.
				PendingIntent intent = PendingIntent.getActivity(mPonyExpressApp, 
						0, new Intent(), 0);
				
				int icon;
				CharSequence text = "";
				int icon_counter = 0;
				
				while (mDownloaderAwake){
					if (mCurrentDownloads > 0){ 
						if (mCurrentDownloads == 1){
							text = getText(R.string.downloading_episode);
						} else {
							text = Integer.toString(mCurrentDownloads) + " " 
							+ getText(R.string.downloading_episodes);
						}
						
						switch (icon_counter) {
							case 0:
								icon = R.drawable.sixgunicon0;
								break;
							case 1:
								icon = R.drawable.sixgunicon1;
								break;
							case 2:
								icon = R.drawable.sixgunicon2;
								break;
							case 3:
								icon = R.drawable.sixgunicon3;
								break;
							case 4:
								icon = R.drawable.sixgunicon4;
								break;
							case 5:
								icon = R.drawable.sixgunicon5;
								break;
							case 6:
								icon = R.drawable.sixgunicon6;
								break;
							default:
								icon = R.drawable.sixgunicon0;
						}
						
						if (icon_counter > 5){
							icon_counter = 0;
						} else icon_counter++;
						
						Notification notification = new Notification(
								icon, null,
								System.currentTimeMillis());
						notification.flags |= Notification.FLAG_ONGOING_EVENT;
						notification.setLatestEventInfo(mPonyExpressApp, 
								getText(R.string.app_name), text, intent);

						mNM.notify(notifyID, notification);
					} else {
						mNM.cancel(notifyID);
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						mNM.cancel(notifyID);
						return;
					}
				}
			}
		}).start();
	}
		

	public int isEpisodeDownloading(String PodcastTitle) {
		// Iterate through the array to find podcastname and return index or -1
		// if not downloading.
		int index = -1;
		if (mEpisodes.isEmpty()) return index;
		
		for (DownloadingEpisode episode:mEpisodes){
			if (episode.getTitle().equals(PodcastTitle)){
				if (episode.getDownloadProgress() < episode.getSize()){
					index = mEpisodes.indexOf(episode);
				}else{
					Log.d(TAG, "Episode: " + PodcastTitle + " not downloading!");
				}
			}
		}
		return index;
	}

	public double getProgress(final int index) {
		// look up podcast in array and return percent progress
		final DownloadingEpisode episode = mEpisodes.get(index);
		final int size = episode.getSize();
		final int progress = episode.getDownloadProgress();
		double percent = progress/(double)size * 100; 
		return percent;
	}

	public boolean checkForDownloadError(int index) {
		return mEpisodes.get(index).getDownloadFailed();
	}

	public void resetDownloadError(int index) {
		mEpisodes.get(index).resetDownloadFailed();
		
	}
	
}
