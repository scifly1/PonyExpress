package org.sixgun.ponyexpress.util;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeKeys;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * Helper class that handles all database interactions for the app.
 */
public class PonyExpressDbAdaptor {
	private static final int DATABASE_VERSION = 12;
	private static final String DATABASE_NAME = "PonyExpress.db";
    private static final String TABLE_NAME = "Episodes";
    private static final String TEMP_TABLE_NAME = "Temp_Episodes";
    private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                EpisodeKeys._ID + " INTEGER PRIMARY KEY," +
                EpisodeKeys.TITLE + " TEXT," +
                EpisodeKeys.DATE + " INTEGER," +
                EpisodeKeys.URL + " TEXT," +
                EpisodeKeys.FILENAME + " TEXT," +
                EpisodeKeys.DESCRIPTION + " TEXT," +
                EpisodeKeys.DOWNLOADED + " INTEGER," +
                EpisodeKeys.LISTENED + " INTEGER" +
                EpisodeKeys.LENGTH + " INTEGER);";
    private static final String TEMP_TABLE_CREATE = 
    	"CREATE TEMP TABLE " + TEMP_TABLE_NAME + " (" +
    	EpisodeKeys._ID + " INTEGER PRIMARY KEY," +
        EpisodeKeys.TITLE + " TEXT," +
        EpisodeKeys.DATE + " INTEGER," +
        EpisodeKeys.URL + " TEXT," +
        EpisodeKeys.FILENAME + " TEXT," +
        EpisodeKeys.DESCRIPTION + " TEXT," +
        EpisodeKeys.DOWNLOADED + " INTEGER," +
        EpisodeKeys.LISTENED + " INTEGER);";
    	
