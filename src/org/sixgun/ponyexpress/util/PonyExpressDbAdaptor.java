package org.sixgun.ponyexpress.util;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.Podcast;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaPlayer;
import android.os.Environment;

/*
 * Helper class that handles all database interactions for the app.
 */
public class PonyExpressDbAdaptor {
	private static final int DATABASE_VERSION = 17;
	private static final String DATABASE_NAME = "PonyExpress.db";
    private static final String PODCAST_TABLE = "Podcasts";
    private static final String EPISODES_TABLE = "Episodes";
    private static final String PLAYLIST_TABLE = "Playlist";
    private static final String TEMP_PODCASTS_NAME = "Temp_podcasts";
    
    @SuppressWarnings("deprecation")
	private static final String PODCAST_TABLE_CREATE = 
    	"CREATE TABLE " + PODCAST_TABLE + " (" + 
    	PodcastKeys._ID + " INTEGER PRIMARY KEY, " +
    	PodcastKeys.NAME + " TEXT," +
    	PodcastKeys.FEED_URL + " TEXT," +
    	PodcastKeys.ALBUM_ART_URL + " TEXT," +
    	PodcastKeys.TABLE_NAME + " TEXT," +
    	PodcastKeys.TAG + " TEXT, " +
    	PodcastKeys.GROUP + " TEXT);";
    
    @SuppressWarnings("deprecation")
	private static final String NEW_PODCAST_TABLE_CREATE = 
    		"CREATE TABLE " + PODCAST_TABLE + " (" + 
    				PodcastKeys._ID + " INTEGER PRIMARY KEY, " +
    				PodcastKeys.NAME + " TEXT," +
    				PodcastKeys.FEED_URL + " TEXT," +
    				PodcastKeys.ALBUM_ART_URL + " TEXT," +
    				PodcastKeys.TABLE_NAME + " TEXT);";

    @SuppressWarnings("deprecation")
	private static final String TEMP_PODCAST_TABLE_CREATE = 
        	"CREATE TEMP TABLE " + TEMP_PODCASTS_NAME + " (" + 
        	PodcastKeys._ID + " INTEGER PRIMARY KEY, " +
        	PodcastKeys.NAME + " TEXT," +
        	PodcastKeys.FEED_URL + " TEXT," +
        	PodcastKeys.ALBUM_ART_URL + " TEXT," +
        	PodcastKeys.TABLE_NAME + " TEXT);" ;
    
    private static final String PLAYLIST_TABLE_CREATE =
    		"CREATE TABLE IF NOT EXISTS " + PLAYLIST_TABLE + " (" +
    		PodcastKeys._ID + " INTEGER PRIMARY KEY, " +
    		PodcastKeys.NAME + " TEXT," + 
    		EpisodeKeys.ROW_ID + " INTEGER," + 
    		PodcastKeys.PLAY_ORDER + " INTEGER);";
    
    private static final String NEW_UNIFIED_EPISODES_TABLE_CREATE =
    		"CREATE TABLE IF NOT EXISTS " + EPISODES_TABLE + " (" + 
    				EpisodeKeys._ID + " INTEGER PRIMARY KEY," +
                    EpisodeKeys.TITLE + " TEXT," +
                    EpisodeKeys.DATE + " INTEGER," +
                    EpisodeKeys.URL + " TEXT," +
                    EpisodeKeys.FILENAME + " TEXT," +
                    EpisodeKeys.DESCRIPTION + " TEXT," +
                    EpisodeKeys.DOWNLOADED + " INTEGER," +
                    EpisodeKeys.LISTENED + " INTEGER," +
                    EpisodeKeys.SIZE + " INTEGER," +
                    EpisodeKeys.PODCAST_ID + " INTEGER," +
                    " FOREIGN KEY (" + EpisodeKeys.PODCAST_ID + ") " +
                    		"REFERENCES " + PODCAST_TABLE + " (" + 
                    PodcastKeys._ID + "));";
    	//Upgrade to version 16 adds a duration column to this table 
    
    private static final String TEMP2_PODCAST_TABLE_CREATE = 
        	"CREATE TEMP TABLE " + TEMP_PODCASTS_NAME + " (" + 
        	PodcastKeys._ID + " INTEGER PRIMARY KEY, " +
        	PodcastKeys.NAME + " TEXT," +
        	PodcastKeys.FEED_URL + " TEXT," +
        	PodcastKeys.ALBUM_ART_URL + " TEXT);" ;
    
    private static final String NEWER_PODCAST_TABLE_CREATE = 
    		"CREATE TABLE " + PODCAST_TABLE + " (" + 
    				PodcastKeys._ID + " INTEGER PRIMARY KEY, " +
    				PodcastKeys.NAME + " TEXT," +
    				PodcastKeys.FEED_URL + " TEXT," +
    				PodcastKeys.ALBUM_ART_URL + " TEXT);";
    	
