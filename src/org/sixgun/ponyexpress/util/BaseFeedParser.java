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
package org.sixgun.ponyexpress.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/*
 * BaseFeedParser is an abstract class that takes a url and its getInputStream() 
 * method returns an InputStream object from that url.
 */

public abstract class BaseFeedParser {
    
    final URL feedUrl;
    /**
     * Constructor - takes the URL of the RSS feed to be parsed.
     * @param feedUrl
     */
    protected BaseFeedParser(String feedUrl){
        try {
            this.feedUrl = new URL(feedUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Opens a connection to feedUrl.
     * 
     * @return an InputStream from the feedUrl
     */
    protected InputStream getInputStream() {
    	try {
    			return feedUrl.openConnection().getInputStream();
    	} catch (IOException e) {
    		return null; 
    	}

    }

}
