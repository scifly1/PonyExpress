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

import org.sixgun.ponyexpress.view.RemoteImageView;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;


public class EpisodeCursorAdapter extends CursorAdapter {

	
	private static final String YOUTUBE_THUMBNAIL_LINK_END = "jpg";
	private static final String YOUTUBE_THUMBNAIL_LINK_START = "src=\"http";

	public EpisodeCursorAdapter(Context context, Cursor c) {
		super(context, c);
	}


	/* (non-Javadoc)
	 * @see android.widget.CursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final int titleIndex = cursor.getColumnIndex(EpisodeKeys.TITLE);
		final int listenedIndex = cursor.getColumnIndex(EpisodeKeys.LISTENED);
		TextView episodeText = (TextView) view.findViewById(R.id.episode_text);
		String title = cursor.getString(titleIndex);
		int listened = cursor.getInt(listenedIndex);
		episodeText.setText(title);
		if (listened == -1){ //not listened == -1
			episodeText.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
		} else episodeText.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);

		if (getItemViewType(cursor) == 1){
			//Youtube episode
			RemoteImageView thumbnail = (RemoteImageView) view.findViewById(R.id.thumbnail);
			if (thumbnail != null){
				//Get thumbnail url from description text
				String description = cursor.getString(
						cursor.getColumnIndexOrThrow(EpisodeKeys.DESCRIPTION));
				final int end = description.indexOf(YOUTUBE_THUMBNAIL_LINK_END) + YOUTUBE_THUMBNAIL_LINK_END.length();
				final int start = description.indexOf(YOUTUBE_THUMBNAIL_LINK_START)+5;
				String url = description.substring(start, end);
            	if (url!= null && !"".equals(url) && !"null".equalsIgnoreCase(url)){
            		thumbnail.setRemoteURI(url);
            		thumbnail.loadImage();
            	}
			}
		}
	}

	/* (non-Javadoc)
	 * @see android.widget.CursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
	 */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = new View(context);
		if (getItemViewType(cursor)== 0){
			v = vi.inflate(R.layout.episode_row, null);
		} else {
			v = vi.inflate(R.layout.youtube_episode_row, null);
		}
		return v;
	}
	
	/**
	 * Determines the type of item to view, either a regular episode
	 * or a youtube episode.
	 * @param cursor
	 * @return 1 for youtube, 0 for regular
	 */
	private int getItemViewType(Cursor cursor) {
		if (cursor.getString(cursor.getColumnIndexOrThrow(EpisodeKeys.URL)).
				contains("www.youtube.com")){
			return 1;
		} else {
	        return 0;
	    }
	}

	@Override
	public int getItemViewType(int position) {
	    Cursor cursor = (Cursor) getItem(position);
	    return getItemViewType(cursor);
	}

	@Override
	public int getViewTypeCount() {
	    return 2;
	}

}
