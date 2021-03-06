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

import org.sixgun.ponyexpress.Podcast;
import org.xml.sax.Attributes;

import android.content.Context;
import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

/**
 * Parses a Podcast RSS feed and extracts general information about the podcast
 * such as title and almum art URI.
 *
 */
public class PodcastFeedParser extends BaseFeedParser {

	private static final String MEDIA_NS = "http://www.itunes.com/dtds/podcast-1.0.dtd";
	// names of the XML tags
    private static final String NAME = "title";
    private static final String ALBUM_ART_URL = "image";
	private static final String TAG = "Pony/PodcastFeedParser";
	private static final String URL = "url";


	public PodcastFeedParser(Context ctx, String feedUrl) {
		super(ctx, feedUrl);
		
	}
	
	/**
	 * Parses the RSS feed from the InputStream and extracts text elements and 
	 * url attributes from them.
	 * @return a List of Episodes.
	 */
	public Podcast parse() {
		final Podcast new_podcast = new Podcast();
		if (mFeedUrl == null){
			return null;
		}
				
		//Set up the required elements.
		RootElement root = new RootElement("rss");
		Element channel = root.requireChild("channel");
		Element image = channel.getChild("image");
		
		/*Set up the ElementListeners.
		 * 
		 */
		
		//Store the Feed URL
		new_podcast.setFeedUrl(mFeedUrl);
		
		//This listener catches the name.
		channel.requireChild(NAME).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				PonyLogger.d(TAG,"Podcast is: " + body);
				new_podcast.setName(body);
			}
		});

		//This Listener catches the album art url from itunes namespace.
		channel.getChild(MEDIA_NS, ALBUM_ART_URL).setStartElementListener(new StartElementListener() {

			@Override
			public void start(Attributes attributes) {
				String url = attributes.getValue("", "href");
				PonyLogger.d(TAG, "Podcast art from: " + url);
				new_podcast.setArt_Url(url);				
			}
		});
		
		//This Listener catches the album art url from xml if itunes is not set.
		image.getChild(URL).setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				if (new_podcast.getArt_Url() == null){
					PonyLogger.d(TAG, "Podcast art from: " + body);
					new_podcast.setArt_Url(body);
				}
			}
		});

		//Finally, now the listeners are set up we can parse the XML file.
		
		HttpURLConnection conn = getConnection();
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
			} else {
				NotifyError("");
				return null;		
			}
		}catch (AssertionError e){ //xml.parse repacks SocketTimeoutException as Assertion errors.
			Throwable cause = e.getCause();
			if (cause instanceof SocketTimeoutException){
				PonyLogger.e(TAG, "SocketTimeoutException caught parsing podcast feed");
				NotifyError("");
				return null;
			}
		} catch (Exception e) {
			NotifyError("");
			return null;
		} finally {
			if (conn != null){
				conn.disconnect();
			}
		}
		return new_podcast;

		
		
	}
	
	public String parseAlbumArtURL() {
		if (mFeedUrl == null){
			return null;
		}
				
		//Set up the required elements.
		RootElement root = new RootElement("rss");
		Element channel = root.requireChild("channel");
		Element image = channel.getChild("image");
		
		//Use a podcast instance as it can hold the url and we need to use a final
		//object here.
		final Podcast new_podcast = new Podcast();
		
		//Set up the ElementListener.
		//This Listener catches the album art url from itunes namespace.
		channel.getChild(MEDIA_NS, ALBUM_ART_URL).setStartElementListener(new StartElementListener() {
			
			@Override
			public void start(Attributes attributes) {
				String url = attributes.getValue("", "href");
				PonyLogger.d(TAG, "Podcast art from: " + url);
				new_podcast.setArt_Url(url);				
			}
		});

		//This Listener catches the album art url from xml if itunes is not set.
		image.getChild(URL).setEndTextElementListener(new EndTextElementListener() {

			@Override
			public void end(String body) {
				if (new_podcast.getArt_Url() == null){
					PonyLogger.d(TAG, "Podcast art from: " + body);
					new_podcast.setArt_Url(body);
				}
			}
		});

		//Finally, now the listeners are set up we can parse the XML file.
		
		HttpURLConnection conn = getConnection();	
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
			} else {
				NotifyError("");
				return null;
			}
		}catch (AssertionError e){ //xml.parse repacks SocketTimeoutException as Assertion errors.
			Throwable cause = e.getCause();
			if (cause instanceof SocketTimeoutException){
				PonyLogger.e(TAG, "SocketTimeoutException caught parsing album art url");
				return null;
			}
		} catch (Exception e) {
			NotifyError("");
			return null;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return new_podcast.getArt_Url().toString();
	}


}
