package org.sixgun.ponyexpress.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
		
		Log.e(TAG, "Starting BackupParser");
		final List<String> urllist = new ArrayList<String>();
		InputStream filename = null;
				
		try {
			filename = new FileInputStream(Environment.getExternalStorageDirectory()+"/all-subscriptions.opml");
		} catch (FileNotFoundException e){
			Log.e(TAG, "No file found");
			//TODO
		}
		
		RootElement opml = new RootElement(OPML);
		Element body = opml.requireChild(BODY);		
		
		//This Listener catches the length and url of the podcast.
		body.getChild(OUTLINE).setStartElementListener(new StartElementListener() {
			@Override
			public void start(Attributes attributes) {
				String url = attributes.getValue(XMLURL);
				Log.d(TAG, "Adding " + url);
				urllist.add(url);
			}
		});
		
		if (filename != null){
			try {
				Xml.parse(filename, Xml.Encoding.UTF_8, 
						opml.getContentHandler());
			} catch (SAXException e) { //Thrown if any requiredChild calls are not satisfied
				Log.e(TAG, "RSS feed is malformed, required data is missing!");
        	} catch (IOException e) {
        		//TODO
        	}
		}
		Log.d(TAG, "List returned");
		return urllist;

	}

}
