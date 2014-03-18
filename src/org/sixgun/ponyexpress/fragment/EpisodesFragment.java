/*
 * Copyright 2013 Paul Elms
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
package org.sixgun.ponyexpress.fragment;

import java.util.List;

import org.sixgun.ponyexpress.Episode;
import org.sixgun.ponyexpress.EpisodeCursorAdapter;
import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.PodcastKeys;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.EpisodeTabsFragActivity;
import org.sixgun.ponyexpress.activity.ShowNotesActivity;
import org.sixgun.ponyexpress.service.DownloaderService;
import org.sixgun.ponyexpress.util.Utils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class EpisodesFragment extends ListFragment {

	private static final int MARK_ALL_LISTENED = 0;
	private static final int MARK_ALL_NOT_LISTENED = MARK_ALL_LISTENED +1;
	private static final int DOWNLOAD_ALL = MARK_ALL_NOT_LISTENED + 1;

	public static EpisodesFragment newInstance(String podcast_name, String art_url) {
        EpisodesFragment epis = new EpisodesFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putString(PodcastKeys.NAME, podcast_name);
        args.putString(PodcastKeys.ALBUM_ART_URL, art_url);
        epis.setArguments(args);

        return epis;
    }

	private PonyExpressApp mPonyExpressApp;
	private int mNumberUnlistened;
	private String mPodcastName;
	private TextView mUnlistenedText;
	private String mAlbumArtUrl;
	private ViewGroup mBackground;

	public String getShownPodcast() {
		return getArguments().getString(PodcastKeys.NAME, "");
	}

	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		//Important to always return a view here, unlike what the docs say.
		//If not then other methods called later eg:onResume() may try to use 
		// the listview which won't have been created. The fragment will still
		//be recreated after oriention change even when not needed.
		View v = inflater.inflate(R.layout.episodes, container, false);
		
		TextView title = (TextView) v.findViewById(R.id.title);
		mUnlistenedText = (TextView)v.findViewById(R.id.unlistened_eps);
		
		//Set title.
		title.setText(mPodcastName);
		//Set the background
				mBackground = (ViewGroup) v.findViewById(R.id.episodes_body);
				mBackground.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					
					@Override
					public void onGlobalLayout() {
						if (!mAlbumArtUrl.equals(null) && isAdded()){
							Resources res = getResources();
							int new_height = mBackground.getHeight();
							int new_width = mBackground.getWidth();
							BitmapDrawable new_background = PonyExpressApp.
									sBitmapManager.createBackgroundFromAlbumArt
									(res, mAlbumArtUrl, new_height, new_width);
							mBackground.setBackgroundDrawable(new_background);
						}
					}
				});
		return v;
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Get Podcast name and album art url from bundle or saved instance.
		Bundle data;
		if (savedInstanceState != null){
			data = savedInstanceState;
		} else {
			data = getArguments();
		}
		mPodcastName = data.getString(PodcastKeys.NAME);
		mAlbumArtUrl = data.getString(PodcastKeys.ALBUM_ART_URL);

		mPonyExpressApp = (PonyExpressApp) getActivity().getApplication();
		setHasOptionsMenu(true);
	}
	

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PodcastKeys.NAME, mPodcastName);
		outState.putString(PodcastKeys.ALBUM_ART_URL, mAlbumArtUrl);
	}

	/** 
	 * (Re-)list the episodes.
	 */
	@Override
	public void onResume() {
		super.onResume();
		listEpisodes();
	}

	/**
	 * Query the database for all Episode titles to populate the ListView.
	 */
	private void listEpisodes(){
		Cursor c = mPonyExpressApp.getDbHelper().getAllEpisodeNamesDescriptionsAndLinks(mPodcastName);
		getActivity().startManagingCursor(c);		
		
		EpisodeCursorAdapter episodes = new EpisodeCursorAdapter(getActivity(), c);
		setListAdapter(episodes);
		//enable the context menu
		registerForContextMenu(getListView());
		//Also update the unlistened text at the same time.
		mNumberUnlistened = mPonyExpressApp.getDbHelper().countUnlistened(mPodcastName);
		final String unListenedString = Utils.formUnlistenedString(mPonyExpressApp, mNumberUnlistened);		
		mUnlistenedText.setText(unListenedString);
		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.episode_context, menu);
		
		//Set the title of the menu
		AdapterView.AdapterContextMenuInfo item = (AdapterContextMenuInfo) menuInfo;
		TextView episodeText = (TextView) item.targetView.findViewById(R.id.episode_text);
		menu.setHeaderTitle(episodeText.getText());
		//Gather data to determine which menu items to show
		boolean listened = true;
		if (mPonyExpressApp.getDbHelper().getListened(item.id) == -1){
			listened = false;
		}
		boolean downloaded = false;
		if (mPonyExpressApp.getDbHelper().isEpisodeDownloaded(item.id)){
			downloaded = true;
		}
		//Hide unneeded items
		if (listened){
			menu.removeItem(R.id.mark_listened);
		} else {
			menu.removeItem(R.id.mark_not_listened);
		}
		if (downloaded){
			menu.removeItem(R.id.download);
		} else {
			menu.removeItem(R.id.re_download);
			menu.removeItem(R.id.listen);
			menu.removeItem(R.id.delete);
		}
		if (item.targetView.getTag().equals(EpisodeCursorAdapter.YOUTUBE_EPISODE)){
			menu.removeItem(R.id.download);
			menu.add(Menu.NONE, R.id.view, Menu.NONE, R.string.view);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()){
		case R.id.shownotes:
			//TODO Use show notes fragment
			final Bundle data = Episode.packageEpisode(mPonyExpressApp, mPodcastName, info.id);
			Intent intent = new Intent(getActivity(), ShowNotesActivity.class);
			intent.putExtras(data);
			startActivity(intent);
			return true;
		case R.id.mark_listened:
			markListened(info.id);
			listEpisodes();
			return true;
		case R.id.mark_not_listened:
			markNotListened(info.id);
			listEpisodes();
			return true;
		case R.id.view:
			//Fallthrough: onListItemClick will handle it
		case R.id.download:
			//Fallthrough: onListItemClick determines if can listen or download
		case R.id.listen:
			onListItemClick(getListView(), info.targetView, info.position, info.id);
			return true;
		case R.id.re_download:
			//Check user wants to download on the current network type
			if (!mPonyExpressApp.getInternetHelper().isDownloadAllowed()){
				Toast.makeText(mPonyExpressApp, R.string.wrong_network_type, Toast.LENGTH_SHORT).show();
			} else {
				mPonyExpressApp.getDbHelper().update(info.id, EpisodeKeys.DOWNLOADED, "false");
				startDownload(info.id);
			}
			return true;
		case R.id.delete:
			markListened(info.id);
			if (Utils.deleteFile(mPonyExpressApp, info.id, mPodcastName)){
				mPonyExpressApp.getDbHelper().update(info.id, EpisodeKeys.DOWNLOADED, "false");
				//Remove from playlist if in it
				mPonyExpressApp.getDbHelper().removeEpisodeFromPlaylist(mPodcastName, info.id);
			}
			listEpisodes();
		default:
			return super.onContextItemSelected(item);
		}	
	}
	

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Bundle episode = Episode.packageEpisode(mPonyExpressApp, mPodcastName, id);
		final String url = episode.getString(EpisodeKeys.URL);
		if (url != null && url.contains(Episode.YOUTUBE_URL)){
			//Launch youtube app if available.
			Uri uri = Uri.parse(url);
			uri = Uri.parse("vnd.youtube:" + uri.getQueryParameter("v"));
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			//Check intent can be resolved to youtube app
			List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(intent, 
					PackageManager.MATCH_DEFAULT_ONLY);
			if (list.size() > 0){
				startActivity(intent);
				markListened(id);
			} else {
				//Tell user youtube app is not installed
				Toast.makeText(mPonyExpressApp, R.string.no_youtube, Toast.LENGTH_LONG).show();
			}

		} else {
			Intent intent = new Intent(getActivity(),EpisodeTabsFragActivity.class);
			intent.putExtras(episode);
			startActivity(intent);
		}
	}



	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(Menu.NONE,MARK_ALL_LISTENED,Menu.NONE,R.string.mark_all_listened).
		setIcon(R.drawable.ic_menu_mark);
		menu.add(Menu.NONE,MARK_ALL_NOT_LISTENED,Menu.NONE,R.string.mark_all_not_listened).
		setIcon(R.drawable.ic_menu_revert);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		//Check that downloads are permitted under current network and that there
		//are episodes to download, 
		//If not remove the Download All menu item.
		final SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(mPonyExpressApp);
		final boolean onlyOnWiFi = 
				prefs.getBoolean(getString(R.string.wifi_only_key), true);
		Cursor c = mPonyExpressApp.getDbHelper().getAllUndownloadedAndUnlistened(mPodcastName);
		final int episodes_to_download = c.getCount();
		c.close();
		if ((onlyOnWiFi && mPonyExpressApp.getInternetHelper().getConnectivityType() 
				== ConnectivityManager.TYPE_MOBILE) || episodes_to_download == 0 ) {
			//Hide download all menu item	
			menu.removeItem(DOWNLOAD_ALL);
		} else if (menu.findItem(DOWNLOAD_ALL) == null){
			menu.add(Menu.NONE,DOWNLOAD_ALL, Menu.FIRST, R.string.download_all).setIcon(R.drawable.ic_menu_download);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
		case DOWNLOAD_ALL:
			//query database for undownloaded and unlistened shows
			Cursor c = mPonyExpressApp.getDbHelper()
			.getAllUndownloadedAndUnlistened(mPodcastName);
			if (c != null && c.getCount() > 0){
				c.moveToFirst();
				//for each, create intent for downloader service
				for (int i = 0; i < c.getCount(); i++){
					Intent intent = new Intent(getActivity(),DownloaderService.class);
					Bundle bundle = Episode.packageEpisode(mPonyExpressApp, mPodcastName, c.getLong(0));
					intent.putExtras(bundle);
					intent.putExtra("action", DownloaderService.DOWNLOAD);
					getActivity().startService(intent);
					
					c.moveToNext();
				}	
			}
			c.close();			
			return true;
		case MARK_ALL_LISTENED:
			markAllListened();
			listEpisodes();
			return true;
		case MARK_ALL_NOT_LISTENED:
			markAllNotListened();
			listEpisodes();
			return true;
		default:	
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void markAllListened() {
		Cursor c = mPonyExpressApp.getDbHelper().getAllNotListened(mPodcastName);
		int columnIndex = c.getColumnIndex(EpisodeKeys._ID);
		getActivity().startManagingCursor(c);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++){
			long rowID = c.getLong(columnIndex);
			markListened(rowID);
			c.moveToNext();
		}
	}
	private void markListened(Long rowID) {
		//Sets listened to 0 ie: the start of the episode
		mPonyExpressApp.getDbHelper().update(rowID, 
				EpisodeKeys.LISTENED, 0);
	}
	private void markNotListened(long rowID) {
		//Sets listened to -1, which flags as not listened
		mPonyExpressApp.getDbHelper().update(rowID, 
				EpisodeKeys.LISTENED, -1);
	}
	
	private void markAllNotListened() {
		Cursor c = mPonyExpressApp.getDbHelper().getAllListened(mPodcastName);
		int columnIndex = c.getColumnIndex(EpisodeKeys._ID);
		getActivity().startManagingCursor(c);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++){
			long rowID = c.getLong(columnIndex);
			markNotListened(rowID);
			c.moveToNext();
		}
	}
	
	private void startDownload(long id){
		Intent intent = new Intent(getActivity(),DownloaderService.class);
		intent.putExtras(Episode.packageEpisode(mPonyExpressApp, mPodcastName, id));
		intent.putExtra("action", DownloaderService.DOWNLOAD);
		getActivity().startService(intent);
	}
}
