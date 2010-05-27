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
package org.sixgun.ponyexpress;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

/*
 * SaxFeedPArser implements a basic Android SAX parser.  It finds the XML tags in the RSS
 * feed that is returned by this.getInputStream() and extracts the text elements
 * or attributes from them using ElementListeners.
 */
public class SaxFeedParser extends BaseFeedParser{
	
	// names of the XML tags
    static final String PUB_DATE = "pubDate";
    static final String CONTENT = "enclosure";
    static final String TITLE = "title";
    static final String ITEM = "item";
    
    /**
     * Constructor - Takes a feedUrl and passes it to the SuperClass.
     * @param feedUrl
     */
	public SaxFeedParser(String feedUrl) {
		super(feedUrl);
		
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
		 * the list as it should have had all its details recorded by the other 
		 * listeners.
		 */
		item.setEndElementListener(new EndElementListener(){
            public void end() {
                new_episode.setDownloaded(false);
                new_episode.setListened(false);
            	episodes.add(new Episode(new_episode));
            }
		});
		//This listener catches the title.
		item.getChild(TITLE).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_episode.setTitle(body);
			}
		});
		//This listener catches the pubDate.
		item.getChild(PUB_DATE).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_episode.setDate(body);
			}
		});
		//This Listener catches the url of the podcast.
		item.getChild(CONTENT).setStartElementListener(new StartElementListener() {
			
			@Override
			public void start(Attributes attributes) {
				String url = attributes.getValue("", "url");
				new_episode.setLink(url);
			}
		});
		//Finally, now the listeners are set up we can parse the XML file.
		try {
            Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, 
root.getContentHandler());
        } catch (Exception e) {
        	//TODO Change this to cause a popup informing user that cannot connect.
            throw new RuntimeException(e);
        }
		
		return episodes;
		
	}

}
