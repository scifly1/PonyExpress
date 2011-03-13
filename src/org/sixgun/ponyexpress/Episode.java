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

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.sixgun.ponyexpress.util.Utils;

import android.util.Log;

/*
 * POJO representing an Episode of a podcast.  Holds the pubDate,
 * title and url of each episode.
 */

public class Episode implements Comparable<Episode> {

	private static final String TAG = "PonyExpress Episode";
	static SimpleDateFormat FORMATTER = 
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    private String mTitle;
    private URL mLink;
    private Date mDate;
    private String mDescription;
	private Boolean mDownloaded;
	private int mListened;  // This is the number of msec that have been listened to, or -1 if not listened.
	private String mLength; //This is the file size in bytes
	
    /**
     * Constructor.  Creates 'empty' episode.
     */
    public Episode() {
    	mDownloaded = false;
    	mListened = -1;
	}
    
    /**
     * Copy constructor.  
     * @param episode to copy.
     */
    public Episode(Episode episode){
    	this(episode.mDate,episode.mLink,episode.mTitle,episode.mDescription,episode.mDownloaded,episode.mListened,episode.mLength);
    }
    /**
     * Alternative private constructor used by the copy constructor to create copies.
     * @param _date
     * @param _link
     * @param _title
     * @param _downloaded 
     * @param _listened 
     * @param _description 
     * @param _length
     */
    private Episode(Date _date, URL _link, String _title, String _description, Boolean _downloaded, int _listened, String _length) {
		this.mDate = _date;
		this.mLink = _link;
		this.mTitle = _title;
		this.mDescription = _description;
		this.mDownloaded = _downloaded;
		this.mListened = _listened;
		this.mLength = _length;
	}

	

	/*
     * Getters and Setters for each field.
     */
    public void setLink(String link) {
        mLink = Utils.getURL(link);
    }
    
    public URL getLink() {
    	return mLink;
    }
    
    public void setDate(String date) {
        // pad the date if necessary
        while (!date.endsWith("00")){
            date += "0";
        }
        try {
            this.mDate = FORMATTER.parse(date.trim());
        } catch (ParseException e) {
            Log.e(TAG,"Error parsing the date from the feed!",e);
        }
    }
    
    public Date getDate() {
    	return mDate;
    }

	public void setTitle(String title) {
		this.mTitle = title;
	}

	public String getTitle() {
		return mTitle;
	}
	

	/**
	 * @return downloaded
	 */
	public Boolean beenDownloaded() {
		return mDownloaded;
	}

	/**
	 * @return listened
	 */
	public int beenListened() {
		return mListened;
	}

	@Override
	public int compareTo(Episode another) {
		if (this.mDate.equals(another.mDate)) return 0;
		else if (this.mDate.getTime() < another.mDate.getTime()) return -1;
		else return 1;
	}

	public void setDescription(String description) {
		this.mDescription = description;
	}
	
	public String getDescription(){
		return mDescription;
	}

	public void setLength(String length) {
		this.mLength = length;
		
	}
	public String getLength() {
		return mLength;
		
	}
	
}
