package org.sixgun.ponyexpress;


import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class PonyExpressDbAdaptor {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "PonyExpress.db";
    private static final String TABLE_NAME = "Episodes";
    private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                EpisodeKeys._ID + " INTEGER PRIMARY KEY," +
                EpisodeKeys.TITLE + " TEXT," +
                EpisodeKeys.DATE + " INTEGER," +
                EpisodeKeys.URL + " TEXT," +
                EpisodeKeys.FILENAME + " TEXT);";
    
    private PonyExpressDbHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private final Context mCtx;
    
    public class PonyExpressDbHelper extends SQLiteOpenHelper {
        

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
    
    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public PonyExpressDbAdaptor(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialisation call)
     * @throws SQLException if the database could be neither opened or created
     */
    public PonyExpressDbAdaptor open() throws SQLException {
        mDbHelper = new PonyExpressDbHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }

}
