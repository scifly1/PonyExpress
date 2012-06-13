/*
 * Copyright 2012 James Daws
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.sixgun.ponyexpress.PonyExpressApp;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.os.Environment;
import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;


public class BackupParser {
	
	private static final String OPML = "opml";
	private static final String XMLURL = "xmlUrl";
	private static final String BODY = "body";
	private static final String OUTLINE = "outline";

	private static final String TAG = "BackupParser";

	public List<String> parse() {

		Log.d(TAG, "Starting BackupParser");
		final List<String> urllist = new ArrayList<String>();
		InputStream filename = null;

		try {
			filename = new FileInputStream(Environment.getExternalStorageDirectory() + PonyExpressApp.PODCAST_PATH + "all-subscriptions.opml");
		} catch (FileNotFoundException e){
			Log.e(TAG, "No file found");
			//TODO
		}

		RootElement opml = new RootElement(OPML);
		Element body = opml.requireChild(BODY);		

		//This Listener catches and adds the xmlUrl attribute to the url list.
		body.getChild(OUTLINE).setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				String url = attributes.getValue(XMLURL);
				urllist.add(url);
			}
		});

		if (filename != null){
			try {
				Xml.parse(filename, Xml.Encoding.UTF_8, 
						opml.getContentHandler());
			} catch (SAXException e) { //Thrown if any requiredChild calls are not satisfied
				//TODO Proper error handling
				Log.e(TAG, "RSS feed is malformed, required data is missing!");
			} catch (IOException e) {
				//TODO
			}
		}
		Log.d(TAG, "List returned");
		return urllist;
	}
}
