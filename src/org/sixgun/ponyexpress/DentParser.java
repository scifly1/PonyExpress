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

	//The context is only needed for using the debug testfeeds.
	@SuppressWarnings("unused")
	private Context mCtx;
	// names of the XML tags
	static final String ENTRY = "entry";
	static final String TITLE = "title";
	static final String AUTHOR = "author";
	static final String NAME = "name";
	protected static final String TAG = "DentParser";
	private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
	
	
	protected DentParser(Context ctx, String feedUrl) {
		super(feedUrl);
		mCtx = ctx;
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
		RootElement root = new RootElement(ATOM_NS,"feed");
		Element entry = root.getChild(ATOM_NS,ENTRY);  
				
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
		entry.getChild(ATOM_NS,TITLE).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				Log.d(TAG,"Found title: " + body);
				new_dent.setTitle(body);
			}
		});
		//This listener catches the Author.
		entry.getChild(ATOM_NS,AUTHOR).getChild(ATOM_NS,NAME).setEndTextElementListener(
				new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				Log.d(TAG,"Found author: " + body);
				new_dent.setAuthor(body);
			}
		});
		
		//Finally, now the listeners are set up we can parse the XML file.
		
		InputStream istream = this.getInputStream();
		//To debug with test feeds comment out the above line and uncomment the next line.
	    //InputStream istream = mCtx.getResources().openRawResource(R.raw.dentfeed);
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
