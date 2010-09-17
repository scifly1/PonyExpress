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

/**
 * Represents a podcast.  Holds the name,
 * url and album art url of each episode.
 */
public class Podcast {

	private String mName;
    private URL mFeed_Url;
    private URL mArt_Url;
    private String mIdenticaTag;
    
    /**
     * Constructor.  Creates 'empty' podcast.
     */
    public Podcast() {
    	mIdenticaTag = "";  //If this is not set later it will remain empty and can be checked for.
	}
    
    /**
     * Copy constructor.  
     * @param episode to copy.
     */
    public Podcast(Podcast newPodcast) {
    	this(newPodcast.mName,newPodcast.mFeed_Url,newPodcast.mArt_Url,newPodcast.mIdenticaTag);
	}
    
    /**
     * Alternative private constructor used by the copy constructor to create copies.
     * @param _date
     * @param _link
     * @param _title
     */
    private Podcast(String _name, URL _feed, URL _art, String _tag){
    	this.mName = _name;
    	this.mFeed_Url = _feed;
    	this.mArt_Url = _art;
    	this.mIdenticaTag = _tag;
    }
    
	/*
     * Getters and Setters for each field.
     */
    /**
	 * @param mName the mName to set
	 */
	public void setName(String mName) {
		this.mName = mName;
	}
	/**
	 * @return the mName
	 */
	public String getName() {
		return mName;
	}
	/**
	 * @param mFeed_Url the mFeed_Url to set
	 */
	public void setFeed_Url(String mFeed_Url) {
		try {
            this.mFeed_Url = new URL(mFeed_Url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
            //TODO Handle incorrect urls better as they will be entered by hand
        }
	}
	/**
	 * @return the mFeed_Url
	 */
	public URL getFeed_Url() {
		return mFeed_Url;
	}
	/**
	 * @param mArt_Url the mArt_Url to set
	 */
	public void setArt_Url(String mArt_Url) {
		try {
            this.mArt_Url = new URL(mArt_Url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
	}
	/**
	 * @return the mArt_Url
	 */
	public URL getArt_Url() {
		return mArt_Url;
	}

	/**
	 * @param mIdenticaTag the mIdenticaTag to set
	 */
	public void setIdenticaTag(String mIdenticaTag) {
		this.mIdenticaTag = mIdenticaTag;
	}

	/**
	 * @return the mIdenticaTag
	 */
	public String getIdenticaTag() {
		return mIdenticaTag;
	}
}
