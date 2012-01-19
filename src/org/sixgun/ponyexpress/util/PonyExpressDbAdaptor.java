package org.sixgun.ponyexpress.util;


import java.io.File;
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
import android.os.Environment;
import android.util.Log;

/*
 * Helper class that handles all database interactions for the app.
 */
public class PonyExpressDbAdaptor {
	private static final int DATABASE_VERSION = 13;
	private static final String DATABASE_NAME = "PonyExpress.db";
    private static final String PODCAST_TABLE = "Podcasts";
    private static final String PLAYLIST_TABLE = "Playlist";
    private static final String TEMP_TABLE_NAME = "Temp_Episodes";
    private static final String EPISODE_TABLE_FIELDS =
                " (" +
                EpisodeKeys._ID + " INTEGER PRIMARY KEY," +
                EpisodeKeys.TITLE + " TEXT," +
                EpisodeKeys.DATE + " INTEGER," +
                EpisodeKeys.URL + " TEXT," +
                EpisodeKeys.FILENAME + " TEXT," +
                EpisodeKeys.DESCRIPTION + " TEXT," +
                EpisodeKeys.DOWNLOADED + " INTEGER," +
                EpisodeKeys.LISTENED + " INTEGER," +
                EpisodeKeys.SIZE + " INTEGER);";
//  FIXME Updates to Database do not yet account for podcasts table or multiple episodes tables
    @SuppressWarnings("unused")
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
    private static final String PODCAST_TABLE_CREATE = 
    	"CREATE TABLE " + PODCAST_TABLE + " (" + 
    	PodcastKeys._ID + " INTEGER PRIMARY KEY, " +
    	PodcastKeys.NAME + " TEXT," +
    	PodcastKeys.FEED_URL + " TEXT," +
    	PodcastKeys.ALBUM_ART_URL + " TEXT," +
    	PodcastKeys.TABLE_NAME + " TEXT," + 
    	PodcastKeys.TAG + " TEXT," +
    	PodcastKeys.GROUP + " TEXT);";
    
