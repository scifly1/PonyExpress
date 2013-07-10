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
package org.sixgun.ponyexpress.activity;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeCursorAdapter;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PlaylistInterface;
import org.sixgun.ponyexpress.PodcastCursorAdapter;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.service.DownloaderService;
import org.sixgun.ponyexpress.util.InternetHelper;
import org.sixgun.ponyexpress.util.PonyLogger;
import org.sixgun.ponyexpress.util.Bitmap.RecyclingImageView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class PlaylistActivity extends Activity implements PlaylistInterface {

	private static final int START_PLAYBACK = 0;
	private static final int NOT_DOWNLOADED_DIALOG = 0;
	private static final String TAG = "PlaylistActivity";
	private static final int ABOUT_DIALOG = 1;
	private ListView mPlaylist;
	private View mNoPlaylist;
	private PonyExpressApp mPonyExpressApp;
	private ViewGroup mBackground;
	private String mAlbumArtUrl;
	private String mPodcastName;
	private CheckBox mAlwaysDownloadCheckbox;
	protected long mRowIdForNotDownloadedDialog;
	private boolean mListingEpisodes = false;
	private ListView mPodcastsAndEpisodesList;
	private TextView mPlaylistSubtitle;
	private boolean mAutoPlaylistsOn = false;
	private View mPodcastEpisodeLists;
	private View mDivider;
	private View mDownloadButton;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playlist);
		
		mPonyExpressApp = (PonyExpressApp) getApplication();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mPonyExpressApp);
		if (prefs.getBoolean(getString(R.string.auto_playlist_key), false)){
			mAutoPlaylistsOn  = true;
		}
		//Get the listviews as we need to manage them.
		mPlaylist = (ListView) findViewById(R.id.playlist_list);
		mNoPlaylist = (TextView) findViewById(R.id.no_list);
		
		mPodcastsAndEpisodesList = (ListView) findViewById(R.id.podcasts_episodes_list);
		mPlaylistSubtitle = (TextView) findViewById(R.id.playlist_subtitle);
		mPodcastEpisodeLists = findViewById(R.id.podcast_episode_lists);
		mDivider = findViewById(R.id.divider);
		mDownloadButton = findViewById(R.id.download_overview_button);
		
		if (!mAutoPlaylistsOn){
			//Set the background of the episode list when shown
			mBackground = (ViewGroup) findViewById(R.id.playlist_episodes_body);
			mBackground.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					if (mListingEpisodes && !mAlbumArtUrl.equals(null)){
						Resources res = getResources();
						int new_height = mBackground.getHeight();
						int new_width = mBackground.getWidth();
						BitmapDrawable new_background = PonyExpressApp.
								sBitmapManager.createBackgroundFromAlbumArt(res, mAlbumArtUrl, new_height, new_width);
						mBackground.setBackgroundDrawable(new_background);
					}
				}
			});
			
			listPodcasts(false);
		} else {
			mPodcastEpisodeLists.setVisibility(View.GONE);
			mDivider.setVisibility(View.GONE);
			mDownloadButton.setVisibility(View.GONE);
			mPlaylist.setStackFromBottom(false);
		}
		
		listPlaylist();
		
	}
		
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		//Re-list playlist to ensure it is updated if it was empty before 
		listPlaylist();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		//Handle pressing back when showing the episodes list. 
		goBack(null);
	}

	/*
	 * @see org.sixgun.ponyexpress.PlaylistInterface#goBack(android.view.View)
	 */
	@Override
	public void goBack(View v) {
		if (mListingEpisodes){
			mListingEpisodes = false;
			//Remove podcast art from background
			mAlbumArtUrl = null;
			mBackground.setBackgroundResource(R.drawable.background);
			
			listPodcasts(false);
		} else {
			finish();
		}
	}
		
	/*
	 * @see org.sixgun.ponyexpress.PlaylistInterface#listPlaylist()
	 */
	@Override
	public void listPlaylist() {
		Cursor c = mPonyExpressApp.getDbHelper().getPlaylist();
		if (c.getCount() > 0){
			mPlaylist.setVisibility(View.VISIBLE);
			mNoPlaylist.setVisibility(View.GONE);
			startManagingCursor(c);
			//Create a cursor adaptor to populate the ListView
			PlaylistCursorAdapter adapter = new PlaylistCursorAdapter(mPonyExpressApp, c);

			mPlaylist.setAdapter(adapter);

			registerForContextMenu(mPlaylist);
		} else {
			mPlaylist.setVisibility(View.GONE);
			mNoPlaylist.setVisibility(View.VISIBLE);
		}
		if (mAutoPlaylistsOn){
			mPlaylistSubtitle.setText(c.getCount() + " " +getString(R.string.auto_playlist_subtitle));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity#listPodcasts(boolean)
	 */
	protected void listPodcasts(boolean addFooter) {
		Cursor c = mPonyExpressApp.getDbHelper().getAllPodcastNamesAndArt();
		startManagingCursor(c);
		//Create a CursorAdapter to map podcast title and art to the ListView.
		PlaylistPodcastCursorAdapter adapter = new PlaylistPodcastCursorAdapter(mPonyExpressApp, c);
		
		mPodcastsAndEpisodesList.setAdapter(adapter);
		mPlaylistSubtitle.setText(R.string.playlist_subtitle_pod);
	}
	
	protected void listEpisodes(String podcast_name){
		//Method called when a podcast is selected. Switches 
		//the adapter used in the listView from the podcastAdapter 
		//to the episodes adapter.
		mListingEpisodes = true;
		
		Cursor c = mPonyExpressApp.getDbHelper().getAllEpisodeNamesDescriptionsAndLinks(podcast_name);
		startManagingCursor(c);		
		
		EpisodeCursorAdapter episodes = new PlaylistEpisodeCursorAdapter(this, c);
		mPodcastsAndEpisodesList.setAdapter(episodes);
		registerForContextMenu(mPodcastsAndEpisodesList);
		mPlaylistSubtitle.setText(R.string.playlist_subtitle_eps);
	}
	
	private void startDownload(long id) {
		Intent intent = new Intent(this,DownloaderService.class);
		Bundle bundle = Episode.packageEpisode(mPonyExpressApp, mPodcastName, id);
		intent.putExtras(bundle);
		intent.putExtra("action", DownloaderService.DOWNLOAD);
		startService(intent);
		//Start a new thread and sleep for a few seconds then re-list playlist to update it 
		//if a download error occurred. (DownloaderService will have removed 
		//the episode from the playlist).
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					PonyLogger.e(TAG, "Interupted sleep in startDownload", e);
				}
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						listPlaylist();
						
					}
				});
			}
			
		}).start();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = prefs.edit();
		switch (id){
		case NOT_DOWNLOADED_DIALOG:
			AlertDialog.Builder builder = new Builder(this);
			LayoutInflater inflater = this.getLayoutInflater();
			View message = inflater.inflate(R.layout.auto_download_dialog,(ViewGroup) findViewById(R.id.auto_download_dialog_root));
			mAlwaysDownloadCheckbox = (CheckBox) message.findViewById(R.id.always_download_checkBox);
			
			builder.setView(message);
			builder.setCancelable(false)
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			})
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (mAlwaysDownloadCheckbox.isChecked()){
						editor.putBoolean(getString(R.string.auto_download_key), true);
					} else {
						editor.putBoolean(getString(R.string.auto_download_key), false);
					}
					editor.commit();
					mPonyExpressApp.getDbHelper().addEpisodeToPlaylist(mPodcastName, mRowIdForNotDownloadedDialog);			
					listPlaylist();
					startDownload(mRowIdForNotDownloadedDialog);
					
				}
			});
			dialog = (AlertDialog) builder.create();
		break;
		case ABOUT_DIALOG:
			dialog = AboutDialog.create(this);
			break;
		default:	
			dialog = null;
			break;
		}
		return dialog;
		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle episode) {
		super.onPrepareDialog(id, dialog, episode);
		switch (id){
		case NOT_DOWNLOADED_DIALOG:
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			final boolean auto_download = prefs.getBoolean(getString(R.string.auto_download_key), false);
			mAlwaysDownloadCheckbox.setChecked(auto_download);
			mRowIdForNotDownloadedDialog = episode.getLong(EpisodeKeys.ROW_ID);
			break;
		case ABOUT_DIALOG:
			//Nothing to prepare
			break;
		default:
			break;
		}
		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//TODO Add more entries.
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.playlist_options_menu, menu);
	    return true;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (mPonyExpressApp.getDbHelper().playlistEmpty()){
			menu.removeItem(R.id.clear_playlist);
		}
		return true;
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_menu:
	    	startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
	        return true;
		case R.id.clear_playlist:
			mPonyExpressApp.getDbHelper().clearPlaylist();
			listPlaylist();
			return true;
		case R.id.downloads:
			startActivity(new Intent(
					mPonyExpressApp, DownloadOverviewActivity.class));
			return true;
		case R.id.about:
			showDialog(ABOUT_DIALOG);
		default: 
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		//Set the title of the menu
		AdapterView.AdapterContextMenuInfo item = (AdapterContextMenuInfo) menuInfo;
		TextView episode_name = (TextView) item.targetView.findViewById(R.id.episode_text);
		menu.setHeaderTitle(episode_name.getText());
		
		if (v.getId() == R.id.playlist_list && mAutoPlaylistsOn){
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.auto_playlist_context, menu);
			
		}else if (v.getId() == R.id.playlist_list){
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.playlist_context, menu);
			
		} else if (v.getId() == R.id.podcasts_episodes_list && mListingEpisodes){
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.episode_playlist_context, menu);
			//Find which listened option to show
			boolean listened = true;
			if (mPonyExpressApp.getDbHelper().getListened(item.id) == -1){
				listened = false;
			}
			if (listened){
				menu.removeItem(R.id.mark_listened);
			} else {
				menu.removeItem(R.id.mark_not_listened);
			}
			//Check if it is a youtube video
			if (item.targetView.getTag().equals(EpisodeCursorAdapter.YOUTUBE_EPISODE)){
				menu.removeItem(R.id.add_to_playlist);
			}
		} else {
			super.onCreateContextMenu(menu, v, menuInfo);
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()){
		//Playlist context menu items
		case R.id.move_top:
			mPonyExpressApp.getDbHelper().moveToTop(info.position);
			listPlaylist();
			return true;
		case R.id.move_bottom:
			mPonyExpressApp.getDbHelper().moveToBottom(info.position);
			listPlaylist();
			return true;
		case R.id.remove_from_playlist:
			mPonyExpressApp.getDbHelper().moveToTop(info.position);
			mPonyExpressApp.getDbHelper().popPlaylist();
			listPlaylist();
			return true;
		case R.id.move_up:
			mPonyExpressApp.getDbHelper().moveUpPlaylist(info.position);
			listPlaylist();
			return true;
		case R.id.move_down:
			mPonyExpressApp.getDbHelper().moveDownPlaylist(info.position);
			listPlaylist();
			return true;
		case R.id.shownotes_playlist:
			
			final String podcast_name = mPonyExpressApp.getDbHelper().getPodcastFromPlaylist(info.id);
			final long episode_id = mPonyExpressApp.getDbHelper().getEpisodeFromPlaylist(info.id);
			viewShowNotes(podcast_name,episode_id);
			
			return true;
		//Episode context menu items
		case R.id.add_to_playlist:
			selectEpisode(info.id);
			return true;
		case R.id.shownotes:
			viewShowNotes(mPodcastName, info.id);
			return true;
		case R.id.mark_listened:
			mPonyExpressApp.getDbHelper().update(info.id, 
					EpisodeKeys.LISTENED, 0);
			listEpisodes(mPodcastName);
			return true;
		case R.id.mark_not_listened:
			mPonyExpressApp.getDbHelper().update(info.id, 
					EpisodeKeys.LISTENED, -1);
			listEpisodes(mPodcastName);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	private void viewShowNotes(String podcast_name, long row_id){
		final Bundle playlist_episode_data = Episode.packageEpisode(mPonyExpressApp, podcast_name, row_id);
		Intent i = new Intent(this, ShowNotesActivity.class);
		i.putExtras(playlist_episode_data);
		startActivity(i);
	}
	
	/**
	 * We subclass CursorAdapter to handle our display the results from our Playlist cursor. 
	 * Overide newView to create/inflate a view to bind the data to.
	 * Overide bindView to determine how the data is bound to the view.
	 */
	private class PlaylistCursorAdapter extends CursorAdapter{

		public PlaylistCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			if (cursor == null){
				return;
			}
			String episode_title = "";
			int listened = -1;
			final int row_id_column_index = cursor.getColumnIndex(EpisodeKeys.ROW_ID);
			final long row_id = cursor.getLong(row_id_column_index);
			episode_title = mPonyExpressApp.getDbHelper().
					getEpisodeTitle(row_id);
			listened = mPonyExpressApp.getDbHelper().getListened(row_id);
			
			
			TextView episodeName = (TextView) view.findViewById(R.id.episode_text);
			episodeName.setText(episode_title);
			if (listened == -1){ //not listened == -1
				episodeName.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
			} else episodeName.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);

			final int podcast_name_index = cursor.getColumnIndex(PodcastKeys.NAME);
			final String podcast_name = cursor.getString(podcast_name_index);
			
			if (mAutoPlaylistsOn){
				String albumArtUrl = mPonyExpressApp.getDbHelper().getAlbumArtUrl(podcast_name);
				RecyclingImageView albumArt = (RecyclingImageView)view.findViewById(R.id.album_art);
				
				if (albumArtUrl!= null && !"".equals(albumArtUrl) && !"null".equalsIgnoreCase(albumArtUrl)){
					PonyExpressApp.sBitmapManager.loadImage(albumArtUrl, albumArt);
				}
			}
			
			view.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					viewShowNotes(podcast_name, row_id);
					
				}
			});
			
			view.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					openContextMenu(v);
					return true;
				}
			});
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			if (mAutoPlaylistsOn){
				v = vi.inflate(R.layout.episode_row_auto_playlist, parent, false);
			} else {
				v = vi.inflate(R.layout.episode_row_playlist, parent, false);
			}
			
			return v;
		}
		
	}
	
	private class PlaylistPodcastCursorAdapter extends PodcastCursorAdapter{

		public PlaylistPodcastCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			//Call super's method to get all layout sorted
			super.bindView(view, context, cursor);

			//Add Click listener's for each row.
			final int id_index = cursor.getColumnIndex(PodcastKeys._ID);
			final long id = cursor.getLong(id_index);
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					selectPodcast(id);

				}
			});
			
		}

		/* (non-Javadoc)
		 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity.PodcastCursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
		 */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = new View(context);
			v = vi.inflate(R.layout.playlist_podcast_row, parent, false);
			return v;
		}
	}
	
	private class PlaylistEpisodeCursorAdapter extends EpisodeCursorAdapter {

		public PlaylistEpisodeCursorAdapter(Context context, Cursor c) {
			super(context, c);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			//Call super method to set out layout
			super.bindView(view, context, cursor);
			
			//Add Click listener's for each row.
			final int id_index = cursor.getColumnIndex(EpisodeKeys._ID);
			final long id = cursor.getLong(id_index);
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//Check if a Youtube video or regular episode
					if (v.getTag().equals(EpisodeCursorAdapter.REGULAR_EPISODE)){
						selectEpisode(id);
					} else {
						Toast.makeText(mPonyExpressApp, R.string.youtube_to_playlist, Toast.LENGTH_SHORT).show();
					}
				}
			});
			
			view.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					openContextMenu(v);
					return true;
				}
			});
		}
		
		
	}
	

	/* 
	 * Starts the PlaylistEpisodesActivity to select the required episodes
	 * @param id row_id of the podcast in the database
	 */
	protected void selectPodcast(long id) {
		//Get the podcast name and album art url and number of unlistened episodes.
		mPodcastName = mPonyExpressApp.getDbHelper().getPodcastName(id);
		mAlbumArtUrl = mPonyExpressApp.getDbHelper().getAlbumArtUrl(id);
		
		//Change listView by changing adapter used with listEpisodes(podcast name, art url)
		listEpisodes(mPodcastName);
	}
	
	private void selectEpisode(long id) {
		//is episode downloaded?
		if (!mPonyExpressApp.getDbHelper().isEpisodeDownloaded(id)){
			// is Connectivity ok and are downloads allowed?
			switch (mPonyExpressApp.getInternetHelper().isDownloadPossible()){
			case InternetHelper.NO_CONNECTION:
				Toast.makeText(mPonyExpressApp, R.string.not_downloaded_no_internet, Toast.LENGTH_SHORT).show();
				return;
			case InternetHelper.MOBILE_NOT_ALLOWED:
				Toast.makeText(mPonyExpressApp, R.string.not_downloaded_wrong_network_type, Toast.LENGTH_SHORT).show();
				return;
			case InternetHelper.DOWNLOAD_OK:
				final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				final boolean auto_download = prefs.getBoolean(getString(R.string.auto_download_key), false);
				if (!auto_download){
					Bundle episode = new Bundle();
					episode.putLong(EpisodeKeys.ROW_ID, id);
					showDialog(NOT_DOWNLOADED_DIALOG, episode);
				} else {
					//Auto-download ok
					startDownload(id);
					mPonyExpressApp.getDbHelper().addEpisodeToPlaylist(mPodcastName, id);			
					listPlaylist();
				}
				break;
			default:
				PonyLogger.e(TAG, "Unknown return from InternetHelper.isDownloadPossible");
				return;
			}
		} else {
			mPonyExpressApp.getDbHelper().addEpisodeToPlaylist(mPodcastName, id);			
			listPlaylist();
		}
	
	}


	/*
	 * @see org.sixgun.ponyexpress.PlaylistInterface#startPlaylist(android.view.View)
	 */
	@Override
	public void startPlaylist(View v) {
		if (!mPonyExpressApp.getDbHelper().playlistEmpty()){
			//Start EpisodeTabs as happens from EpisodeActivity
			// but hand over a flag to indicate to play from the playlist.
			SharedPreferences prefs = getSharedPreferences(PodcastKeys.PLAYLIST, 0);
			final SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PodcastKeys.PLAYLIST, true);
			editor.commit();
			Intent intent = new Intent(this,EpisodeTabs.class);
			intent.putExtra(PodcastKeys.PLAYLIST, true);
			startActivityForResult(intent, START_PLAYBACK);
		}
	}
	
	/*
	 * @see org.sixgun.ponyexpress.PlaylistInterface#openDownloadOverview(android.view.View)
	 */
	@Override
	public void openDownloadOverview(View v){
		Intent intent = new Intent(this,DownloadOverviewActivity.class);
		startActivity(intent);
	}
	

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.PonyExpressActivity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == START_PLAYBACK){
			if (resultCode == RESULT_OK) {
				//Playback completed get next episode
				startPlaylist(null);
			}
		}
	}
	

}
