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

import java.io.InputStream;
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
    static final String IDENTICA_TAG = "identica_tag";
    static final String IDENTICA_GROUP = "identica_group";
    
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
		
		//This listener catches the identi.ca tag
		podcast.getChild(IDENTICA_TAG).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_podcast.setIdenticaTag(body);
			}
		});
		
		//This listener catches the identi.ca group
		podcast.getChild(IDENTICA_GROUP).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_podcast.setIdenticaGroup(body);
			}
		});
		
		//Finally, now the listeners are set up we can parse the XML file.
				
		InputStream istream = getInputStream();
		//To debug with test.xml comment out the above line and uncomment the next line.
	    //InputStream istream = mCtx.getResources().openRawResource(R.raw.test);
		
		if (istream != null){
			try {
				Xml.parse(istream, Xml.Encoding.UTF_8, 
						root.getContentHandler());
			} catch (Exception e) {
				NotifyError("");
			}
		}
		
		return podcasts;
	}
	
	

}
