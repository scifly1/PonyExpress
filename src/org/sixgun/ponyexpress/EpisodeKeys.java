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

import android.provider.BaseColumns;

public final class EpisodeKeys implements BaseColumns {
	// This class cannot be instantiated
	//These are all keys in each Episodes table in the database
	public static final String TITLE = "title";
	public static final String DATE = "date";
	public static final String URL = "url";
	public static final String FILENAME = "filename";
	public static final String DESCRIPTION = "description";
	public static final String DOWNLOADED = "downloaded";
	public static final String LISTENED = "listened";
	public static final String SIZE = "length";
	//This is the row_id of the episode in the episodes table as used in the 
	//playlist table (a different string is required, else it clashes with
	// the _ID key of the playlist table.
	public static final String ROW_ID = "row_id";
	
	//This is not a key in the Db, only a useful constant.
	public static final String EP_NUMBER = "episode_number";
	
	
	

}