    private static final String TAG = "PonyExpressDbAdaptor";
	private PonyExpressDbHelper mDbHelper;
    private SQLiteDatabase mDb;
    public boolean mDatabaseUpgraded = false;
    
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
            db.execSQL(PODCAST_TABLE_CREATE);
            //Call onUpgrade to ensure new users get the updated db
            onUpgrade(db,1,DATABASE_VERSION);
        }

		@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    		PonyLogger.i("PonyExpress", "Upgrading database from version " + oldVersion + " to "
                    + newVersion);
    		switch (oldVersion) {
    		case 1: //New installs have version 1.
    		//Fallthrough 
    			
    		//This is commented out as no devices exist with a db version 11.
    		//It is retained as an example of how to lay out an upgrade switch/case etc.. 
    			
    		//Copy old data across to new table
//			case 11:
//				//Begin transaction
//				db.beginTransaction();
//				try {
//					//Create temp table
//					db.execSQL(TEMP_TABLE_CREATE);
//					//copy all needed columns accross (this copies all, adjust depeding on what the upgrade is)
//					db.execSQL("INSERT INTO " + TEMP_TABLE_NAME + " SELECT * FROM " +
//							TABLE_NAME + ";");
//					//Add/remove/change any new columns to get to new version
//					//Drop old table
//					db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
//					//Create new table
//					db.execSQL(EPISODE_TABLE_CREATE);
//					//INSERT into new table.
//					db.execSQL("INSERT INTO " + TABLE_NAME + " SELECT *, NULL FROM " +
//							TEMP_TABLE_NAME + ";");
//					db.execSQL("DROP TABLE " + TEMP_TABLE_NAME);				
//					db.setTransactionSuccessful();
//				} finally {
//				mDatabaseUpgraded = true;
//				db.endTransaction();
//				}
    		
    		//NOTE: no break; Fallthrough
    		case 12: //Added a playlist table to pony
    			//Begin transaction
    			db.beginTransaction();
    			try {
    				//Drop empty tables for old podcasts
    				List <String> empty_tables = findEmptyTables(db);
    				for (String table : empty_tables){
    					db.execSQL("DROP TABLE IF EXISTS " + table + ";");
    				}
    				//Create new playlist table
					db.execSQL(PLAYLIST_TABLE_CREATE);
					//No need to set mDatabaseUpgraded to true as the 
					//feeds do not need updating with this db upgrade.
					db.setTransactionSuccessful();
				} catch (SQLException e) {
					PonyLogger.e(TAG, "SQLException on db upgrade to 12", e);
				} finally {
					db.endTransaction();
				}
    			//Fallthrough
    		case 13:  //Removed Identi.ca column from Podcast table
    			//Begin transaction
    			db.beginTransaction();
    			try {
    				//Remove identica columns from Podcasts table
    				//Create Temp podcasts table without identi.ca columns 
    				db.execSQL(TEMP_PODCAST_TABLE_CREATE);
    				//Move data to temp podcasts_table
    				@SuppressWarnings("deprecation")
					String columns = PodcastKeys._ID + ", " + PodcastKeys.NAME + 
    						", " + PodcastKeys.FEED_URL + ", " + 
    						PodcastKeys.ALBUM_ART_URL + ", " + 
    						PodcastKeys.TABLE_NAME;
    				db.execSQL("INSERT INTO " + TEMP_PODCASTS_NAME + " SELECT " 
    						+ columns + " FROM " + PODCAST_TABLE + ";");
    				//Drop old podcasts table	
    				db.execSQL("DROP TABLE IF EXISTS " + PODCAST_TABLE);
					//Create new table
					db.execSQL(NEW_PODCAST_TABLE_CREATE);
					//INSERT into new table.
					db.execSQL("INSERT INTO " + PODCAST_TABLE + " SELECT * FROM " 
					+ TEMP_PODCASTS_NAME + ";");
					db.execSQL("DROP TABLE " + TEMP_PODCASTS_NAME);
    				db.setTransactionSuccessful();
    			} catch (SQLException e) {
					PonyLogger.e(TAG, "SQLException on db upgrade to 13", e);
				} finally {
					db.endTransaction();
				}
    			//Fallthrough
    		case 14:  //Upgraded to unified Episodes table with added foreign key
    			//Begin transaction
    			db.beginTransaction();
    			try {
    				//Create new Episodes table
    				db.execSQL(NEW_UNIFIED_EPISODES_TABLE_CREATE);
    				//Get a cursor of the PodEps tables
    				final String[] columns = {"name"};
    				final String selection = "type='table' AND name LIKE 'PodEps%'";
    				Cursor c = db.query("sqlite_master", columns, selection, null, null, null, null);
    				if (c.getCount() > 0){
    					//For each table Insert the data into the new table together with the 
    					//foreign key
    					c.moveToFirst();
    					for (int i=0; i < c.getCount(); i++){
    						//Get the Podcast Row_id from the PodEps* name.
    						String podcast_key = c.getString(0).substring(6);
    						//INSERT into new table.
    						final String wanted_columns = 
    								EpisodeKeys.TITLE + ", " +
    								EpisodeKeys.DATE + ", " +
    								EpisodeKeys.URL + ", " +
    								EpisodeKeys.FILENAME + ", " +
    								EpisodeKeys.DESCRIPTION + ", " +
    								EpisodeKeys.DOWNLOADED + ", " +
    								EpisodeKeys.LISTENED + ", " +
    								EpisodeKeys.SIZE ;
    						db.execSQL("INSERT INTO " + EPISODES_TABLE + "("+ 
    								wanted_columns +", "+ EpisodeKeys.PODCAST_ID + ")" 
    								+ " SELECT "+ wanted_columns + ", " + podcast_key
    								+ " FROM " + c.getString(0) + ";");
    						//Drop the old table
    						db.execSQL("DROP TABLE IF EXISTS " + c.getString(0));
    						c.moveToNext();
    					}
    				}else{
    					PonyLogger.d(TAG, "No old PodEps tables to update");
    				}
    				c.close();
    				//No need to set mDatabaseUpgraded to true as the 
					//feeds do not need updating with this db upgrade.
    				db.setTransactionSuccessful();
    			} catch (SQLException e) {
					PonyLogger.e(TAG, "SQLException on db upgrade to 14", e);
				} finally {
					db.endTransaction();
				}
    			//Fallthrough
    		case 15:
    			//Begin transaction
    			db.beginTransaction();
    			try {
    				//Upgrade the Podcasts table to remove the TableName column. 
    				//Create temp table
					db.execSQL(TEMP2_PODCAST_TABLE_CREATE);
					//Move data to temp podcasts_table
					String podcast_columns = PodcastKeys._ID + ", " + PodcastKeys.NAME + 
    						", " + PodcastKeys.FEED_URL + ", " + 
    						PodcastKeys.ALBUM_ART_URL;
    				db.execSQL("INSERT INTO " + TEMP_PODCASTS_NAME + " SELECT " 
    						+ podcast_columns + " FROM " + PODCAST_TABLE + ";");
    				//Drop old podcasts table	
    				db.execSQL("DROP TABLE IF EXISTS " + PODCAST_TABLE);
					//Create new table
					db.execSQL(NEWER_PODCAST_TABLE_CREATE);
					//INSERT into new table.
					db.execSQL("INSERT INTO " + PODCAST_TABLE + " SELECT * FROM " 
					+ TEMP_PODCASTS_NAME + ";");
					db.execSQL("DROP TABLE " + TEMP_PODCASTS_NAME);
    				
    				//No need to set mDatabaseUpgraded to true as the 
					//feeds do not need updating with this db upgrade.
    				db.setTransactionSuccessful();
    			} catch (SQLException e) {
					PonyLogger.e(TAG, "SQLException on db upgrade to 15", e);
				} finally {
					db.endTransaction();
				}
    			//Fallthrough
    		case 16:
    			//Add a column to hold each episodes duration to the episodes table
    			db.beginTransaction();
    			String add_duration_column = "ALTER TABLE " + EPISODES_TABLE + " ADD " +
    			 EpisodeKeys.DURATION + " INTEGER;";
    			try {
    				db.execSQL(add_duration_column);
    				//Set flag for PonyExpressFragActivity to update the feeds
    				//with durations.
    				mDatabaseUpgraded = true;
    				db.setTransactionSuccessful();
    			} catch (SQLException e){
    				PonyLogger.e(TAG, "SQLException on db upgrade to 16", e);
    				mDatabaseUpgraded = false;
    			} finally {
    				db.endTransaction();
    			}
    			break; //Only the final upgrade case has a break.  
			default:
				PonyLogger.e(TAG, "Unknown version:" + newVersion + " to upgrade database to.");
				break;
			}
    	}
		
		/**
		 * Find empty tables from where Podcasts have been deleted
		 *  and drop them during a database upgrade.
		 */
		@Deprecated
		private List<String> findEmptyTables(SQLiteDatabase db){
			List<String> empty_tables = new ArrayList<String>();
			//Get a list of all PodEps* tables
			final String[] columns = {"name"};
			final String selection = "type='table' AND name LIKE 'PodEps%'";
			Cursor c = db.query("sqlite_master", columns, selection, null, null, null, null);
			
			//Find the tables that are empty and do not appear in the 
			//podcast table.
			//First get a list of all current podcasts
			String[] cols = {PodcastKeys.TABLE_NAME};
			Cursor d = db.query(PODCAST_TABLE, cols, null, null, null, null, null);
			List<String> podcasts = new ArrayList<String>();
			d.moveToFirst();
			if (d != null && d.getCount() != 0){
				for (int i = 0; i < d.getCount(); i++){
					podcasts.add(d.getString(0));
					d.moveToNext();
				}
			}
			//Now find the names of empty tables
			if (c != null && c.getCount() > 0){
				c.moveToFirst();
				for (int i = 0; i < c.getCount(); i++){
					long num = DatabaseUtils.queryNumEntries(db, c.getString(0));
					PonyLogger.d(TAG, c.getString(0) + " has " + num + "rows");
					//if empty check the name isn't in the podcasts list.
					if (num == 0 && !podcasts.contains(c.getString(0))){
						PonyLogger.d(TAG, "Table " + c.getString(0) + " is to be dropped");
						empty_tables.add(c.getString(0));
					}
					c.moveToNext();
				}
			}
			d.close();
			c.close();
			
			return empty_tables;
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
    
    private long getPodcastId(String podcast_name){
    	final String[] columns = {PodcastKeys._ID};
		//Use double quote here for the podcastName as it is an identifier.
		final String quotedName = Utils.handleQuotes(podcast_name);
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "= " + quotedName, null, null, null, null, null);
		long id = 0;
		boolean cursor_not_empty;
		if (cursor != null){
			cursor_not_empty = cursor.moveToFirst();
			if (cursor_not_empty){
				id = cursor.getLong(0);
				PonyLogger.d(TAG, "Podcast Id of Episode is: " + id);
			} else {
				PonyLogger.e(TAG, "Looking for a Podcast name not in the database!");
			}
		}
		cursor.close();
    	return id;
    }
    
    /**
     * Adds each episode to the database.
     * @param episode
     * @param podcast_name
     * @return The row ID of the inserted row or -1 if an error occurred.
     */
    public long insertEpisode(Episode episode, String podcast_name) {
    	final long podcast_id = getPodcastId(podcast_name); 
        ContentValues episodeValues = new ContentValues();
        episodeValues.put(EpisodeKeys.TITLE, episode.getTitle());
        episodeValues.put(EpisodeKeys.DATE, episode.getDate().getTime());
        episodeValues.put(EpisodeKeys.URL, episode.getLink().toString());
        final String filename = episode.getLink().getFile(); 
        episodeValues.put(EpisodeKeys.FILENAME, filename);
        episodeValues.put(EpisodeKeys.DESCRIPTION, episode.getDescription());
        episodeValues.put(EpisodeKeys.DOWNLOADED, episode.beenDownloaded());
        episodeValues.put(EpisodeKeys.LISTENED, episode.beenListened());
        episodeValues.put(EpisodeKeys.SIZE, episode.getLength());
        episodeValues.put(EpisodeKeys.PODCAST_ID, podcast_id);

        return mDb.insert(EPISODES_TABLE, null, episodeValues);
    }
    

	/**
     * Deletes the episode with rowID index.
     * @param rowID
     * @param podcast_name
     * @return True if successful.
     */
	public boolean deleteEpisode(Long rowID) {
		return mDb.delete(EPISODES_TABLE, EpisodeKeys._ID + "=" + rowID, null) > 0;
	}
	
	/**
	 * Gets all unique Episode names, descriptions, durations and urls from the correct podcast table.
	 * @param podcast_name
	 * @return A Cursor object, which is positioned before the first entry
	 */
	public Cursor getAllEpisodeNamesDescriptionsAndLinks(String podcast_name){
		final long podcast_id = getPodcastId(podcast_name);
		final String[] columns = {EpisodeKeys._ID, EpisodeKeys.TITLE, EpisodeKeys.LISTENED, 
				EpisodeKeys.DESCRIPTION, EpisodeKeys.URL, EpisodeKeys.DURATION};
		return mDb.query(
				true,EPISODES_TABLE,columns,EpisodeKeys.PODCAST_ID + "=" + podcast_id,null,null,null,EpisodeKeys.DATE +" DESC" ,null);
	}
	/**
	 * Get a list of the names of all the podcasts.
	 */
	public List<String> listAllPodcasts(){
		final String[] podcast_columns = {PodcastKeys._ID, PodcastKeys.NAME};
		final Cursor podcasts_cursor = mDb.query(true, PODCAST_TABLE, podcast_columns, 
				null, null, null, null, null, null);
		List<String> podcast_names = new ArrayList<String>();
		if (podcasts_cursor != null && podcasts_cursor.getCount() > 0){
			podcasts_cursor.moveToFirst();
			for (int i = 0; i < podcasts_cursor.getCount(); i++){
				final String name = podcasts_cursor.getString(1);
				podcast_names.add(name);
				podcasts_cursor.moveToNext();
			}
		} else {
			PonyLogger.e(TAG, "empty cursor at listAllPodcasts()");
		}
		podcasts_cursor.close();
		return podcast_names;
	}
	
	public Cursor getAllListened(String podcast_name) {
		final long podcast_id = getPodcastId(podcast_name);
		final String[] columns = {EpisodeKeys._ID};
		final String where = EpisodeKeys.LISTENED + "!=? AND " + EpisodeKeys.PODCAST_ID + "=?";
		final String[] args = {String.valueOf(-1),String.valueOf(podcast_id)};
		return mDb.query(
				true,EPISODES_TABLE,columns,where,args,null,null,null,null);
	}
	
	public Cursor getAllNotListened(String podcast_name) {
		final long podcast_id = getPodcastId(podcast_name);
		final String[] columns = {EpisodeKeys._ID};
		final String where = EpisodeKeys.LISTENED + "=? AND " + EpisodeKeys.PODCAST_ID + "=?";
		final String[] args = {String.valueOf(-1),String.valueOf(podcast_id)};
		return mDb.query(
				true,EPISODES_TABLE,columns,where,args,null,null,null,null);
	}
	
	/**
	 * Gets all undownloaded and unlistened episodes excluding youtube episodes.
	 * @param the name of the Podcast
	 * @return a cursor of all undownloaded and unlistened episodes.
	 */
	public Cursor getAllUndownloadedAndUnlistened(String podcast_name) {
		final long podcast_id = getPodcastId(podcast_name);
		final String[] columns = {EpisodeKeys._ID, EpisodeKeys.TITLE, EpisodeKeys.URL,
				EpisodeKeys.SIZE};
		final String where = EpisodeKeys.PODCAST_ID + "= " + podcast_id + " AND " +
				EpisodeKeys.DOWNLOADED + "= 0 AND " + EpisodeKeys.LISTENED + "= -1 AND " + 
				EpisodeKeys.URL + " NOT LIKE '%www.youtube.com%'";
		return mDb.query(true, EPISODES_TABLE, columns, where, null, null, null, null, null);
	}
	
	/**
	 * Gets all filenames of all the files that have been downloaded and should be 
	 * on the SD Card.
	 */
	public Map<Long, String> getFilenamesOnDisk(){
		//Get all episodes that have been downloaded (and are still on disk)
		Map<Long, String> files = new HashMap<Long, String>();
		final String[] columns = {EpisodeKeys._ID, EpisodeKeys.DOWNLOADED, 
				EpisodeKeys.FILENAME};
		final Cursor cursor = mDb.query(true, EPISODES_TABLE, columns, 
				EpisodeKeys.DOWNLOADED + "!= 0", null, null, null, null, null);

		String short_filename = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			for (int i = 0; i < cursor.getCount(); i++){
				final String filename = cursor.getString(2);
				//get everything after last '/' (separator) and remove the '/'
				short_filename = filename.substring(filename.lastIndexOf('/')).substring(1);
				files.put(cursor.getLong(0),short_filename);
				cursor.moveToNext();
			}
		} else {
			PonyLogger.e(TAG, "Empty cursor at getFilenamesFromDisk()");
		}
		cursor.close();
		return files;
	}

	
	public String getEpisodeUrl(long row_ID){
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.URL};
		final Cursor cursor = mDb.query(true, EPISODES_TABLE,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String url = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			url = cursor.getString(1);
			PonyLogger.d(TAG, "Url of Episode is: " + url);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getEpisodeUrl()");
		}
		cursor.close();
		return url;	
	}
	
	/**
	 * Updates an episodes row when it has been downloaded.
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
		return mDb.update(EPISODES_TABLE, values, EpisodeKeys._ID + "=" + rowID, null) > 0;
	}
	/**
	 * Updates an episodes row when it has been listened or if the duration is being recorded.
	 * @param rowID 
	 * @param key
	 * @param newRecord
	 * @return true if update successful.
	 */
	public boolean update(long rowID, String key, int newRecord){
		ContentValues values = new ContentValues();
		if (key == EpisodeKeys.LISTENED){
			values.put(EpisodeKeys.LISTENED, newRecord);
		} else if( key == EpisodeKeys.DURATION){
			values.put(EpisodeKeys.DURATION, newRecord);
		}
		return mDb.update(EPISODES_TABLE, values, EpisodeKeys._ID + "=" + rowID, null) > 0;
	}
	/**
	 * Updates the database with a new album art url.
	 * @param rowID 
	 * @param newUrl
	 * @return true if update successful.
	 */
	public boolean update(long rowID, String newUrl){
		ContentValues values = new ContentValues();
		values.put(PodcastKeys.ALBUM_ART_URL, newUrl);
		return mDb.update(PODCAST_TABLE, values, PodcastKeys._ID + "=" + rowID, null) > 0;
	}

	/**
	 * Gets the episodes filename
	 * @param row_ID
	 * @param podcast_name
	 * @return filename in the form "/file.ext"
	 */
	public String getEpisodeFilename(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.FILENAME};
		final Cursor cursor = mDb.query(true, EPISODES_TABLE,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String filename = "";
		String short_filename = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			filename = cursor.getString(1);
			//get everything after last '/' (separator) 
			short_filename = filename.substring(filename.lastIndexOf('/'));
			PonyLogger.d(TAG, "Filename of Episode is: " + short_filename);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getEpisodeFilename()");
		}
		cursor.close();
		return short_filename;	
	}
	
	public String getEpisodeTitle(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.TITLE};
		final Cursor cursor = mDb.query(true, EPISODES_TABLE,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String title = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			title = cursor.getString(1);
		} else{
			PonyLogger.e(TAG, "Empty cursor at getEpisodeTitle()");
		}
		cursor.close();
		return title;
	}

	public boolean isEpisodeDownloaded(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DOWNLOADED};
		final Cursor cursor = mDb.query(true, EPISODES_TABLE,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		int downloaded = 0;  //false
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			downloaded = cursor.getInt(1);
			PonyLogger.d(TAG, "Episode downloaded: " + downloaded);
		} else {
			PonyLogger.e(TAG, "Empty cursor at isEpisodeDownloaded()");
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
		final Cursor cursor = mDb.query(true, EPISODES_TABLE,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String description = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			description = cursor.getString(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getDescription()");
		}
		cursor.close();
		return description;
	}

	public int getListened(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.LISTENED};
		final Cursor cursor = mDb.query(true, EPISODES_TABLE,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		int listened = -1;
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			listened = cursor.getInt(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getListened()");
		}
		cursor.close();
		return listened;
	}
	/**
	 * Returns the row_ID of the oldest episode of a podcast in the database.
	 * @param podcast_name
	 * @return row_ID
	 */
	public long getOldestEpisode(String podcast_name) {
		final long podcast_id = getPodcastId(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DATE};
		final String where = EpisodeKeys.PODCAST_ID + "=" + podcast_id;
		final Cursor cursor = mDb.query(true, EPISODES_TABLE, columns, 
				where, null, null, null, EpisodeKeys.DATE + " ASC", "1");
		long row_ID = -1;
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			row_ID = cursor.getLong(0);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getOldestEpisode()");
		}
		cursor.close();
		return row_ID;
	}

	public int getNumberOfRows(String podcast_name) {
		final long podcast_id = getPodcastId(podcast_name);
		final String[] columns = { EpisodeKeys._ID};
		final String where = EpisodeKeys.PODCAST_ID + "=" + podcast_id;
		final Cursor cursor = mDb.query(EPISODES_TABLE, columns, 
				where, null, null, null, null);
		int rows = 0;
		if (cursor != null && cursor.getCount() >= 0){
			rows = cursor.getCount();
		} else {
			PonyLogger.e(TAG,"Empty cursor at getNumberofRows()");
		}
		cursor.close();
		return rows;
	}

	public int getEpisodeSize(long row_ID) {
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.SIZE};
		final Cursor cursor = mDb.query(true, EPISODES_TABLE,
				columns, EpisodeKeys._ID + "=" + row_ID, 
				null, null, null, null, null);
		int size = 0;
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			size = cursor.getInt(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getEpisodeSize()");
		}
		cursor.close();
		return size;
	}
	
	public boolean containsEpisode(String title, String podcast_name) {
		final long podcast_id = getPodcastId(podcast_name);
		final String quotedTitle = Utils.handleQuotes(title);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.TITLE};
		final Cursor cursor = mDb.query(true, EPISODES_TABLE,
				columns, EpisodeKeys.TITLE + "=" + quotedTitle
				+ " AND " + EpisodeKeys.PODCAST_ID + "= " + podcast_id, 
				null, null, null, null, null);
		if (cursor.getCount() > 0){
			cursor.close();
			return true;
		} else {
			cursor.close();
			return false;
		}
	}
	
	/** 
	 * Takes a Podcast instance (with feed url) and gets rest of info from
	 * the feed. ie: podcast title and album art url and adds
	 * it them to the Db.
	 *  @param podcast
	 *  @return the name of the inserted podcast or null
	 */
	public String addNewPodcast(Podcast podcast){
		PodcastFeedParser parser = new PodcastFeedParser(mCtx,podcast.getFeed_Url().toString());
		Podcast new_podcast = parser.parse();
		if (new_podcast != null){
			//Insert Podcast into Podcast table
			insertPodcast(new_podcast);
			return new_podcast.getName();
		}
		return null;
	}
	
	
	/**
    * Adds each podcast to the database.
    * @param podcast
    * @return The row ID of the inserted row or -1 if an error occurred. 
    */
	private boolean insertPodcast(Podcast podcast) {
		ContentValues podcastValues = new ContentValues();
        String name = podcast.getName();
        podcastValues.put(PodcastKeys.NAME, name);
        podcastValues.put(PodcastKeys.FEED_URL, podcast.getFeed_Url().toString());
        if (podcast.getArt_Url() != null){
        	podcastValues.put(PodcastKeys.ALBUM_ART_URL, 
        			podcast.getArt_Url().toString());
        } else {
        	podcastValues.putNull(PodcastKeys.ALBUM_ART_URL);
        }     
        //Insert the record
		Long row_ID = mDb.insert(PODCAST_TABLE, null , podcastValues);
		if (row_ID != -1){
			return true;
		}else return false;
         
	}

	/**
	 * Gets all unique Podcast names from the database.
	 * @return A Cursor object, which is positioned before the first entry
	 */
	public Cursor getAllPodcastNamesAndArt() {
		final String[] columns = {PodcastKeys._ID, PodcastKeys.NAME, PodcastKeys.ALBUM_ART_URL};
		return mDb.query(
				true,PODCAST_TABLE,columns,null,null,null,null,PodcastKeys.NAME ,null);
	}

	public String getPodcastName(long row_ID) {
		final String[] columns = {PodcastKeys._ID,PodcastKeys.NAME};
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String name = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			name = cursor.getString(1);
			PonyLogger.d(TAG, "Title of Podcast is: " + name);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getPodcastName()");
		}
		cursor.close();
		return name;
	}

	public String getPodcastUrl(String podcast_name) {
		final String quotedName = Utils.handleQuotes(podcast_name);
		final String[] columns = {PodcastKeys._ID,PodcastKeys.FEED_URL};
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "=" + quotedName ,
				null, null, null, null, null);
		String url = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			url = cursor.getString(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getPodcastUrl()");
		}
		cursor.close();
		return url;
	}
	
	public List<String> getAllPodcastsUrls(){
		final String[] podcast_columns = {PodcastKeys._ID, PodcastKeys.FEED_URL};
		final Cursor podcasts_cursor = mDb.query(true, PODCAST_TABLE, podcast_columns, 
				null, null, null, null, null, null);
		List<String> podcast_urls = new ArrayList<String>();
		if (podcasts_cursor != null && podcasts_cursor.getCount() > 0){
			podcasts_cursor.moveToFirst();
			for (int i = 0; i < podcasts_cursor.getCount(); i++){
				final String name = podcasts_cursor.getString(1);
				podcast_urls.add(name);
				podcasts_cursor.moveToNext();
			}
		} else {
			PonyLogger.e(TAG, "empty cursor at getAllPodcastsUrls()");
		}
		podcasts_cursor.close();
		return podcast_urls;
	}
	
	public String getAlbumArtUrl(long row_ID){
		final String[] columns = {PodcastKeys._ID,PodcastKeys.ALBUM_ART_URL};
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys._ID + "=" + row_ID, null, null, null, null, null);
		String url = "";
		if (cursor != null  && cursor.getCount() > 0){
			cursor.moveToFirst();
			url = cursor.getString(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getAlbumArtUrl()");
		}
		cursor.close();
		return url;
	}
	
	public String getAlbumArtUrl(String podcast_name){
		final String[] columns = {PodcastKeys._ID,PodcastKeys.ALBUM_ART_URL};
		//Use double quote here for the podcastName as it is an identifier.
		final String quoted_name = Utils.handleQuotes(podcast_name);
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "=" + quoted_name, null, null, null, null, null);
		String url = "";
		if (cursor != null  && cursor.getCount() > 0){
			cursor.moveToFirst();
			url = cursor.getString(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getAlbumArtUrl()");
		}
		cursor.close();
		return url;
	}
	
	public void updateAlbumArtUrl(String podcast_url, String artUrl){
		final String[] columns = {PodcastKeys._ID,PodcastKeys.ALBUM_ART_URL,PodcastKeys.FEED_URL};
		final String quotedUrl = Utils.handleQuotes(podcast_url);
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.FEED_URL + "=" + quotedUrl, null, null, null, null, null);
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			final String old_url = cursor.getString(1);
			if (!artUrl.equals(old_url)){
				//Album art has changed
				PonyLogger.i(TAG, "Old art: " + old_url + " New art: " + artUrl);
				update(cursor.getLong(0), artUrl);
			}
		} else {
			PonyLogger.e(TAG, "Empty cursor at updateAlbumArtUrl()");
		}
		cursor.close();
	}
	
	public int countUnlistened(String podcast_name){
		final long podcast_id = getPodcastId(podcast_name);
		final String[] columns = {PodcastKeys._ID};
		Cursor c = mDb.query(
				true,EPISODES_TABLE,columns,EpisodeKeys.LISTENED + "= -1 AND " + 
		EpisodeKeys.PODCAST_ID + "="+ podcast_id,null,null,null,null,null);
		final int count = c.getCount();
		c.close();
		return count;
	}

	public boolean removePodcast(long rowID){
		final String podcast_name =  getPodcastName(rowID);
		// Podcast path is Path + podcast name.
		File rootPath = Environment.getExternalStorageDirectory();
		//Check rootpath accessible
		final String state = Environment.getExternalStorageState();
		boolean deleted = false;
		if (Environment.MEDIA_MOUNTED.equals(state)){
			//Remove any episodes of the podcast from the playlist
			final String quote_name = Utils.handleQuotes(podcast_name);
			mDb.delete(PLAYLIST_TABLE, PodcastKeys.NAME + "=" + quote_name, null);
			
			final String path = PonyExpressApp.PODCAST_PATH + podcast_name;
			PonyLogger.i(TAG, "Deleting " + path + "from SD Card");
			File podcast_path = new File(rootPath + path);
			deleted = Utils.deleteDir(podcast_path);
			//Delete episodes from episode table
			long podcast_id = getPodcastId(podcast_name);
			final String where = EpisodeKeys.PODCAST_ID + "=" + podcast_id;
			mDb.delete(EPISODES_TABLE, where, null);
			PonyLogger.i(TAG, "Removing episodes from database");
			//Remove entry from Podcasts table
			mDb.delete(PODCAST_TABLE, PodcastKeys._ID + "=" + rowID, null);
			PonyLogger.i(TAG, "Removing podcast from database");	
		}
		//Send broadcast to inform app that database changed and can now update view.
		Intent intent = new Intent("org.sixgun.ponyexpress.PODCAST_DELETED");
		mCtx.sendBroadcast(intent);
		return deleted;
	}

	public boolean checkDatabaseForUrl(Podcast podcast){
		boolean mCheckDatabase = false;
		final Cursor cursor = mDb.rawQuery("SELECT * FROM " + PODCAST_TABLE + " WHERE " + PodcastKeys.FEED_URL + "= '" + podcast.getFeed_Url().toString() + "'", null);
		if (cursor != null && cursor.getCount() > 0){
			mCheckDatabase = true;
		}
		cursor.close();
		return mCheckDatabase;	
	}
	/**
	 * Gets the Podcast name and episode row id of all episodes in
	 * the playlist.
	 * @return cursor over the playlist table
	 */
	public Cursor getPlaylist() {
		final String[] columns = {EpisodeKeys._ID, PodcastKeys.NAME, EpisodeKeys.ROW_ID};
		return mDb.query(
				true,PLAYLIST_TABLE,columns,null,null,null,null,PodcastKeys.PLAY_ORDER +" ASC" ,null);
	}
	
	public boolean playlistEmpty(){
		if (DatabaseUtils.queryNumEntries(mDb, PLAYLIST_TABLE) == 0 ){
			return true;
		} else return false;
	}
	
	/**
	 * Adds an episode to the playlist.
	 * @return the row_id if successful or already in playlist or -1 if an error.
	 */
	public long addEpisodeToPlaylist(String podcast_name, long row_id){
		//check if episode is already in playlist before adding it.
		final long rowID = episodeInPlaylist(row_id);
		if (rowID == 0){
			ContentValues episodeValues = new ContentValues();
			episodeValues.put(PodcastKeys.NAME, podcast_name);
			episodeValues.put(EpisodeKeys.ROW_ID, row_id);
			//Find number of episodes already in playlist and add 1 to get
			//next play order.
			long next_order = DatabaseUtils.queryNumEntries(mDb, PLAYLIST_TABLE);
			episodeValues.put(PodcastKeys.PLAY_ORDER, next_order +1);
			return mDb.insert(PLAYLIST_TABLE, null, episodeValues);
		} else return rowID;
		
	}
		
	/**
	 * Empties the playlist.
	 */
	public void clearPlaylist() {
		mDb.delete(PLAYLIST_TABLE, null, null);
	}
	
	/**
	 * Moves the selected episode up one in the running order.
	 * @param position
	 * @return true if successful.
	 */
	public boolean moveUpPlaylist(long position){
		//Already at the top, just return
		if (position == 0) {
			return true;
		}
		
		//Add 1 so that we are using counting numbers like the play order.
		position++;
		
		final String[] columns = {PodcastKeys._ID, PodcastKeys.PLAY_ORDER};
		final Cursor c = mDb.query(PLAYLIST_TABLE, columns, 
				PodcastKeys._ID, null, null, null, null);
		ContentValues cv = new ContentValues();
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++){
				//Move up one
				if (c.getInt(1) == position){
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1) - 1);
				}else if (c.getInt(1) == position - 1){
					//Move down one	
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1) + 1);
				}else{
					//Keep the same position
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1));
				}
				mDb.update(PLAYLIST_TABLE, cv, PodcastKeys._ID + "=" + c.getLong(0), null);
				c.moveToNext();
			}
		}else{
			PonyLogger.e(TAG, "Empty cursor at moveUpPlaylist()");
			c.close();
			return false;
		}
		c.close();
		return true;
	}
	
	/**
	 * Moves the selected episode down one in the running order.
	 * @param position
	 * @return true if successful.
	 */
	public boolean moveDownPlaylist(long position){
						
		//Add 1 so that we are using counting numbers like the play order.
		position++;
		
		final String[] columns = {PodcastKeys._ID, PodcastKeys.PLAY_ORDER};
		final Cursor c = mDb.query(PLAYLIST_TABLE, columns, 
				PodcastKeys._ID, null, null, null, null);
		ContentValues cv = new ContentValues();
		if (c != null && c.getCount() > 0){
			int last = c.getCount();
			//Check and return if already on the bottom
			if (last == position){
				c.close();
				return true;
			}
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++){
				//Move down one
				if (c.getInt(1) == position){
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1) + 1);
				}else if (c.getInt(1) == position + 1){
					//Move down one	
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1) - 1);
				}else{
					//Keep the same position
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1));
				}
				mDb.update(PLAYLIST_TABLE, cv, PodcastKeys._ID + "=" + c.getLong(0), null);
				c.moveToNext();
			}
		}else{
			PonyLogger.e(TAG, "Empty cursor at moveDownPlaylist()");
			c.close();
			return false;
		}
		c.close();
		return true;
	}
	
	/**
	 * Moves the selected episode to the top of the playlist.
	 * @param position
	 * @return true if successful
	 */
	public boolean moveToTop(long position){
		//Already at the top, just return
		if (position == 0) {
			return true;
		}
		
		//Add 1 so that we are using counting numbers like the play order.
		position++;
		
		final String[] columns = {PodcastKeys._ID, PodcastKeys.PLAY_ORDER};
		final Cursor c = mDb.query(PLAYLIST_TABLE, columns, 
				PodcastKeys._ID, null, null, null, null);
		ContentValues cv = new ContentValues();
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++){
				//Move to the top
				if (c.getInt(1) == position){
					cv.put(PodcastKeys.PLAY_ORDER, 1);
				}
				//Move down one
				if(c.getInt(1) < position){
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1) + 1);
				}
				//Keep the same position
				if(c.getInt(1) > position){
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1));
				}
				mDb.update(PLAYLIST_TABLE, cv, PodcastKeys._ID + "=" + c.getLong(0), null);
				c.moveToNext();
			}
		}else{
			PonyLogger.e(TAG, "Empty cursor at moveToTop()");
			c.close();
			return false;
		}
		c.close();
		return true;
	}
	
	/**
	 * Moves the selected episode to the bottom of the playlist.
	 * @param position
	 * @return true if successful
	 */
	public boolean moveToBottom(long position){
		
		//Add 1 so that we are using counting numbers like the play order.
		position++;
		
		final String[] columns = {PodcastKeys._ID, PodcastKeys.PLAY_ORDER};
		final Cursor c = mDb.query(PLAYLIST_TABLE, columns, 
				PodcastKeys._ID, null, null, null, null);
		ContentValues cv = new ContentValues();
		if (c != null && c.getCount() > 0){
			int last = c.getCount();
			//Check and return if already on the bottom
			if (last == position){
				c.close();
				return true;
			}
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++){
				//Move to the bottom
				if (c.getInt(1) == position){
					cv.put(PodcastKeys.PLAY_ORDER, last);
				}
				//Move up one
				if(c.getInt(1) > position){
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1) - 1);
				}
				//Keep the same position
				if(c.getInt(1) < position){
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1));
				}
				mDb.update(PLAYLIST_TABLE, cv, PodcastKeys._ID + "=" + c.getLong(0), null);
				c.moveToNext();
			}
		}else{
			PonyLogger.e(TAG, "Empty cursor at moveToBottom()");
			c.close();
			return false;
		}
		c.close();
		return true;
	}
	/**
	 * Returns the podcast name for the episode with id in the playlist
	 * table
	 * @params id
	 * @return podcast name
	 */
	public String getPodcastFromPlaylist(long id){
		final String[] columns = {PodcastKeys._ID, PodcastKeys.NAME};
		final Cursor cursor = mDb.query(true, PLAYLIST_TABLE,
				columns, PodcastKeys._ID + "=" + id ,
				null, null, null, null, null);
		String podcast_name = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			podcast_name = cursor.getString(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getPodcastFromPlaylist(long)");
		}
		cursor.close();
		return podcast_name;
	}
	
	/**
	 * Returns the Podcast name for the next episode on the playlist.
	 * @return podcast name
	 */
	public String getPodcastFromPlaylist() {
		final String[] columns = {PodcastKeys._ID, PodcastKeys.NAME};
		final Cursor cursor = mDb.query(true, PLAYLIST_TABLE,
				columns, PodcastKeys.PLAY_ORDER + "=" + 1 ,
				null, null, null, null, null);
		String podcast_name = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			podcast_name = cursor.getString(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getPodcastFromPlaylist()");
		}
		cursor.close();
		return podcast_name;
	}
	/**
	 * Returns the episode id for the next episode on the playlist.
	 * @return row_id
	 */
	public long getEpisodeFromPlaylist() {
		final String[] columns = {PodcastKeys._ID, EpisodeKeys.ROW_ID};
		final Cursor cursor = mDb.query(true, PLAYLIST_TABLE,
				columns, PodcastKeys.PLAY_ORDER + "=" + 1 ,
				null, null, null, null, null);
		long episode_id = 0;
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			episode_id = cursor.getLong(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getEpisodeFromPlaylist()");
		}
		cursor.close();
		return episode_id;
	}
	/**
	 * Returns the episode id for the episode with id in the playlist.
	 * @params id in playlist table
	 * @return row_id in podcast table
	 */
	public long getEpisodeFromPlaylist(long id) {
		final String[] columns = {PodcastKeys._ID, EpisodeKeys.ROW_ID};
		final Cursor cursor = mDb.query(true, PLAYLIST_TABLE,
				columns, PodcastKeys._ID + "=" + id ,
				null, null, null, null, null);
		long episode_id = 0;
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			episode_id = cursor.getLong(1);
		} else {
			PonyLogger.e(TAG, "Empty cursor at getEpisodeFromPlaylist()");
		}
		cursor.close();
		return episode_id;
	}
	/**
	 * Removes the top episode from the playlist table
	 * and reorders the remaining episodes
	 */
	public void popPlaylist() {
		mDb.delete(PLAYLIST_TABLE, PodcastKeys.PLAY_ORDER + "=" + 1, null);
		//Query the table for all the current play_orders
		final String[] columns = {PodcastKeys._ID, PodcastKeys.PLAY_ORDER};
		final Cursor c = mDb.query(PLAYLIST_TABLE, columns, 
				PodcastKeys._ID, null, null, null, null);
		int old_order = 0;
		ContentValues cv = new ContentValues();
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++){
				//Get the old play_order
				old_order = c.getInt(1);
				cv.put(PodcastKeys.PLAY_ORDER, old_order - 1);
				//Update the play_order by -1
				mDb.update(PLAYLIST_TABLE, cv, PodcastKeys._ID + "=" + c.getLong(0), null);
				c.moveToNext();
			}
		}
		c.close();
	}
	
	/**
	 * Returns true if there is only one episode left in the playlist.
	 * @return
	 */
	public boolean playlistEnding() {
		if (DatabaseUtils.queryNumEntries(mDb, PLAYLIST_TABLE) == 1 ){
			return true;
		} else return false;
		
	}

	public void removeEpisodeFromPlaylist(String podcast_name, long rowID) {
		final String quoted_name = Utils.handleQuotes(podcast_name);
		//Get the play order number for this episode so we can fill
		// the gap left later.
		final String[] columns = {PodcastKeys._ID, PodcastKeys.PLAY_ORDER};
		final Cursor c = mDb.query(PLAYLIST_TABLE, columns, 
				PodcastKeys.NAME + "="+ quoted_name + " AND " + 
						EpisodeKeys.ROW_ID + "=" + rowID,
						null, null, null, null);
		int play_order_position = 0;
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			play_order_position = c.getInt(1);
			c.close();
		} else {
			PonyLogger.e(TAG, "Null cursor returned by removeEpisodeFromPlaylist(String,long)");
			c.close();
		}
				
		if (play_order_position != 0){
			//Remove episode
			mDb.delete(PLAYLIST_TABLE, 
					PodcastKeys.NAME + "="+ quoted_name + " AND " + 
			EpisodeKeys.ROW_ID + "=" + rowID , null);
			
			fillGap(play_order_position);
		}		
	}

	public void removeEpisodeFromPlaylist(String podcastName, String title) {
		//Find the rowId of the episode in the episode table
		long rowId;
		try {
			rowId = getRowIdOfEpisode(podcastName, title);
			//Remove from the Playlist table
			removeEpisodeFromPlaylist(podcastName, rowId);
		} catch (EpisodeNotFoundException e) {
			PonyLogger.e(TAG, "Seaching for an unknown episode", e);
		}
		
		
	}
	
	private void fillGap(int position){
		final String[] columns = {PodcastKeys._ID, PodcastKeys.PLAY_ORDER};
		final Cursor c = mDb.query(PLAYLIST_TABLE, columns, 
				PodcastKeys._ID, null, null, null, null);
		ContentValues cv = new ContentValues();
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++){
				if (c.getInt(1) > position){
					cv.put(PodcastKeys.PLAY_ORDER, c.getInt(1) - 1);
					mDb.update(PLAYLIST_TABLE, cv, PodcastKeys._ID + "=" + c.getLong(0), null);
				}else if (c.getInt(1) == position){
					PonyLogger.e(TAG, "This episode should be gone! Shouldn't be here!");
				}
				c.moveToNext();
			}
		}else{
			PonyLogger.e(TAG, "Empty cursor at fillGap(int)");
			c.close();
			return;
		}
		c.close();
		return;
	}
	/**
	 * Checks to see if an episode is in the playlist
	 * @param row_id of episdoe in Episode table
	 * @return row_id in playlist table or 0
	 */
	private long episodeInPlaylist(long row_id){
		long rowID = 0;
		final String[] columns = {PodcastKeys._ID};
		final Cursor c = mDb.query(PLAYLIST_TABLE, columns, 
				EpisodeKeys.ROW_ID + "="+ row_id,
						null, null, null, null); 
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			rowID = c.getLong(0);
		}
		c.close();
		return rowID;
	}
	
	/**
	 * Check if episode is already in the playlist
	 * @return true if present
	 */
	public boolean episodeInPlaylist(String podcast_name, String episode_title){
		long rowId = -1;
		try {
			rowId = getRowIdOfEpisode(podcast_name, episode_title);
		} catch (EpisodeNotFoundException e) {
			PonyLogger.e(TAG, "Seaching for an unknown episode", e);
			return false;
		}
		final String quoted_name = Utils.handleQuotes(podcast_name);
		final String[] columns = {PodcastKeys._ID};
		final Cursor c = mDb.query(PLAYLIST_TABLE, columns, 
				PodcastKeys.NAME + "="+ quoted_name + " AND " + 
						EpisodeKeys.ROW_ID + "=" + rowId,
						null, null, null, null); 
		if (c.getCount() > 0){
			c.close();
			return true;
		} else {
			c.close();
			return false;
		}
	}
	
	/**
	 * Looks up the episode table row_id of a particular episode.
	 * @throws EpisodeNotFoundException 
	 */
	private long getRowIdOfEpisode(String podcast_name, String episode_title) throws EpisodeNotFoundException{
		long rowId = -1;
		final long podcast_id = getPodcastId(podcast_name);
		final String quotedTitle = Utils.handleQuotes(episode_title);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.TITLE};
		final Cursor cursor = mDb.query(true, EPISODES_TABLE,
				columns, EpisodeKeys.PODCAST_ID + "=" + podcast_id + " AND " +
		EpisodeKeys.TITLE + "=" + quotedTitle, 
				null, null, null, null, null);
		if (cursor.getCount() > 0){
			cursor.moveToFirst();
			rowId = cursor.getLong(0);						
			}
		cursor.close();
		if (rowId == -1){
			throw new EpisodeNotFoundException();
		}
		return rowId;
	}

	/**
	 * Compilies the auto playlist
	 * @return true if successful
	 */
	public boolean compileAutoPlaylist() {
		clearPlaylist();
		return compilePlaylistByDate();
	}
	
	public void recompileAutoPlaylist(){
		//Check for a partially listened episode at the top of the current playlist
		//Want to retain this episode as the user hasn't finished it.
		final long episode_id = getEpisodeFromPlaylist();
		final String podcast_name = getPodcastFromPlaylist();
		boolean retainTopEpisode = false;
		if (getListened(episode_id) > -1){
			retainTopEpisode = true;
		}
		clearPlaylist();
		if (retainTopEpisode){
			addEpisodeToPlaylist(podcast_name, episode_id);
		}
		compilePlaylistByDate();
		
	}

	private boolean compilePlaylistByDate() {
		final Cursor c = getAllUnlistenedDownloadedEpisodesByDate();
		return compilePlaylist(c);
	}

	private boolean compilePlaylist(Cursor c) {
		if (c.getCount() > 0){
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++){
				addEpisodeToPlaylist(getPodcastName(c.getLong(0)), c.getLong(1));
				c.moveToNext();
			}
			return true;
		} else return false;
		
	}

	private Cursor getAllUnlistenedDownloadedEpisodesByDate() {
		final String[] columns = {EpisodeKeys.PODCAST_ID, EpisodeKeys._ID};
		final String where = EpisodeKeys.DOWNLOADED + "= 1 AND " + 
		EpisodeKeys.LISTENED + "= -1 AND " + EpisodeKeys.URL + " NOT LIKE '%www.youtube.com%'";
		Cursor c = mDb.query(true, EPISODES_TABLE, columns, where, null, null,
				null, EpisodeKeys.DATE + " ASC", null );
		return c;
		
	}
	
	/**
	 * Gets the duration of each episode already stored in the episode table.
	 * Only used during upgrade to version 17.
	 * @param db 
	 */
	public void getAllDurations(){
		//Get Cursor of all episodes without a duration
		final String[] columns = {EpisodeKeys._ID};
		final String where = EpisodeKeys.DURATION + " ISNULL AND " + EpisodeKeys.DOWNLOADED +"=1";
		Cursor c = mDb.query(true, EPISODES_TABLE, columns, where, null, null, null, null, null);
		PonyLogger.d(TAG, "Episodes with no duration: "+ c.getCount());
		if (c.getCount()>0){
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++){
				int duration = getDuration(c.getLong(0));
				update(c.getLong(0), EpisodeKeys.DURATION, duration);
				c.moveToNext();
			}
		}
		c.close();
	}

	public int getDuration(long row_id) {
		MediaPlayer mp = new MediaPlayer();
		
		final String podcast_name = getPodcastNameForEpisode(row_id);
		final String file = getEpisodeFilename(row_id);
		final String path = PonyExpressApp.PODCAST_PATH + podcast_name + file;
		
		try {
			mp.setDataSource(new File(Environment.getExternalStorageDirectory(),path).
					getAbsolutePath());
			mp.prepare();
		} catch (IllegalArgumentException e) {
			PonyLogger.e(TAG, "Illegal path supplied to player", e);
			e.printStackTrace();
		} catch (SecurityException e) {
			PonyLogger.e(TAG,"Player cannot access path",e);
			e.printStackTrace();
		} catch (IllegalStateException e) {
			PonyLogger.e(TAG, "Player is not set up correctly", e);
			e.printStackTrace();
		} catch (IOException e) {
			PonyLogger.e(TAG,"Player cannot access path",e);
			e.printStackTrace();
		}
		int duration = mp.getDuration();
		mp.reset();
		mp.release();
		return duration;
	}

	
	private String getPodcastNameForEpisode(long row_id){
		final String query = "SELECT " + PodcastKeys.NAME + " FROM " + PODCAST_TABLE +
				" JOIN " + EPISODES_TABLE + " ON " + PODCAST_TABLE + "." + PodcastKeys._ID +
				"=" + EPISODES_TABLE + "." + EpisodeKeys.PODCAST_ID + " WHERE " + EPISODES_TABLE +
				"." + EpisodeKeys._ID + "=" + row_id;
		Cursor c = mDb.rawQuery(query, null);
		String podcast_name = "";
		if (c.getCount()> 0){
			c.moveToFirst();
			podcast_name = c.getString(0);
			PonyLogger.d(TAG, "Podcast name is " + podcast_name);
		}
		c.close();
		return podcast_name;
	}

	public int getPlaylistTime() {
		//Get cursor over playlist table with a join to episodes.duration
		final String query = "SELECT " + EpisodeKeys.DURATION +", " + EpisodeKeys.LISTENED 
				+ " FROM " + EPISODES_TABLE +
				" JOIN " + PLAYLIST_TABLE + " ON " + EPISODES_TABLE + "." + EpisodeKeys._ID +
				"=" + PLAYLIST_TABLE+ "." + EpisodeKeys.ROW_ID;
		Cursor c = mDb.rawQuery(query, null);
		int total_duration = 0;
		if (c.getCount() > 0){
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++){
				int elapsed = c.getInt(1);
				if (elapsed == -1){ //not listened too yet.
					elapsed = 0; 
				}
				total_duration += c.getInt(0) - elapsed;
				c.moveToNext();
			}
		}
		c.close();
		return total_duration;
	}

}
