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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;


public class EpisodeCursorAdapter extends CursorAdapter {

	
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

	}

	/* (non-Javadoc)
	 * @see android.widget.CursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
	 */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = new View(context);
		v = vi.inflate(R.layout.episode_row, null);
		return v;
	}

}
