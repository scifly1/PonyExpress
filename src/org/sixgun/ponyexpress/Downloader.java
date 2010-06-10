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
package org.sixgun.ponyexpress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

/**
 *Service that handles the downloading and saving of podcasts.
 *It queues multiple calls to startService from the calling Activity 
 *and do them one at a time in a worker thread.  When it is finished all
 * its work it stops itself.
 */
public class Downloader extends IntentService {

	private static final String TAG = "PonyExpress Downloader";
	private static final String PODCAST_PATH = "/Android/data/org.sixgun.PonyExpress/files";
	private PonyExpressApp mPonyExpressApp;
	private PonyExpressDbAdaptor mDbHelper;
	private long mRow_ID;
	private URL mUrl;
	private File mRoot;
	private FileOutputStream mOutFile;
	private InputStream mInFile;
	private String mFilename;

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
		Log.d(TAG,"Downloader started");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle data = intent.getExtras();
		mUrl = getURL(data.getString(EpisodeKeys.URL));
		mRow_ID= data.getLong(EpisodeKeys._ID);
		if (mUrl != null && isSDCardWritable()){
			prepareForDownload();
			if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
				downloadFile();
			} else {
				Log.d(TAG, "No Internet Connection.");
				//TODO NM
			}
		}
	}
	
	/**
	 * Parse the url string to a URL type.
	 * @param url string from the Intent.
	 * @return URL object.
	 */
	private URL getURL(String _url) {
		URL url;
		try {
			url = new URL(_url);
		} catch (MalformedURLException e) {
			Log.e(TAG, "Episode URL badly formed.", e);
			//TODO NM
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
		File path = new File(mRoot, PODCAST_PATH);
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
			}//TODO NM
			Log.d(TAG,"Podcast written to SD card.");
			mDbHelper.update(mRow_ID, EpisodeKeys.DOWNLOADED,"true");
		} catch (IOException e) {
			Log.e(TAG, "Error reading/writing to file.", e);
			//TODO NM
		}
	}
	}
	
