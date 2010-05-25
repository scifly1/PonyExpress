/*
 * Copyright 2010 Paul Elms

    This file is part of PonyExpress.

    PonyExpress is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    PonyExpress is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with PonyExpress.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.sixgun.ponyexpress;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PonyExpressDbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "PonyExpress.db";
    private static final String TABLE_NAME = "Episodes";
    private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                Episode._ID + " INTEGER PRIMARY KEY," +
                Episode.TITLE + " TEXT," +
                Episode.DATE + " INTEGER," +
                Episode.URL + " TEXT," +
                Episode.FILENAME + " TEXT);";

    PonyExpressDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w("PonyExpress", "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
	}

}