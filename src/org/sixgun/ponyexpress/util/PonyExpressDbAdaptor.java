package org.sixgun.ponyexpress.util;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.Podcast;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.R;

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
    private static final String PODCAST_TABLE = "Podcasts";
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
    	
    private static final String TAG = "PonyExpressDbAdaptor";
    
    private PonyExpressDbHelper mDbHelper;
    private SQLiteDatabase mDb;
    public boolean mDatabaseUpgraded = false;
    public boolean mNewDatabase = false;
    
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
            mNewDatabase = true;
        }

		@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    		Log.w("PonyExpress", "Upgrading database from version " + oldVersion + " to "
                    + newVersion);
    		// Copy old data across to new table
    		switch (newVersion) {
//			case 12:
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
//				break;

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
        if (mNewDatabase) {
        	loadSixgunPodcasts();
        }
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }
    
    private String getTableName(String podcastName) {
		final String[] columns = {PodcastKeys.TABLE_NAME};
		final String quotedName = "\"" + podcastName + "\"";
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "= " + quotedName, null, null, null, null, null);
		String tablename = "";
		if (cursor != null){
			cursor.moveToFirst();
			tablename = cursor.getString(0);
			Log.d(TAG, "Tablename of Episode is: "+ tablename);
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
		final String[] columns = {EpisodeKeys._ID, EpisodeKeys.TITLE, EpisodeKeys.DATE};
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
		if (podcasts_cursor != null){
			podcasts_cursor.moveToFirst();
			for (int i = 0; i < podcasts_cursor.getCount(); i++){
				final String name = podcasts_cursor.getString(1);
				podcast_names.add(name);
				podcasts_cursor.moveToNext();
			}
		}
		podcasts_cursor.close();
		return podcast_names;
	}
	/**
	 * Gets all filenames of all the files that have been downloaded and should be 
	 * on the SD Card.
	 */
	public Map<Long, String> getFilenamesOnDisk(String podcast_name){
		//Get all episodes from correct table that have been downloaded (and are still on disk)
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID, EpisodeKeys.DOWNLOADED, 
				EpisodeKeys.FILENAME};
		Map<Long, String> files = new HashMap<Long, String>();
		final Cursor cursor = mDb.query(true, table_name, columns, 
				EpisodeKeys.DOWNLOADED + "!= 0", null, null, null, null, null);

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
	 * @param podcast_name
	 * @return The Latest episodes date as a string.
	 */
	public Date getLatestEpisodeDate(String podcast_name){
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys.DATE};
		final Cursor cursor = mDb.query(table_name, 
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
	
	public String getEpisodeUrl(long row_ID, String podcast_name){
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.URL};
		final Cursor cursor = mDb.query(true, table_name,
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
	public boolean update(String podcast_name, long rowID, String key, int newRecord){
		final String table_name = getTableName(podcast_name);
		ContentValues values = new ContentValues();
		if (key == EpisodeKeys.LISTENED){
			values.put(EpisodeKeys.LISTENED, newRecord);
		}
		return mDb.update(table_name, values, EpisodeKeys._ID + "=" + rowID, null) > 0;
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
	
	public String getEpisodeTitle(long row_ID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.TITLE};
		final Cursor cursor = mDb.query(true, table_name,
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

	public boolean isEpisodeDownloaded(long row_ID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DOWNLOADED};
		final Cursor cursor = mDb.query(true, table_name,
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

	public String getDescription(long row_ID, String podcast_name) {
		final String table_name = getTableName(podcast_name);
		final String[] columns = {EpisodeKeys._ID,EpisodeKeys.DESCRIPTION};
		final Cursor cursor = mDb.query(true, table_name,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String description = "";
		if (cursor != null){
			cursor.moveToFirst();
			description = cursor.getString(1);
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
		if (cursor != null){
			cursor.moveToFirst();
			listened = cursor.getInt(1);
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
		if (cursor != null){
			cursor.moveToFirst();
			row_ID = cursor.getLong(0);
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
		if (cursor != null){
			rows = cursor.getCount();
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
		if (cursor != null){
			cursor.moveToFirst();
			size = cursor.getInt(1);
		}
		cursor.close();
		return size;
	}
	
	/**
	 * Loads the Sixgun productions podcast titles and urls into the database
	 * when first created.
	 */
	private void loadSixgunPodcasts() {
		String[] feed_urls =  mCtx.getResources().getStringArray(R.array.Sixgun_Podcasts);
		String[] identica_tags = mCtx.getResources().getStringArray(R.array.Sixgun_Podcast_Tags); 
		String[] identica_groups = mCtx.getResources().getStringArray(R.array.Sixgun_Podcast_Groups);
		final int feeds = feed_urls.length;
		final int tags = identica_tags.length;
		final int groups = identica_groups.length;
		if (feeds != tags || groups != feeds){
			throw new RuntimeException("Number of Sixgun Podcast feed does not equal the number of tags/groups.");			
		}
		for (int i = 0; i < feeds; ++i) {
			PodcastFeedParser parser = new PodcastFeedParser(feed_urls[i]);
			Podcast podcast = parser.parse();
			podcast.setIdenticaTag(identica_tags[i]);
			podcast.setIdenticaGroup(identica_groups[i]);
			insertPodcast(podcast);
			//Create table for this podcast's episodes
			String tableName = getTableName(podcast.getName());
			mDb.execSQL("CREATE TABLE " + tableName + EPISODE_TABLE_FIELDS);	
		}
	}
	
	/**
    * Adds each podcast to the database.
    * @param podcast
    * @return The row ID of the inserted row or -1 if an error occurred. 
    */
	private boolean insertPodcast(Podcast podcast) {
		//FIXME Check if episode is already in the database first?.
        ContentValues podcastValues = new ContentValues();
        String name = podcast.getName();
        podcastValues.put(PodcastKeys.NAME, name);
        podcastValues.put(PodcastKeys.FEED_URL, podcast.getFeed_Url().toString());
        podcastValues.put(PodcastKeys.ALBUM_ART_URL, podcast.getArt_Url().toString());
        podcastValues.put(PodcastKeys.TAG, podcast.getIdenticaTag());
        podcastValues.put(PodcastKeys.GROUP, podcast.getIdenticaGroup());
        podcastValues.putNull(PodcastKeys.TABLE_NAME);
     
        //Insert the record and then update it with the created Episode table name
		Long row_ID = mDb.insert(PODCAST_TABLE, null , podcastValues);
		String tablename = "PodEps";
		if (row_ID != -1){
			tablename = tablename + row_ID;
			podcastValues.put(PodcastKeys.TABLE_NAME,tablename);
			final String quotedName = "\"" + name + "\"";
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
		if (cursor != null){
			cursor.moveToFirst();
			name = cursor.getString(1);
			Log.d(TAG, "Title of Podcast is: "+ name);
		}
		cursor.close();
		return name;
	}

	public String getPodcastUrl(String podcast_name) {
		final String quotedName = "\"" + podcast_name + "\"";
		final String[] columns = {PodcastKeys._ID,PodcastKeys.FEED_URL};
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "=" + quotedName ,
				null, null, null, null, null);
		String url = "";
		if (cursor != null){
			cursor.moveToFirst();
			url = cursor.getString(1);
		}
		cursor.close();
		return url;
	}
	
	public String getAlbumArtUrl(long row_ID){
		final String[] columns = {PodcastKeys._ID,PodcastKeys.ALBUM_ART_URL};
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, EpisodeKeys._ID + "=" + row_ID, null, null, null, null, null);
		String url = "";
		if (cursor != null){
			cursor.moveToFirst();
			url = cursor.getString(1);
		}
		cursor.close();
		return url;
	}

	public String getIdenticaTag(String podcast_name) {
		final String quotedName = "\"" + podcast_name + "\"";
		final String[] columns = {PodcastKeys._ID,PodcastKeys.TAG};
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "=" + quotedName ,
				null, null, null, null, null);
		String url = "";
		if (cursor != null){
			cursor.moveToFirst();
			url = cursor.getString(1);
		}
		cursor.close();
		return url;
	}

	public String getIdenticaGroup(String podcast_name) {
		final String quotedName = "\"" + podcast_name + "\"";
		final String[] columns = {PodcastKeys._ID,PodcastKeys.GROUP};
		final Cursor cursor = mDb.query(true, PODCAST_TABLE,
				columns, PodcastKeys.NAME + "=" + quotedName ,
				null, null, null, null, null);
		String url = "";
		if (cursor != null){
			cursor.moveToFirst();
			url = cursor.getString(1);
		}
		cursor.close();
		return url;
	}
}
