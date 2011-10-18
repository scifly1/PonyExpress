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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpStatus;
import org.sixgun.ponyexpress.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * BaseFeedParser is an abstract class that takes a url and its getInputStream() 
 * method returns an InputStream object from that url.
 */

public abstract class BaseFeedParser {
    
    private static final String TAG = "BaseFeedParser";
	private static final int NOTIFY_ID = 6;
    protected Context mCtx;
	protected URL mFeedUrl;
	
    /**
     * Constructor - takes the URL of the RSS feed to be parsed.
     * @param feedUrl
     */
    protected BaseFeedParser(Context ctx, String feedUrl){
    	try {
            mFeedUrl = new URL(feedUrl);
        } catch (MalformedURLException e) {
            NotifyError("");
            mFeedUrl = null;
        }
        mCtx = ctx;
    }
    
	/**
     * Opens a connection to feedUrl.
     * 
     * @return an InputStream from the feedUrl
     */
    protected InputStream getInputStream() {
    	InputStream istream = null;
    	int attempts = 0;
    	URLConnection conn;
		//try to connect to server a maximum of five times
    	do {
			conn = openConnection();
			attempts++;
		} while (conn == null && attempts < 5);
    	//if connected get Inputstream
    	if (conn != null){
    		try {
    			istream = conn.getInputStream();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
		return istream;
    }
    
    private HttpURLConnection openConnection(){
    	HttpURLConnection conn;
		try {
			conn = (HttpURLConnection) mFeedUrl.openConnection();
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

    protected void NotifyError(String error_message) {
    	//Send a notification to the user telling them of the error
		//This uses an empty intent because there is no new activity to start.
		PendingIntent intent = PendingIntent.getActivity(mCtx.getApplicationContext(), 
				0, new Intent(), 0);
		NotificationManager notifyManager = 
			(NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.stat_notify_error;
		
		CharSequence text = mCtx.getText(R.string.feed_error);
		if (error_message != ""){
			text = error_message;
		}

		Notification notification = new Notification(
				icon, null,
				System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(mCtx.getApplicationContext(), 
				mCtx.getText(R.string.app_name), text, intent);
		notifyManager.notify(NOTIFY_ID,notification);
		
	}
    
    abstract public Object parse();
    
}
