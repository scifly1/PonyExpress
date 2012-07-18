/*
 * Copyright 2012 Paul Elms
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

import org.sixgun.ponyexpress.util.Utils;
import org.sixgun.ponyexpress.view.RemoteImageView;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class PodcastCursorAdapter extends CursorAdapter {

	private PonyExpressApp mPonyExpressApp;

	/**
	 * We subclass CursorAdapter to display the results from our Podcast cursor. 
	 * Overide newView to create/inflate a view to bind the data to.
	 * Overide bindView to determine how the data is bound to the view.
	 * 
	 * This is subclassed in PonyExpressActivity and PlaylistActivity to handle 
	 * their podcast lists.
	 */
	
	
	public PodcastCursorAdapter(Context context, Cursor c) {
		super(context, c);
		mPonyExpressApp = (PonyExpressApp)context.getApplicationContext();
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final int nameIndex = cursor.getColumnIndex(PodcastKeys.NAME);
		final int artUrlIndex = cursor.getColumnIndex(PodcastKeys.ALBUM_ART_URL);
		//get the number of unlistened episodes
		String name = cursor.getString(nameIndex);
		final int unlistened = mPonyExpressApp.getDbHelper().countUnlistened(name);

		//Remove the words "ogg feed" if present at the end.
		name = Utils.stripper(name, "Ogg Feed");

		TextView podcastName = (TextView) view.findViewById(R.id.podcast_text);
		RemoteImageView albumArt = (RemoteImageView)view.findViewById(R.id.album_art);
		TextView unlistenedText = (TextView) view.findViewById(R.id.unlistened_eps);

		podcastName.setText(name);
		String albumArtUrl = cursor.getString(artUrlIndex);
		if (albumArtUrl!= null && !"".equals(albumArtUrl) && !"null".equalsIgnoreCase(albumArtUrl)){
			albumArt.setRemoteURI(albumArtUrl);
			albumArt.loadImage();
		} else {
			albumArt.loadDefault();
		}
		final String unlistenedString = Utils.formUnlistenedString(context, unlistened);
		unlistenedText.setText(unlistenedString);

		
		

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = new View(context);
		v = vi.inflate(R.layout.podcast_row, parent, false);
		return v;
	}
}