    private static final String TAG = "PonyExpressDbAdaptor";
    
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
                    + newVersion);
    		// Copy old data across to new table
    		switch (newVersion) {
			case 12:
				//Begin transaction
				db.beginTransaction();
				try {
					//Create temp table
					db.execSQL(TEMP_TABLE_CREATE);
					//copy all needed columns accross (this copies all, adjust depeding on what the upgrade is)
					db.execSQL("INSERT INTO " + TEMP_TABLE_NAME + " SELECT * FROM " +
							TABLE_NAME + ";");
					//Add/remove/change any new columns to get to new version
					//Drop old table
					db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
					//Create new table
					db.execSQL(TABLE_CREATE);
					//INSERT into new table.
					db.execSQL("INSERT INTO " + TABLE_NAME + " SELECT * FROM " +
							TEMP_TABLE_NAME + ";");
					db.execSQL("DROP TABLE " + TEMP_TABLE_NAME);				
					db.setTransactionSuccessful();
				} finally {
				db.endTransaction();
				}
				break;

			default:
				Log.e(TAG, "Unknow version:" + newVersion + " to upgrade database to.");
				break;
			}
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
     * Open the PonyExpress database. If it cannot be opened, try to create a new
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
    public long insertEpisode(Episode episode) {
    	//FIXME Check if episode is already in the database first?.
        ContentValues episodeValues = new ContentValues();
        episodeValues.put(EpisodeKeys.TITLE, episode.getTitle());
        episodeValues.put(EpisodeKeys.DATE, episode.getDate().getTime());
        episodeValues.put(EpisodeKeys.URL, episode.getLink().toString());
        final String filename = episode.getLink().getFile(); 
        episodeValues.put(EpisodeKeys.FILENAME, filename);
        episodeValues.put(EpisodeKeys.DESCRIPTION, episode.getDescription());
        episodeValues.put(EpisodeKeys.DOWNLOADED, episode.beenDownloaded());
        episodeValues.put(EpisodeKeys.LISTENED, episode.beenListened());
        episodeValues.put(EpisodeKeys.LENGTH, episode.getLength());

        return mDb.insert(TABLE_NAME, null, episodeValues);
    }
    /**
     * Deletes the episode with rowID index.
     * @param rowID
     * @return True if successful.
     */
	public boolean deleteEpisode(Long rowID) {
		
		return mDb.delete(TABLE_NAME, EpisodeKeys._ID + "=" + rowID, null) > 0;
	}
	/**
	 * Gets all unique Episode names from the database.
	 * @return A Cursor object, which is positioned before the first entry
	 */
	public Cursor getAllEpisodeNames(){
		final String[] columns = {EpisodeKeys._ID, EpisodeKeys.TITLE, EpisodeKeys.DATE};
		return mDb.query(
				true,TABLE_NAME,columns,null,null,null,null,EpisodeKeys.DATE +" DESC" ,null);
	}
	/**
	 * Gets all filenames of all the files that have been downloaded and should be 
	 * on the SD Card.
	 */
	public Map<Long, String> getFilenamesOnDisk(){
		final String[] columns = {EpisodeKeys._ID, EpisodeKeys.DOWNLOADED, 
				EpisodeKeys.FILENAME};
		final Cursor cursor = mDb.query(true, TABLE_NAME, columns, 
				EpisodeKeys.DOWNLOADED + "!= 0", null, null, null, null, null);
		Map<Long, String> files = new HashMap<Long, String>();
		String short_filename = "";
		if (cursor != null){
			cursor.moveToFirst();
			for (int i = 0; i < cursor.getCount(); i++){
				final String filename = cursor.getString(2);
				//get everything after last '/' (separator) and remove the '/'
				short_filename = filename.substring(filename.lastIndexOf('/')).substring(1);
				files.put(cursor.getLong(0),short_filename);
				cursor.moveToNext();
			}
		}
		cursor.close();		
		return files;
	}
	/**
	 * Method to determine the date of the latest episode in the database.
	 * @return The Latest episodes date as a string.
	 */
	public Date getLatestEpisodeDate(){
		final String[] columns = {EpisodeKeys.DATE};
		final Cursor cursor = mDb.query(TABLE_NAME, 
				columns, null, null, null, null, EpisodeKeys.DATE +" DESC", "1");
		final boolean result = cursor.moveToFirst();
		//Check the cursor is at a row.
		if (result == true){
			final Long latest_date = cursor.getLong(0);
			cursor.close();
			Log.d("PonyExpressDbAdaptor","Latest date is:" + latest_date.toString());
			return new Date(latest_date);
		}else{
			cursor.close();
			//return date 0 milliseconds
			return new Date(0);
		}
	}
	
	public String getEpisodeUrl(long row_ID){
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.URL};
		final Cursor cursor = mDb.query(true, TABLE_NAME,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String url = "";
		if (cursor != null){
			cursor.moveToFirst();
			url = cursor.getString(1);
			Log.d(TAG, "Url of Episode is: "+ url);
		}
		cursor.close();
		return url;	
	}
	
	/**
	 * Updates the database when a podcast has been downloaded or listened to.
	 * @param rowID 
	 * @param key
	 * @param newRecord
	 * @return true if update successful.
	 */
	public boolean update(long rowID, String key, String newRecord) {
		ContentValues values = new ContentValues();
		//Change ContentValues as required
		if (key == EpisodeKeys.DOWNLOADED){
			if (newRecord == "true"){
				values.put(EpisodeKeys.DOWNLOADED, true);
			}else {
				values.put(EpisodeKeys.DOWNLOADED,false);
			}
		}
		return mDb.update(TABLE_NAME, values, EpisodeKeys._ID + "=" + rowID, null) > 0;
	}
	public boolean update(long rowID, String key, int newRecord){
		ContentValues values = new ContentValues();
		if (key == EpisodeKeys.LISTENED){
			values.put(EpisodeKeys.LISTENED, newRecord);
		}
		return mDb.update(TABLE_NAME, values, EpisodeKeys._ID + "=" + rowID, null) > 0;
	}

	/**
	 * Gets the episodes filename
	 * @param row_ID
	 * @return filename in the form "/file.ext"
	 */
	public String getEpisodeFilename(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.FILENAME};
		final Cursor cursor = mDb.query(true, TABLE_NAME,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String filename = "";
		String short_filename = "";
		if (cursor != null){
			cursor.moveToFirst();
			filename = cursor.getString(1);
			//get everything after last '/' (separator) 
			short_filename = filename.substring(filename.lastIndexOf('/'));
			Log.d(TAG, "Filename of Episode is: "+ short_filename);
		}
		cursor.close();
		return short_filename;	
	}
	
	public String getEpisodeTitle(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.TITLE};
		final Cursor cursor = mDb.query(true, TABLE_NAME,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String title = "";
		if (cursor != null){
			cursor.moveToFirst();
			title = cursor.getString(1);
			Log.d(TAG, "Title of Episode is: "+ title);
		}
		cursor.close();
		return title;
	}

	public boolean isEpisodeDownloaded(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DOWNLOADED};
		final Cursor cursor = mDb.query(true, TABLE_NAME,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		int downloaded = 0;  //false
		if (cursor != null){
			cursor.moveToFirst();
			downloaded = cursor.getInt(1);
			Log.d(TAG, "Episode downloaded: "+ downloaded);
		}
		cursor.close();
		if (downloaded == 0){
			return false;
		}else {
			return true;
		}
			
	}

	public String getDescription(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DESCRIPTION};
		final Cursor cursor = mDb.query(true, TABLE_NAME,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String description = "";
		if (cursor != null){
			cursor.moveToFirst();
			description = cursor.getString(1);
		}
		cursor.close();
		return description;
	}

	public int getListened(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.LISTENED};
		final Cursor cursor = mDb.query(true, TABLE_NAME,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		int listened = -1;
		if (cursor != null){
			cursor.moveToFirst();
			listened = cursor.getInt(1);
		}
		cursor.close();
		return listened;
	}
	/**
	 * Returns the row_ID of the oldest episode in the database.
	 * @return row_ID
	 */
	public long getOldestEpisode() {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DATE};
		final Cursor cursor = mDb.query(true, TABLE_NAME, columns, 
				null, null, null, null, EpisodeKeys.DATE + " ASC", "1");
		long row_ID = -1;
		if (cursor != null){
			cursor.moveToFirst();
			row_ID = cursor.getLong(0);
		}
		cursor.close();
		return row_ID;
	}

	public int getNumberOfRows() {
		final String[] columns = {EpisodeKeys._ID};
		final Cursor cursor = mDb.query(TABLE_NAME, columns, 
				null, null, null, null, null);
		int rows = 0;
		if (cursor != null){
			rows = cursor.getCount();
		}
		cursor.close();
		return rows;
	}

	   
    
}
