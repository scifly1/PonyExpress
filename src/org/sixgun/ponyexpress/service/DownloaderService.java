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

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.util.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;


public class DownloaderService extends Service {


    protected static final int NOTIFY_ID = 1;
    private final IBinder mBinder = new DownloaderServiceBinder();
    private int mDownloadCounter;
	private static final String TAG = "PonyExpress Downloader";
	private static final String NO_MEDIA_FILE = ".nomedia";
	private PonyExpressApp mPonyExpressApp;
	private Bundle mData;
	private File mRoot;
	protected NotificationManager mNM;
	
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
	}

	
	public void downloadEpisode(final Bundle _data) {
		final Bundle data = _data; 
		mDownloadCounter += 1;
		
		final String podcastName = data.getString(PodcastKeys.NAME);
		final String podcastPath = PonyExpressApp.PODCAST_PATH + podcastName;
		final URL url = Utils.getURL(mData.getString(EpisodeKeys.URL));
		final int totalSize = mData.getInt(EpisodeKeys.SIZE);
		final long row_ID = mData.getLong(EpisodeKeys._ID);
		
		final Handler handler = new Handler(){

			/* (non-Javadoc)
			 * @see android.os.Handler#handleMessage(android.os.Message)
			 */
			@Override
			public void handleMessage(Message msg) {
				//TODO Handle the totalDownloaded int that the message is carrying
				//in what and assign to the correct DownloadingEpisode object via an 
				// ArrayList of all DownloadingEpisodes.
				
			}
			
		};
		
		new Thread(new Runnable() {
			
			FileOutputStream outFile;
			final int notifyID = mDownloadCounter;
			
			
			@Override
			public void run() {
				if (url != null && isSDCardWritable()){
					outFile = prepareForDownload(podcastPath, url);
					createNoMediaFile(podcastPath);
					
					if (mPonyExpressApp.getInternetHelper().checkConnectivity() 
							&& outFile != null){
						//Begin download
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
								updateNotification(notifyID, totalDownloaded, totalSize);
								handler.sendMessage(Message.obtain(handler, totalDownloaded));
							}
							Log.d(TAG,"Podcast written to SD card.");
							mPonyExpressApp.getDbHelper().update(podcastName, row_ID, EpisodeKeys.DOWNLOADED,"true");
						} catch (IOException e) {
							Log.e(TAG, "Error reading/writing to file.", e);
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
	
	private void updateNotification(final int notifyID, int totalDownloaded, int size) {
		//This uses an empty intent because there is no new activity to start.
		PendingIntent intent = PendingIntent.getActivity(mPonyExpressApp, 
				0, new Intent(), 0);
		
		int icon = R.drawable.sixgunicon0;
		String progress = "";
		double percent = 0.0;
		CharSequence text = "";
		
		percent = totalDownloaded/(double)size * 100;
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
		
		mNM.notify(notifyID, notification);
	}
	
}
