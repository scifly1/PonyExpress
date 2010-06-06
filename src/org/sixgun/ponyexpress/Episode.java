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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * POJO representing an Episode of a podcast.  Holds the pubDate,
 * title and url of each episode.
 */

public class Episode implements Comparable<Episode> {

	static SimpleDateFormat FORMATTER = 
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
    private String mTitle;
    private URL mLink;
    private Date mDate;
	private Boolean mDownloaded;
	private Boolean mListened;
	
    /**
     * Constructor.  Creates 'empty' episode.
     */
    public Episode() {
    	//Nothing is assigned here.
	}
    
    /**
     * Copy constructor.  
     * @param episode to copy.
     */
    public Episode(Episode episode){
    	this(episode.mDate,episode.mLink,episode.mTitle,episode.mDownloaded,episode.mListened);
    }
    /**
     * Alternative private constructor used by the copy constructor to create copies.
     * @param _date
     * @param _link
     * @param _title
     * @param _downloaded 
     * @param _listened 
     */
    private Episode(Date _date, URL _link, String _title, Boolean _downloaded, Boolean _listened) {
		this.mDate = _date;
		this.mLink = _link;
		this.mTitle = _title;
		this.mDownloaded = _downloaded;
		this.mListened = _listened;
	}

	

	/*
     * Getters and Setters for each field.
     */
    public void setLink(String link) {
        try {
            this.mLink = new URL(link);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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
            throw new RuntimeException(e);
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
	 * @param downloaded True if downloaded, false if not.
	 */
	public void setDownloaded(Boolean downloaded) {
		this.mDownloaded = downloaded;
	}

	/**
	 * @return downloaded
	 */
	public Boolean beenDownloaded() {
		return mDownloaded;
	}

	/**
	 * @param listened True if listened to, false if not.
	 */
	public void setListened(Boolean listened) {
		this.mListened = listened;
	}

	/**
	 * @return listened
	 */
	public Boolean beenListened() {
		return mListened;
	}

	@Override
	public int compareTo(Episode another) {
		if (this.mDate.equals(another.mDate)) return 0;
		else if (this.mDate.getTime() < another.mDate.getTime()) return -1;
		else return 1;
	}
}
