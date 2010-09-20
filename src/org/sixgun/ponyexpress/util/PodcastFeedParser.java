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

import java.io.InputStream;

import org.sixgun.ponyexpress.Podcast;
import org.xml.sax.Attributes;

import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

/**
 * Parses a Podcast RSS feed and extracts general information about the podcast
 * such as title and almum art URI.
 *
 */
public class PodcastFeedParser extends BaseFeedParser {

	static final String MEDIA_NS = "http://search.yahoo.com/mrss/";
	// names of the XML tags
    static final String NAME = "title";
    static final String ALBUM_ART_URL = "thumbnail";
	private static final String TAG = "Pony/PodcastFeedParser";
    private String mFeedUrl;
    
    
	protected PodcastFeedParser(String feedUrl) {
		super(feedUrl);
		mFeedUrl = feedUrl;
		Log.d(TAG, "Feed is from: " + feedUrl);
		
	}
	
	/**
	 * Parses the RSS feed from the InputStream and extracts text elements and 
	 * url attributes from them.
	 * @return a List of Episodes.
	 */
	public Podcast parse() {
		final Podcast new_podcast = new Podcast();
				
		//Set up the required elements.
		RootElement root = new RootElement("rss");
		Element channel = root.requireChild("channel");
		
		/*Set up the ElementListeners.
		 * 
		 */
		
		//Store the Feed URL
		new_podcast.setFeed_Url(mFeedUrl);
		
		//This listener catches the name.
		channel.getChild(NAME).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				Log.d(TAG,"Podcast is: " + body);
				new_podcast.setName(body);
			}
		});
		//This Listener catches the album art url of the podcast.
		channel.getChild(MEDIA_NS, ALBUM_ART_URL).setStartElementListener(new StartElementListener() {
			
			@Override
			public void start(Attributes attributes) {
				String url = attributes.getValue("", "url");
				Log.d(TAG,"Podcast art from: " + url);
				new_podcast.setArt_Url(url);				
			}
		});
		
		//Finally, now the listeners are set up we can parse the XML file.
		
		InputStream istream = getInputStream();	    
		if (istream != null){
			try {
				Xml.parse(istream, Xml.Encoding.UTF_8, 
						root.getContentHandler());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}			
		return new_podcast;
		
	}

}