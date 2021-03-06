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

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.sixgun.ponyexpress.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

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
     * Note: the returned connection should be disconnected after use.
     * @return an HttpURLConnection from the feedUrl
     */
    protected HttpURLConnection getConnection() {
    	int attempts = 0;
    	HttpURLConnection conn = null;
		//try to connect to server a maximum of five times
    	do {
    		try{
    			conn = Utils.openConnection(mFeedUrl);
    			attempts++;
    		} catch (SocketTimeoutException ste) {
    			PonyLogger.e(TAG, "Server timed out from getReponseCode()", ste);
    			if (conn != null){
    				conn.disconnect();
    			}
    			return null;
    		} 
		} while (conn == null && attempts < 5);
    	if (conn != null){
    		return conn;
    	} else { NotifyError(""); }
		return null;
    }
    
    /**
     * Sends an error notification.
     * The default error message is "There was an error getting a feed" which is used
     * if an empty string is passed in.
     * @param error_message
     */
    protected void NotifyError(String error_message) {
    	//Send a notification to the user telling them of the error
		//This uses an empty intent because there is no new activity to start.
		PendingIntent intent = PendingIntent.getActivity(mCtx.getApplicationContext(), 
				0, new Intent(), 0);
		NotificationManager notifyManager = 
			(NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.stat_notify_error;
		
		CharSequence text = mCtx.getText(R.string.feed_error);
		if (!error_message.equals("")){
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
