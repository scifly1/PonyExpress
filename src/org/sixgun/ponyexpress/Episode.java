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
    private String title;
    private URL link;
    private Date date;
	
    /*
     * Getters and Setters for each field.
     */
    public void setLink(String link) {
        try {
            this.link = new URL(link);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    public URL getLink() {
    	return link;
    }
    
    public void setDate(String date) {
        // pad the date if necessary
        while (!date.endsWith("00")){
            date += "0";
        }
        try {
            this.date = FORMATTER.parse(date.trim());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    
    public Date getDate() {
    	return date;
    }
    
    public String getDateString() {
    	return FORMATTER.format(date);
    }
    

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}
	
	@Override
	public int compareTo(Episode another) {
		if (another == null) return 1;
        // sort descending, most recent first
        return another.date.compareTo(date);
	}

}
