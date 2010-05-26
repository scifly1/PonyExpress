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

import java.util.List;

import android.app.Activity;
import android.os.Bundle;

/*
 * Launch Activity for PonyExpress.  In a VERY DEBUG state at present.
 */
public class PonyExpress extends Activity {



	private PonyExpressDbAdaptor mDbHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//no views created yet.
		mDbHelper = new PonyExpressDbAdaptor(this);
		mDbHelper.open();
		
		//TODO Get a service to do this, so it doesn't hold us up to much.
		String feed = "http://feeds.feedburner.com/linuxoutlaws-ogg";
		
		SaxFeedParser parser = new SaxFeedParser(feed);
		List<Episode> Episodes = parser.parse();
		
		for (Episode episode: Episodes){
			mDbHelper.addEpisode(episode);
		}
		mDbHelper.close();	
	}
	
}