    private static final String PLAYLIST_TABLE_CREATE =
    		"CREATE TABLE " + PLAYLIST_TABLE + " (" +
    		PodcastKeys._ID + " INTEGER PRIMARY KEY, " +
    		PodcastKeys.NAME + " TEXT," + 
    		EpisodeKeys.ROW_ID + " INTEGER," + 
    		PodcastKeys.PLAY_ORDER + " INTEGER);";
    	
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
    		Log.w("PonyExpress", "Upgrading database from version " + oldVersion + " to "
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
    		case 12:
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
					Log.e(TAG, "SQLException on db upgrade", e);
				} finally {
					db.endTransaction();
				}
    			break; //Only the final upgrade case has a break.   			
			default:
				Log.e(TAG, "Unknow version:" + newVersion + " to upgrade database to.");
				break;
			}
    	}
		
		/**
		 * Find empty tables from where Podcasts have been deleted
		 *  and drop them during a database upgrade.
		 */
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
					Log.d(TAG, c.getString(0) + " has " + num + "rows");
					//if empty check the name isn't in the podcasts list.
					if (num == 0 && !podcasts.contains(c.getString(0))){
						Log.d(TAG, "Table " + c.getString(0) + " is to be dropped");
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
    
    private String getTableName(String podcastName) {
		final String[] columns = {PodcastKeys.TABLE_NAME};
		//Use double quote here for the podcastName as it is an identifier.
		final String quotedName = "\"" + podcastName + "\"";
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "= " + quotedName, null, null, null, null, null);
		String tablename = "";
		boolean cursor_not_empty;
		if (cursor != null){
			cursor_not_empty = cursor.moveToFirst();
			if (cursor_not_empty){
				tablename = cursor.getString(0);
				Log.d(TAG, "Tablename of Episode is: "+ tablename);
			} else {
				Log.e(TAG, "Looking for a Podcast name not in the database!");
			}
		}
		cursor.close();
		return tablename;	
	}
    
    /**
     * Adds each episode to the database.
     * @param episode
     * @param podcast_name
     * @return The row ID of the inserted row or -1 if an error occurred.
     */
    public long insertEpisode(Episode episode, String podcast_name) {
    	//FIXME Check if episode is already in the database first?.
    	final String table_name = getTableName(podcast_name); 
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

        return mDb.insert(table_name, null, episodeValues);
    }
    

	/**
     * Deletes the episode with rowID index.
     * @param rowID
     * @param podcast_name
     * @return True if successful.
     */
	public boolean deleteEpisode(Long rowID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		return mDb.delete(table_name, EpisodeKeys._ID + "=" + rowID, null) > 0;
	}
	/**
	 * Gets all unique Episode names from the correct podcast table.
	 * @param podcast_name
	 * @return A Cursor object, which is positioned before the first entry
	 */
	public Cursor getAllEpisodeNames(String podcast_name){
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID, EpisodeKeys.TITLE, EpisodeKeys.LISTENED};
		return mDb.query(
				true,table_name,columns,null,null,null,null,EpisodeKeys.DATE +" DESC" ,null);
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
			Log.e(TAG, "empty cursor at listAllPodcasts()");
		}
		podcasts_cursor.close();
		return podcast_names;
	}
	
	public Cursor getAllListened(String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID};
		return mDb.query(
				true,table_name,columns,EpisodeKeys.LISTENED + "!= -1",null,null,null,null,null);
	}
	
	public Cursor getAllNotListened(String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID};
		return mDb.query(
				true,table_name,columns,EpisodeKeys.LISTENED + "= -1",null,null,null,null,null);
	}
	
	/**
	 * Gets all undownloaded and unlistened episodes.
	 * @param the name of the Podcast
	 * @return a cursor of all undownloaded and unlistened episodes.
	 */
	public Cursor getAllUndownloadedAndUnlistened(String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID, EpisodeKeys.TITLE, EpisodeKeys.URL,
				EpisodeKeys.SIZE};
		final String where = EpisodeKeys.DOWNLOADED + "= 0 AND " + EpisodeKeys.LISTENED + "= -1";
		return mDb.query(true, table_name, columns, where, null, null, null, null, null);
	}
	
	/**
	 * Gets all filenames of all the files that have been downloaded and should be 
	 * on the SD Card.
	 */
	public Map<Long, String> getFilenamesOnDisk(String podcast_name){
		//Get all episodes from correct table that have been downloaded (and are still on disk)
		final String table_name = getTableName(podcast_name);
		Map<Long, String> files = new HashMap<Long, String>();
		if (table_name != "") { // Podcast not in database for some reason
			final String[] columns = {EpisodeKeys._ID, EpisodeKeys.DOWNLOADED, 
					EpisodeKeys.FILENAME};
			final Cursor cursor = mDb.query(true, table_name, columns, 
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
				Log.e(TAG, "Empty cursor at getFilenamesFromDisk()");
			}
			cursor.close();
		}
		return files;
	}
	/**
	 * Method to determine the date of the latest episode in the database.
	 * @param podcast_name
	 * @return The Latest episodes date as a string.
	 */
