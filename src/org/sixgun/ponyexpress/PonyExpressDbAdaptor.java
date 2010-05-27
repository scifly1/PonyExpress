package org.sixgun.ponyexpress;


import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * Helper class that handles all database interactions for the app.
 */
public class PonyExpressDbAdaptor {
	private static final int DATABASE_VERSION = 4;
	private static final String DATABASE_NAME = "PonyExpress.db";
    private static final String TABLE_NAME = "Episodes";
    private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                EpisodeKeys._ID + " INTEGER PRIMARY KEY," +
                EpisodeKeys.TITLE + " TEXT," +
                EpisodeKeys.DATE + " TEXT," +
                EpisodeKeys.URL + " TEXT," +
                EpisodeKeys.FILENAME + " TEXT," +
                EpisodeKeys.DOWNLOADED + " INTEGER," +
                EpisodeKeys.LISTENED + " INTEGER);";
    
    private PonyExpressDbHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private final Context mCtx;
    /*
     * Inner helper class responsible for creating/opening, upgrading and closing 
     * the database. 
     */
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
    
    /**
     * Adds each episode to the database.
     * @param episode
     * @return The row ID of the inserted row or -1 if an error occurred.
     */
    public long addEpisode(Episode episode) {
    	//TODO Check if episode is already in the database first.
        ContentValues episodeValues = new ContentValues();
        episodeValues.put(EpisodeKeys.TITLE, episode.getTitle());
        episodeValues.put(EpisodeKeys.DATE, episode.getDateString());
        episodeValues.put(EpisodeKeys.URL, episode.getLink().toString());
        String filename = episode.getLink().getFile(); 
        episodeValues.put(EpisodeKeys.FILENAME, filename);
        episodeValues.put(EpisodeKeys.DOWNLOADED, episode.beenDownloaded());
        episodeValues.put(EpisodeKeys.LISTENED, episode.beenListened());

        return mDb.insert(TABLE_NAME, null, episodeValues);
    }
    //TODO These are basic method stubs to remind me to write them, the signatures
    // are not necessarily correct.
//	public int delete(Uri uri, String selection, String[] selectionArgs) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//
//	public Uri insert(Uri uri, ContentValues values) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	
//	public Cursor query(Uri uri, String[] projection, String selection,
//			String[] selectionArgs, String sortOrder) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	public int update(Uri uri, ContentValues values, String selection,
//			String[] selectionArgs) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
    
    
}
