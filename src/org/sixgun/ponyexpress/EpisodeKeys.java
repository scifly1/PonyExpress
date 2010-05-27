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
	public static final String TITLE = "title";
	public static final String DATE = "date";
	public static final String URL = "url";
	public static final String FILENAME = "filename";
	public static final String DOWNLOADED = "downloaded";
	public static final String LISTENED = "listened";
	

}
