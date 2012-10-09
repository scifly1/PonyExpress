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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import org.sixgun.ponyexpress.Dent;
import org.sixgun.ponyexpress.R;

import android.content.Context;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Log;
import android.util.Xml;

/**
 * DentParser implements a basic Android SAX parser.  It finds the XML tags in the RSS
 * feed that is returned by this.getInputStream() and extracts the text elements
 * or attributes from them using ElementListeners.
 */
public class DentParser extends BaseFeedParser {

	// names of the XML tags
	static final String STATUS = "status";
	static final String TEXT = "text";
	static final String USER = "user";
	static final String NAME = "name";
	static final String SCREEN_NAME = "screen_name";
	static final String AVATAR = "profile_image_url";
	protected static final String TAG = "DentParser";
	private static final String NS = "";
	
	
	public DentParser(Context ctx, String feedUrl) {
		super(ctx,feedUrl);
	}
	
	/**
	 * Parses the RSS feed from the InputStream and extracts text elements and 
	 * url attributes from them.
	 * @return a List of Episodes.
	 */
	public ArrayList<Dent> parse() {
		final Dent new_dent = new Dent();
		final ArrayList<Dent> dents = new ArrayList<Dent>();
		
		//Set up the required elements.
		RootElement root = new RootElement(NS,"statuses");
		Element status = root.getChild(NS,STATUS);  
				
		/*Set up the ElementListeners.
		 * The first listens for the end of the entry element, which marks the end
		 * of each dent in the feed.  At this point the Dent is added
		 * the list as it should have had all its details recorded by the other 
		 * listeners.
		 */
		status.setEndElementListener(new EndElementListener(){
            public void end() {
                dents.add(new Dent(new_dent));
            }
		});
		//This listener catches the title.
		status.getChild(NS,TEXT).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				Log.d(TAG,"Found title: " + body);
				new_dent.setTitle(body);
			}
		});
		//This listener catches the user.
		status.getChild(NS,USER).getChild(NS,NAME).setEndTextElementListener(
				new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				Log.d(TAG,"Found user: " + body);
				new_dent.setUser(body);
			}
		});
		//This listener catches the user's screen name.
		status.getChild(NS,USER).getChild(NS,SCREEN_NAME).setEndTextElementListener(
				new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				Log.d(TAG,"Found user's screen name: " + body);
				new_dent.setUserScreenName(body);
			}
		});
		
		//This listener catches the user's avatar URL.
		status.getChild(NS,USER).getChild(NS,AVATAR).setEndTextElementListener(
				new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				Log.d(TAG,"Found Avatar URL: " + body);
				new_dent.setAvatarURI(body);
			}
		});
		
		
		//Finally, now the listeners are set up we can parse the XML file.
		
		HttpURLConnection conn = getConnection();
		//To debug with test feeds comment out the above line and uncomment the next line.
	    //InputStream istream = mCtx.getResources().openRawResource(R.raw.dentfeed);
		InputStream istream = null;
		
		try {
			if (conn != null){
				istream = new BufferedInputStream(conn.getInputStream());
			}
		} catch (IOException e) {
			Log.e(TAG, "Error reading feed from " + mFeedUrl, e);
			NotifyError("");
		} 
		try {
			if (istream != null){
				Xml.parse(istream, Xml.Encoding.UTF_8, 
						root.getContentHandler());
				istream.close();
			} else {
				//If connection errors tell user
				Dent no_dents = new Dent();
				no_dents.setTitle(mCtx.getString(R.string.conn_err_query_failed));
				dents.add(no_dents);
			}
		} catch (Exception e) {
				NotifyError("");
		} finally {
			if (conn != null){
				conn.disconnect();
			}
		}
		
		return dents;
		
	}
}