//	public Date getLatestEpisodeDate(String podcast_name){
//		final String table_name = getTableName(podcast_name);
//		final String[] columns = {EpisodeKeys.DATE};
//		final Cursor cursor = mDb.query(table_name, 
//				columns, null, null, null, null, EpisodeKeys.DATE +" DESC", "1");
//		final boolean result = cursor.moveToFirst();
//		//Check the cursor is at a row.
//		if (result == true){
//			final Long latest_date = cursor.getLong(0);
//			cursor.close();
//			Log.d("PonyExpressDbAdaptor","Latest date is:" + latest_date.toString());
//			return new Date(latest_date);
//		}else{
//			cursor.close();
//			//return date 0 milliseconds
//			return new Date(0);
//		}
//	}
	
	public String getEpisodeUrl(long row_ID, String podcast_name){
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.URL};
		final Cursor cursor = mDb.query(true, table_name,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String url = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			url = cursor.getString(1);
			Log.d(TAG, "Url of Episode is: "+ url);
		} else {
			Log.e(TAG, "Empty cursor at getEpisodeUrl()");
		}
		cursor.close();
		return url;	
	}
	
	/**
	 * Updates the database when a podcast has been downloaded.
	 * @param podcast_name
	 * @param rowID 
	 * @param key
	 * @param newRecord
	 * @return true if update successful.
	 */
	public boolean update(String podcast_name, long rowID, String key, String newRecord) {
		final String table_name = getTableName(podcast_name);
		ContentValues values = new ContentValues();
		//Change ContentValues as required
		if (key == EpisodeKeys.DOWNLOADED){
			if (newRecord == "true"){
				values.put(EpisodeKeys.DOWNLOADED, true);
			}else {
				values.put(EpisodeKeys.DOWNLOADED,false);
			}
		}
		return mDb.update(table_name, values, EpisodeKeys._ID + "=" + rowID, null) > 0;
	}
	/**
	 * Updates the database when a podcast has been listened to.
	 * @param podcast_name
	 * @param rowID 
	 * @param key
	 * @param newRecord
	 * @return true if update successful.
	 */
	public boolean update(String podcast_name, long rowID, String key, int newRecord){
		final String table_name = getTableName(podcast_name);
		ContentValues values = new ContentValues();
		if (key == EpisodeKeys.LISTENED){
			values.put(EpisodeKeys.LISTENED, newRecord);
		}
		return mDb.update(table_name, values, EpisodeKeys._ID + "=" + rowID, null) > 0;
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
	public String getEpisodeFilename(long row_ID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.FILENAME};
		final Cursor cursor = mDb.query(true, table_name,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String filename = "";
		String short_filename = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			filename = cursor.getString(1);
			//get everything after last '/' (separator) 
			short_filename = filename.substring(filename.lastIndexOf('/'));
			Log.d(TAG, "Filename of Episode is: "+ short_filename);
		} else {
			Log.e(TAG, "Empty cursor at getEpisodeFilename()");
		}
		cursor.close();
		return short_filename;	
	}
	
	public String getEpisodeTitle(long row_ID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.TITLE};
		final Cursor cursor = mDb.query(true, table_name,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String title = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			title = cursor.getString(1);
		} else{
			Log.e(TAG, "Empty cursor at getEpisodeTitle()");
		}
		cursor.close();
		return title;
	}

	public boolean isEpisodeDownloaded(long row_ID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DOWNLOADED};
		final Cursor cursor = mDb.query(true, table_name,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		int downloaded = 0;  //false
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			downloaded = cursor.getInt(1);
			Log.d(TAG, "Episode downloaded: "+ downloaded);
		} else {
			Log.e(TAG, "Empty cursor at isEpisodeDownloaded()");
		}
		cursor.close();
		if (downloaded == 0){
			return false;
		}else {
			return true;
		}
			
	}

	public String getDescription(long row_ID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DESCRIPTION};
		final Cursor cursor = mDb.query(true, table_name,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String description = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			description = cursor.getString(1);
		} else {
			Log.e(TAG, "Empty cursor at getDescription()");
		}
		cursor.close();
		return description;
	}

	public int getListened(long row_ID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.LISTENED};
		final Cursor cursor = mDb.query(true, table_name,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		int listened = -1;
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			listened = cursor.getInt(1);
		} else {
			Log.e(TAG, "Empty cursor at getListened()");
		}
		cursor.close();
		return listened;
	}
	/**
	 * Returns the row_ID of the oldest episode in the database.
	 * @param podcast_name
	 * @return row_ID
	 */
	public long getOldestEpisode(String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DATE};
		final Cursor cursor = mDb.query(true, table_name, columns, 
				null, null, null, null, EpisodeKeys.DATE + " ASC", "1");
		long row_ID = -1;
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			row_ID = cursor.getLong(0);
		} else {
			Log.e(TAG, "Empty cursor at getOldestEpisode()");
		}
		cursor.close();
		return row_ID;
	}

	public int getNumberOfRows(String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = { EpisodeKeys._ID};
		final Cursor cursor = mDb.query(table_name, columns, 
				null, null, null, null, null);
		int rows = 0;
		if (cursor != null && cursor.getCount() >= 0){
			rows = cursor.getCount();
		} else {
			Log.e(TAG,"Empty cursor at getNumberofRows()");
		}
		cursor.close();
		return rows;
	}

	public int getEpisodeSize(long row_ID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.SIZE};
		final Cursor cursor = mDb.query(true, table_name,
				columns, EpisodeKeys._ID + "=" + row_ID, 
				null, null, null, null, null);
		int size = 0;
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			size = cursor.getInt(1);
		} else {
			Log.e(TAG, "Empty cursor at getEpisodeSize()");
		}
		cursor.close();
		return size;
	}
	
	public boolean containsEpisode(String title, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String quotedTitle = Utils.handleQuotes(title);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.TITLE};
		final Cursor cursor = mDb.query(true, table_name,
				columns, EpisodeKeys.TITLE + "=" + quotedTitle, 
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
	 * Takes a Podcast instance (with feed url, and identi.ca info) and gets rest of info from
	 * the feed. ie: podcast title and album art url and adds
	 * it them to the Db.
	 *  @param podcast
	 *  @return the name of the inserted podcast or null
	 */
	public String addNewPodcast(Podcast podcast){
		PodcastFeedParser parser = new PodcastFeedParser(mCtx,podcast.getFeed_Url().toString());
		Podcast new_podcast = parser.parse();
		if (new_podcast != null){
			new_podcast.setIdenticaTag(podcast.getIdenticaTag());
			new_podcast.setIdenticaGroup(podcast.getIdenticaGroup());
			//Insert Podcast into Podcast table
			insertPodcast(new_podcast);
			
			//Create table for this podcast's episodes
			String tableName = getTableName(new_podcast.getName());
			mDb.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + EPISODE_TABLE_FIELDS);
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
        
        podcastValues.put(PodcastKeys.TAG, podcast.getIdenticaTag());
        podcastValues.put(PodcastKeys.GROUP, podcast.getIdenticaGroup());
        podcastValues.putNull(PodcastKeys.TABLE_NAME);
     
        //Insert the record and then update it with the created Episode table name
		Long row_ID = mDb.insert(PODCAST_TABLE, null , podcastValues);
		String tablename = "PodEps";
		if (row_ID != -1){
			tablename = tablename + row_ID;
			podcastValues.put(PodcastKeys.TABLE_NAME,tablename);
			final String quotedName = Utils.handleQuotes(name);
			mDb.update(PODCAST_TABLE, podcastValues, PodcastKeys.NAME + "=" + quotedName , null);
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
			Log.d(TAG, "Title of Podcast is: "+ name);
		} else {
			Log.e(TAG, "Empty cursor at getPodcastName()");
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
			Log.e(TAG, "Empty cursor at getPodcastUrl()");
		}
		cursor.close();
		return url;
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
			Log.e(TAG, "Empty cursor at getAlbumArtUrl()");
		}
		cursor.close();
		return url;
	}
	
	public String getAlbumArtUrl(String podcast_name){
		final String[] columns = {PodcastKeys._ID,PodcastKeys.ALBUM_ART_URL};
		//Use double quote here for the podcastName as it is an identifier.
		final String quoted_name = "\"" + podcast_name + "\"";
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "=" + quoted_name, null, null, null, null, null);
		String url = "";
		if (cursor != null  && cursor.getCount() > 0){
			cursor.moveToFirst();
			url = cursor.getString(1);
		} else {
			Log.e(TAG, "Empty cursor at getAlbumArtUrl()");
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
				Log.d(TAG, "Old art: " + old_url + " New art: " + artUrl);
				update(cursor.getLong(0), artUrl);
			}
		} else {
			Log.e(TAG, "Empty cursor at updateAlbumArtUrl()");
		}
		cursor.close();
	}

	public String getIdenticaTag(String podcast_name) {
		final String quotedName = Utils.handleQuotes(podcast_name);
		final String[] columns = {PodcastKeys._ID,PodcastKeys.TAG};
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "=" + quotedName ,
				null, null, null, null, null);
		String url = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			url = cursor.getString(1);
		} else {
			Log.e(TAG, "Empty cursor at getIdenticaTag()");
		}
		cursor.close();
		return url;
	}

	public String getIdenticaGroup(String podcast_name) {
		final String quotedName = Utils.handleQuotes(podcast_name);
		final String[] columns = {PodcastKeys._ID,PodcastKeys.GROUP};
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "=" + quotedName ,
				null, null, null, null, null);
		String url = "";
		if (cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			url = cursor.getString(1);
		} else {
			Log.e(TAG, "Empty cursor at getIdenticaGroup()");
		}
		cursor.close();
		return url;
	}
	
	public int countUnlistened(String podcast_name){
		final String table_name = getTableName(podcast_name);
		final String[] columns = {PodcastKeys._ID};
		Cursor c = mDb.query(
				true,table_name,columns,EpisodeKeys.LISTENED + "= -1",
				null,null,null,null,null);
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
			final String path = PonyExpressApp.PODCAST_PATH + podcast_name;
			Log.d(TAG, "Deleting " + path + "from SD Card");
			File podcast_path = new File(rootPath + path);
			deleted = Utils.deleteDir(podcast_path);
			//Delete episodes from podcast episode table
			String table_name = getTableName(podcast_name);
			mDb.delete(table_name, null, null);
			Log.d(TAG, "Removing episodes from database");
			//Remove entry from Podcasts table
			mDb.delete(PODCAST_TABLE, PodcastKeys._ID + "=" + rowID, null);
			Log.d(TAG, "Removing podcast from database");
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
	 * @return the row_id if successful or -1.
	 */
	public long addEpisodeToPlaylist(String podcast_name, long row_id){
		ContentValues episodeValues = new ContentValues();
		episodeValues.put(PodcastKeys.NAME, podcast_name);
		episodeValues.put(EpisodeKeys.ROW_ID, row_id);
		//Find number of episodes already in playlist and add 1 to get
		//next play order.
		long next_order = DatabaseUtils.queryNumEntries(mDb, PLAYLIST_TABLE);
		episodeValues.put(PodcastKeys.PLAY_ORDER, next_order +1);
		return mDb.insert(PLAYLIST_TABLE, null, episodeValues);
	}
	
	/**
	 * Deletes an episode from the playlist.
	 * @param podcast_name
	 * @param row_id
	 */
	public void removeEpisodeFromPlaylist(String podcast_name, long row_id){
		//TODO
	}
	
	/**
	 * Empties the playlist.
	 */
	public void clearPlaylist() {
		mDb.delete(PLAYLIST_TABLE, null, null);
	}
	
	/**
	 * Moves the selected episode up one in the running order.
	 * @param row_id
	 * @return true if successful.
	 */
	public boolean moveUpPlaylist(long row_id){
		//TODO
		return false;
	}
	
	/**
	 * Moves the selected episode down one in the running order.
	 * @param row_id
	 * @return true if successful.
	 */
	public boolean moveDownPlaylist(long row_id){
		//TODO
		return false;
	}
	
	/**
	 * Moves the selected episode to the top of the playlist.
	 * @return true if successful
	 */
	public boolean moveToTop(long row_id){
		//TODO
		return false;
	}
	
	/**
	 * Moves the selected episode to the bottom of the playlist.
	 * @return true if successful
	 */
	public boolean moveToBotoom(long row_id){
		//TODO
		return false;
	}

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
			Log.e(TAG, "Empty cursor at getPodcastFromPlaylist()");
		}
		cursor.close();
		return podcast_name;
	}

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
			Log.e(TAG, "Empty cursor at getEpisodeFromPlaylist()");
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
	
	
}
