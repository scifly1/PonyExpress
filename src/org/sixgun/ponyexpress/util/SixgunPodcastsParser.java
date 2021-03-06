/*
 * Copyright 2011 Paul Elms
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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.sixgun.ponyexpress.Podcast;

import android.content.Context;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;


public class SixgunPodcastsParser extends BaseFeedParser {

	// names of the XML tags
	static final String PODCAST = "podcast";
    static final String FEED_URL = "podcast_url";
    
    static final String TAG = "EpisodeFeedParser";
    
    
	public SixgunPodcastsParser(Context ctx, String feedUrl) {
		super(ctx, feedUrl);
	}

	@Override
	public List<Podcast> parse() {
		final Podcast new_podcast = new Podcast();
		final List<Podcast> podcasts = new ArrayList<Podcast>();
		
		//Set up the required elements.
		RootElement root = new RootElement("podcast_feeds");
		Element podcast = root.requireChild(PODCAST);
		
		/*Set up the ElementListeners.
		 * The first listens for the end of the item element, which marks the end
		 * of each Podcasts details in the XML.  At this point the Podcast is added
		 * the list as it should have had all its details recorded by the other 
		 * listeners.
		 */
		podcast.setEndElementListener(new EndElementListener(){
            public void end() {
                podcasts.add(new Podcast(new_podcast));
            }
		});
		
		//This listener catches the feed URL.
		podcast.getChild(FEED_URL).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_podcast.setFeedUrl(Utils.getURL(body));
			}
		});
		
		//Finally, now the listeners are set up we can parse the XML file.
				
		HttpURLConnection conn = getConnection();
		//To debug with test.xml comment out the above line and uncomment the next line.
	    //InputStream istream = mCtx.getResources().openRawResource(R.raw.test);
		InputStream istream = null;
		try {
			if (conn != null){
				istream = new BufferedInputStream(conn.getInputStream());
			}
		} catch (IOException e) {
			PonyLogger.e(TAG, "Error reading feed from " + mFeedUrl, e);
			NotifyError("");
		}
		try {
			if (istream != null){
				Xml.parse(istream, Xml.Encoding.UTF_8, 
						root.getContentHandler());
				istream.close();
			}
		}catch (AssertionError e){ //xml.parse repacks SocketTimeoutException as Assertion errors.
			Throwable cause = e.getCause();
			if (cause instanceof SocketTimeoutException){
				PonyLogger.e(TAG, "SocketTimeoutException caught parsing sixgun podcasts");
				NotifyError("");
				return null;
			}
		} catch (Exception e) {
			NotifyError("");
		} finally {
			if (conn != null){
				conn.disconnect();
			}
		}
		
		return podcasts;
	}
	
	

}
