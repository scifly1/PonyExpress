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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

/**
 * DentParser implements a basic Android SAX parser.  It finds the XML tags in the RSS
 * feed that is returned by this.getInputStream() and extracts the text elements
 * or attributes from them using ElementListeners.
 */
public class DentParser extends BaseFeedParser {

	// names of the XML tags
	static final String ENTRY = "entry";
	static final String TITLE = "title";
	static final String AUTHOR = "author";
	static final String NAME = "name";
	
	
	protected DentParser(String feedUrl) {
		super(feedUrl);
	}
	
	/**
	 * Parses the RSS feed from the InputStream and extracts text elements and 
	 * url attributes from them.
	 * @return a List of Episodes.
	 */
	public List<Dent> parse() {
		final Dent new_dent = new Dent();
		final List<Dent> dents = new ArrayList<Dent>();
		
		//Set up the required elements.
		RootElement root = new RootElement("http://www.w3.org/2005/Atom","feed");
		Element entry;
		entry = root.getChild(ENTRY);  
		if (entry == null) return dents;  //return empty if no dents.
		
		/*Set up the ElementListeners.
		 * The first listens for the end of the entry element, which marks the end
		 * of each dent in the feed.  At this point the Dent is added
		 * the list as it should have had all its details recorded by the other 
		 * listeners.
		 */
		entry.setEndElementListener(new EndElementListener(){
            public void end() {
                dents.add(new Dent(new_dent));
            }
		});
		//This listener catches the title.
		entry.getChild(TITLE).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_dent.setTitle(body);
			}
		});
		//This listener catches the Author.
		entry.getChild(AUTHOR).getChild(NAME).setEndTextElementListener(
				new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_dent.setAuthor(body);
			}
		});
		
		//Finally, now the listeners are set up we can parse the XML file.
		
		InputStream istream = this.getInputStream();
		//To debug with test feeds comment out the above line and uncomment the next line.
	    //InputStream istream = mCtx.getResources().openRawResource(R.raw.testfeed);
		if (istream != null){
			try {
				Xml.parse(istream, Xml.Encoding.UTF_8, 
						root.getContentHandler());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return dents;
		
	}

}
