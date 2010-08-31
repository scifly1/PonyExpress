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
import java.net.MalformedURLException;
import java.net.URL;

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.util.PonyExpressDbAdaptor;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

/**
 *Service that handles the downloading and saving of podcasts.
 *It queues multiple calls to startService from the calling Activity 
 *and does them one at a time in a worker thread.  When it is finished all
 * its work it stops itself.
 */
public class Downloader extends IntentService {

	private static final String TAG = "PonyExpress Downloader";
	private static final String NO_MEDIA_FILE = ".nomedia";
	private NotificationManager mNM;
	private static final int NOTIFY_ID = 1;
	private PonyExpressApp mPonyExpressApp;
	private PonyExpressDbAdaptor mDbHelper;
	private long mRow_ID; //Needed in order to update db.
	private URL mUrl;
	private File mRoot;
	private FileOutputStream mOutFile;
	private InputStream mInFile;
	private String mFilename;
	private int mSize;
	private volatile int mTotalDownloaded = 0;

	public Downloader() {
		super("Downloader");
	}

	/* (non-Javadoc)
	 * @see android.app.IntentService#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		//Get the application context.  This must be done here and not in the constructor.
		mPonyExpressApp = (PonyExpressApp) this.getApplication();
		mDbHelper = mPonyExpressApp.getDbHelper();
		//Set up notification system.
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Log.d(TAG,"Downloader started");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle data = intent.getExtras();
		mUrl = getURL(data.getString(EpisodeKeys.URL));
		mRow_ID = data.getLong(EpisodeKeys._ID);
		mSize = data.getInt(EpisodeKeys.SIZE);
		if (mUrl != null && isSDCardWritable()){
			prepareForDownload();
			createNoMediaFile();
			if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
				showNotification();
				downloadFile();
			} else {
				Log.d(TAG, "No Internet Connection.");
			}
		}
	}
	

	/* (non-Javadoc)
	 * @see android.app.IntentService#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		mNM.cancel(NOTIFY_ID);
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
		//TODO NM
		return false;
	}
	
	/**
	 * Creates the path needed to save the files.
	 */
	private void prepareForDownload() {
		File path = new File(mRoot, PonyExpressApp.PODCAST_PATH);
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
		final String path = mRoot + PonyExpressApp.PODCAST_PATH + "/";
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
			mDbHelper.update(mRow_ID, EpisodeKeys.DOWNLOADED,"true");
		} catch (IOException e) {
			Log.e(TAG, "Error reading/writing to file.", e);
			//TODO NM
		}
	}

	/**
	 * Shows a notification in the status bar when downloading an episode.
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
					notification.setLatestEventInfo(mPonyExpressApp, 
							getText(R.string.app_name), text, intent);
					
					mNM.notify(NOTIFY_ID, notification);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						return;
					}
				} while (mTotalDownloaded < mSize);
			}
		}).start();
	}

}
	
