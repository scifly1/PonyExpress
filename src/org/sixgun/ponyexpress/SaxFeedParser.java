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

import org.xml.sax.Attributes;

import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;

public class SaxFeedParser extends BaseFeedParser{

	public SaxFeedParser(String feedUrl) {
		super(feedUrl);
		
	}
	
	public void parse() {
		final Episode new_episode = new Episode();
		RootElement root = new RootElement("rss");
		Element channel = root.requireChild("channel");
		Element item = channel.requireChild(ITEM);
		item.getChild(TITLE).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_episode.setTitle(body);
			}
		});
		item.getChild(PUB_DATE).setEndTextElementListener(new EndTextElementListener() {
			
			@Override
			public void end(String body) {
				new_episode.setDate(body);
			}
		});
		item.getChild(CONTENT).setStartElementListener(new StartElementListener() {
			
			@Override
			public void start(Attributes attributes) {
				String url = attributes.getValue("", "media:content");
				new_episode.setLink(url);
			}
		});
		
	}

}
