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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.R;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.content.Context;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

/**
 * EpisodeFeedPArser implements a basic Android SAX parser.  It finds the XML tags in the RSS
 * feed that is returned by getInputStream() and extracts the text elements
 * or attributes from them using ElementListeners.  This gets the pub_date, descripotion, and title
 * length and url of each individual episode. 
 */
public class EpisodeFeedParser extends BaseFeedParser{
	
	private static final String CONTENT_NS = "http://purl.org/rss/1.0/modules/content/";
	// names of the XML tags
    private static final String PUB_DATE = "pubDate";
    private static final String CONTENT = "enclosure";
    private static final String DESCRIPTION = "description";
    private static final String ENCODED = "encoded";
    private static final String TITLE = "title";
    private static final String ITEM = "item";
    private static final String OGG = "audio/ogg";
    private static final String MPEG = "audio/mpeg";
    private static final String MP3 = "audio/mp3";
    private static final String OLD_OGG = "application/ogg";
	private static final String TAG = "EpisodeFeedParser";
	
	private String mDescription = "";
	private String mDescription_content = "";
    
    /**
     * Constructor - Takes a feedUrl and passes it to the SuperClass.
     * @param ctx
     * @param feedUrl
     */
	public EpisodeFeedParser(Context ctx, String feedUrl) {
		super(ctx, feedUrl);
	}
	/**
	 * Parses the RSS feed from the InputStream and extracts text elements and 
	 * url attributes from them.
	 * @return a List of Episodes.
	 */
	public List<Episode> parse() {
		final Episode new_episode = new Episode();
		final List<Episode> episodes = new ArrayList<Episode>();
		
		//Set up the required elements.
		RootElement root = new RootElement("rss");
		Element channel = root.requireChild("channel");
		Element item = channel.requireChild(ITEM);
		
		/*Set up the ElementListeners.
		 * The first listens for the end if the item element, which marks the end
		 * of each episodes description in the RSS.  At this point the Episode is added
		 * the list as it will have had all its details recorded by the other 
		 * listeners. 
		 */
		item.setEndElementListener(new EndElementListener(){
            public void end() {
            	//Determine which description to use
            	if (mDescription_content.length() > mDescription.length()){
            		new_episode.setDescription(mDescription_content);
            	} else {
            		new_episode.setDescription(mDescription);
            	}
            	
            	episodes.add(new Episode(new_episode));
            	
            	new_episode.clear();
            }
		});
		
		//This listener catches the title.
		item.requireChild(TITLE).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_episode.setTitle(body);
				
			}
		});
		
		//This listener catches the pubDate.
		item.requireChild(PUB_DATE).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_episode.setDate(body);
			}
		});
		//This Listener catches the length and url of the podcast.
		item.getChild(CONTENT).setStartElementListener(new StartElementListener() {
			
			@Override
			public void start(Attributes attributes) {
				//Check that the content is a media file and not a pdf or something.	
				String mime_type = attributes.getValue("", "type");
				
				if (mime_type.equalsIgnoreCase(OGG) || 
						mime_type.equalsIgnoreCase(MPEG) ||
						mime_type.equalsIgnoreCase(OLD_OGG)||
						mime_type.equalsIgnoreCase(MP3)) {
					String length = attributes.getValue("", "length");
					new_episode.setLength(length);
					String url = attributes.getValue("", "url");
					new_episode.setLink(url);
				}
			}
		});
		//This Listener catches the Description of the podcast.
		//The description may be in an encoded tag or a description tag
		//Collect both and use the one with the longest string
		
		item.getChild(CONTENT_NS, ENCODED).setEndTextElementListener(
				new EndTextElementListener() {

					@Override
					public void end(String body) {
						mDescription_content = body;				
					}
				});
		
		item.getChild(DESCRIPTION).setEndTextElementListener(
				new EndTextElementListener() {
					@Override
					public void end(String body) {
						mDescription = body;				
					}
				});
		
		//Finally, now the listeners are set up we can parse the XML file.
		
		HttpURLConnection conn = getConnection();
		//To debug with test feeds comment out the above line and uncomment the next line.
	    //InputStream istream = mCtx.getResources().openRawResource(R.raw.testfeed);
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
			}
		}catch (AssertionError e){ //xml.parse repacks SocketTimeoutException as Assertion errors.
			Throwable cause = e.getCause();
			if (cause instanceof SocketTimeoutException){
				Log.e(TAG, "SocketTimeoutException caught parsing feed url");
			}
		} catch (SAXException e) { //Thrown if any requiredChild calls are not satisfied
			Log.e(TAG, "RSS feed is malformed, required data is missing!");
			NotifyError(mCtx.getString(R.string.malformed_feed));
		} catch (IOException e) {
			NotifyError("");
		} finally {
			if (conn != null){
				conn.disconnect();
			}
		}
		
		
		//This iterator loop removes all episodes that don't have a URL.  This is for
		//feeds that have "news" items in the same feed with the audio items.  We
		//don't want the news, so we delete those.
				
		final Iterator<Episode> iterator = episodes.iterator();
			while (iterator.hasNext()) {
				final Episode episode = iterator.next();
				if ((episode.getLink() == null)){
					iterator.remove();
				}
			}
		
		//Return the final list.
		return episodes;		
	}
}
