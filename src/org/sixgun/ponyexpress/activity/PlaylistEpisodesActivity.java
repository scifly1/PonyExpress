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
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PlaylistInterface;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.service.DownloaderService;
import org.sixgun.ponyexpress.util.InternetHelper;
import org.sixgun.ponyexpress.util.Utils;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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


public class PlaylistEpisodesActivity extends EpisodesActivity implements PlaylistInterface{

	private static final int START_PLAYBACK = 0;
	private static final int NOT_DOWNLOADED_DIALOG = 0;
	private static final String TAG = "PlaylistEpisodesActivity";
	private ListView mPlaylist;
	private CheckBox mAlwaysDownloadCheckbox;
	private long mRowIdForNotDownloadedDialog;
	private TextView mNoPlaylist;

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playlist_episodes);
		
		//Get the playlist listview as we need to manage it.
		mPlaylist = (ListView) findViewById(R.id.playlist_list);
		mNoPlaylist = (TextView) findViewById(R.id.no_list);
				
		listPlaylist();
				
		registerForContextMenu(getListView());
		
		((TextView) findViewById(R.id.playlist_subtitle)).setText(R.string.playlist_subtitle_eps);
		
		//Set the background of the episode list
		mBackground = (ViewGroup) findViewById(R.id.episode_list);
		mBackground.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				Resources res = getResources();
				Bitmap image = PonyExpressApp.sImageManager.get(mAlbumArtUrl);
				if (image != null){
					int new_height = mBackground.getHeight();
					int new_width = mBackground.getWidth();
					BitmapDrawable new_background = Utils.createBackgroundFromAlbumArt
							(res, image, new_height, new_width);
					mBackground.setBackgroundDrawable(new_background);
				}
			}
		});
		
	}
	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.PlaylistInterface#goBack(android.view.View)
	 */
	@Override
	public void goBack(View v) {
		finish();
	}
	
	/* (non-Javadoc)
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
		
		
	}
	
	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//TODO Add more entries.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.playlist_options_menu, menu);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onPrepareOptionsMenu(android.view.Menu)
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
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onOptionsItemSelected(android.view.MenuItem)
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
		default: 
			return super.onOptionsItemSelected(item);
		}
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.playlist_list){
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.playlist_context, menu);
			
			//Set the title of the menu
			AdapterView.AdapterContextMenuInfo item = (AdapterContextMenuInfo) menuInfo;
			TextView episode_name = (TextView) item.targetView.findViewById(R.id.episode_text);
			menu.setHeaderTitle(episode_name.getText());
			//TODO Correct the episode_list context menu.
		}else{
			super.onCreateContextMenu(menu, v, menuInfo);
		}		
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()){
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
		default:
			return super.onContextItemSelected(item);
		}
	}

	/* (non-Javadoc)
	 * @see org.sixgun.ponyexpress.activity.EpisodesActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		//This detects clicks on the ListActivity list which is the episode list.
		//is episode downloaded?
		if (!mPonyExpressApp.getDbHelper().isEpisodeDownloaded(id, mPodcastName)){
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
				Log.e(TAG, "Unkown return from InternetHelper.isDownloadPossible");
				return;
			}
		} else {
			mPonyExpressApp.getDbHelper().addEpisodeToPlaylist(mPodcastName, id);			
			listPlaylist();
		}
		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = prefs.edit();
		//If more dialogs are added this should be a switch case block.
		if (id == NOT_DOWNLOADED_DIALOG){
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
			dialog = builder.create();
		} else dialog = null;
		return dialog;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle episode) {
		super.onPrepareDialog(id, dialog, episode);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean auto_download = prefs.getBoolean(getString(R.string.auto_download_key), false);
		mAlwaysDownloadCheckbox.setChecked(auto_download);
		mRowIdForNotDownloadedDialog = episode.getLong(EpisodeKeys.ROW_ID);
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
			String episode_title = "";
			int listened = -1;
			if (cursor != null){
				final int row_id_column_index = cursor.getColumnIndex(EpisodeKeys.ROW_ID);
				final int podcast_name_column_index = cursor.getColumnIndex(PodcastKeys.NAME);
				final long row_id = cursor.getLong(row_id_column_index);
				final String podcast_name = cursor.getString(podcast_name_column_index);
				episode_title = mPonyExpressApp.getDbHelper().
						getEpisodeTitle(row_id, podcast_name);
				listened = mPonyExpressApp.getDbHelper().getListened(row_id, podcast_name);
			}
			
			TextView episodeName = (TextView) view.findViewById(R.id.episode_text);
			
			episodeName.setText(episode_title);
			if (listened == -1){ //not listened == -1
				episodeName.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
			} else episodeName.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);

			
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//TODO handle clicks on the playlist item
					
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
			v = vi.inflate(R.layout.episode_row_playlist, parent, false);
			return v;
		}
		
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
	
	private void startDownload(long id){
		Intent intent = new Intent(this,DownloaderService.class);
		Bundle bundle = Episode.packageEpisode(mPonyExpressApp, mPodcastName, id);
		intent.putExtras(bundle);
		intent.putExtra("action", DownloaderService.DOWNLOAD);
		startService(intent);
	}
	
}
