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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpStatus;
import org.sixgun.ponyexpress.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.Gravity;

/**
 * Utility class with general utility methods.
 *
 */
public class Utils {
	
	private static final String TAG = "PonyExpressUtils";
	private static Method mBitmapDrawable;

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
		
		return String.format("%d:%02d:%02d", hours,minutes,seconds);
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
	 * @param the URL
	 * @return the connection if connection can be made or null otherwise.
	 */
	static public HttpURLConnection checkURL(URL _url){
		HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) _url.openConnection();
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
		}
		return path.delete();
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
	 * Creates a BitmapDrawable of a specific size from the given album art.
	 * Uses the deprecated BitmapDrawable constructor on android 1.5.
	 * @param res
	 * @param art
	 * @param height
	 * @param width
	 * @return BitmapDrawable
	 */
	static public BitmapDrawable createBackgroundFromAlbumArt(Resources res, Bitmap art, int height, int width){
		Bitmap new_image;
		if (height >  width){
			new_image = Bitmap.createScaledBitmap(art, height, height, true);
		} else {
			new_image = Bitmap.createScaledBitmap(art, width, width, true);	
		}
		//First use deprecated Ctor that will work on android 1.5
		BitmapDrawable new_background = new BitmapDrawable(new_image);
		//If on a newer android use the better Ctor.
		if (mBitmapDrawable !=null){
			try {
				new_background = BitmapDrawable(res, new_image);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Illegal arguments to BitmapDrawable" , e);
			} catch (IOException e) {
				Log.e(TAG, "IOException calling BitmapDrawable", e);
			}
		}
		new_background.setGravity(Gravity.LEFT|Gravity.TOP);
		new_background.setAlpha(80);
		return new_background;
	}
	/**
	 * Method to determine if we need to use the deprcated BitmapDrawable constructor.
	 */
	static private void initCompatibility(){
		try {
	           mBitmapDrawable = BitmapDrawable.class.getMethod(
	                   "BitmapDrawable", new Class[] { Resources.class, Bitmap.class } );
	       } catch (NoSuchMethodException nsme) {
	           /* failure, must be older device */
	       }
	}
	
	static {
		initCompatibility();
	}
	
	private static BitmapDrawable BitmapDrawable(Resources res, Bitmap image) throws IllegalArgumentException, IOException{
		BitmapDrawable bd = null;
		try {
			return (BitmapDrawable) mBitmapDrawable.invoke(null, res, image);
		} catch (InvocationTargetException ite) {
			/* unpack original exception when possible */
			Throwable cause = ite.getCause();
			if (cause instanceof IOException) {
				throw (IOException) cause;
			} else if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				/* unexpected checked exception; wrap and re-throw */
				throw new RuntimeException(ite);
			} 
		} catch (IllegalAccessException e) {
			Log.e(TAG, "unexpected " + e);
		}
		
		return bd;
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
}
