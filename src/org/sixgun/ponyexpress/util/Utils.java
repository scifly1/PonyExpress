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
package org.sixgun.ponyexpress.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Locale;

import org.apache.http.HttpStatus;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

/**
 * Utility class with general utility methods.
 *
 */
public class Utils {
	
	private static final String TAG = "PonyExpressUtils";
	private static final int TIMEOUT = 20000;

	/**
	 * Formats a time in millisecods into a h:mm:ss string
	 * @param milliseconds
	 * @return h:mm:ss string
	 */
	static public String milliToTime(int milliseconds){
		int seconds = (milliseconds / 1000);
		int minutes = seconds / 60;
		int hours = minutes / 60;
		//Extract the remainder in each case to 
		//get the number of hours,minutes,seconds
		seconds = seconds % 60;
		minutes = minutes % 60;
		hours = hours % 60;
		
		return String.format(Locale.US,"%d:%02d:%02d", hours,minutes,seconds);
	}

	/**
	 * Parse the url string to a URL type.
	 * @param _url string from the Intent.
	 * @return URL object.
	 */
	static public URL getURL(String _url) {
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
	 * Checks that the given URL returns status 200 (OK)
	 * The returned connection  must be disconnected by the caller.
	 * @param the URL
	 * @return the connection if connection can be made or null otherwise.
	 * @throws SocketTimeoutException
	 */
	static public HttpURLConnection checkURL(URL _url) throws SocketTimeoutException{
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) _url.openConnection();
			conn.setRequestProperty("Connection", "close");
			conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
			Log.d(TAG,"Response code: " + conn.getResponseCode());
			//Check that the server responds properly
			if (conn.getResponseCode() != HttpStatus.SC_OK){
				return null;
			}
		} catch (IOException e) {
			return null;
		}
		return conn;
	}
	
	/**
	 * Strips words from the end of strings eg: "Ogg Feed"
	 * @param string String to strip from.
	 * @param to_strip string to strip.
	 */
	static public String stripper(String string, String to_strip){
		if (string.endsWith(to_strip)){
			String stripped = string.replace(to_strip,"");
			return stripped;
		}else {
			return string;
		}
	}
	
	/**
	 * Deletes a directory recursively.
	 * @param the direcory to delete.
	 */
	static public boolean deleteDir(File path){
		if (path.exists()){
			File[] files = path.listFiles();
			for (File file:files){
				if (file.isDirectory()){
					deleteDir(file);
				} else {
					file.delete();
				}
			}
			return path.delete();
		} else {
			Log.w(TAG, "Path for deletion could not be found!");
			return true;
		}
		
	}
	
	/** Deletes a file from the SD Card.
	 * 
	 * @param rowID of the file to be deleted from the database.
	 */
	static public boolean deleteFile(PonyExpressApp ponyApp, long rowID, String podcast_name) {
		File rootPath = Environment.getExternalStorageDirectory();
		File dirPath = new File(rootPath,PonyExpressApp.PODCAST_PATH);
		String filename = ponyApp.getDbHelper().getEpisodeFilename(rowID);
		//Add the podcast name as a folder under the PODCAST_PATH
		filename = podcast_name + filename;
		File fullPath = new File(dirPath,filename);
		return fullPath.delete();	
	}
	
	/**
	 * Creates the correct string for any number of unlistened episoded.
	 * @param ctx, the application context.
	 * @param unlistened, the number of unlistened episodes.
	 */
	static public String formUnlistenedString(Context ctx, int unlistened){
		String unlistenedString = "";
		switch (unlistened) {
		case 0: //no unlistened episodes
			break;
		case 1:
			unlistenedString = unlistened + " " + ctx.getString(R.string.new_episode);
			break;
		default: //more than 1 unlistened episode
			unlistenedString = unlistened + " " + ctx.getString(R.string.new_episodes);
		}
		
		return unlistenedString;
	}
	
	/**
	 * Checks to see if the SD card is mounted and writable.
	 * @return boolean
	 */
	public static boolean isSDCardWritable() {
		final String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)){
			Log.d(TAG, "SD Card is mounted");
			File file = Environment.getExternalStorageDirectory();
			if (file.canWrite()){
				Log.d(TAG,"Can Write to SD card.");
				return true;
			}
		}
		return false;
	}

	/**
	 * Writes the PonyExpress folders to the SD card.
	 */
	public static void writePodcastPath(){
		File path = new File(Environment.getExternalStorageDirectory() + PonyExpressApp.PODCAST_PATH);
		path.mkdirs();
	}

	/**
	 * Checks the remaining space on the SD card.
	 * @return the remaining space in megabytes.
	 */
	static public double checkSdCardSpace(){
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		double sdAvailSpace = (double)stat.getAvailableBlocks() *(double)stat.getBlockSize();
		//One megabyte equals 1,000,000 bytes.
		return sdAvailSpace / 1000000;
		
		
	}
	
	/** 
	 * Escapes single quotes inside a string literal and wraps the string in 
	 * single quotes so allowing for double quotes in the string for sqlite. 
	 */
	static public String handleQuotes(String string){
		String new_string =  string.replace("'", "''");
		return "'" + new_string + "'";
	}
	/**
	 * Records the log to a file in the PODCAST_PATH.
	 * Requires the READ_LOG permission in the AndroidManifest.
	 */
	static public void RecordLogToSDCard(){
		//FIXME this blocks when reading the log as logcat continues to
		//output so the read never ends.
		try {
		    File filename = File.createTempFile(PonyExpressApp.PODCAST_PATH , "logfile.txt"); 
		    Process process = Runtime.getRuntime().exec("logcat -v time");
		    InputStream is = process.getInputStream();
		    FileOutputStream fos = new FileOutputStream(filename);
		    byte[] buffer = new byte[1024];
		    int bytesRead;
		    while ((bytesRead = is.read(buffer)) > 0)
		    {
		        fos.write(buffer, 0, bytesRead);
		    }
		    fos.close();
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} 
	}

	
}
