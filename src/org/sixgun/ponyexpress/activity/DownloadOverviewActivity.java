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

import java.util.ArrayList;
import java.util.List;

import org.sixgun.ponyexpress.DownloadingEpisode;
import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.service.DownloaderService;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadOverviewActivity extends ListActivity {

	protected static final String TAG = "DownloadOverviewActivity";
	private DownloaderService mDownloader;
	private boolean mDownloaderBound;
	private volatile boolean mInterruptProgressThread = false;
	private DownloadingEpisodeAdapter mAdapter;
	private List<DownloadingEpisode> mDownloadsArrayList;
	private Handler mHandler;
	private List<DownloadingEpisode> mCancelledDownloads;
	private PonyExpressApp mPonyExpressApp;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_overview);
		mDownloadsArrayList = new ArrayList<DownloadingEpisode>();
		mCancelledDownloads = new ArrayList<DownloadingEpisode>();
		mPonyExpressApp = (PonyExpressApp) getApplication();
		mAdapter = new DownloadingEpisodeAdapter(this,R.layout.downloads_row, mDownloadsArrayList);
		setListAdapter(mAdapter);
		mHandler = new Handler();
		
		
		
	}
		
	/* (non-Javadoc)
	 * @see android.app.ListActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindDownloaderService();
		mInterruptProgressThread = true;
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		doBindDownloaderService();
	}


	public void goBack(View v){
		finish();
	}
	
	Runnable UpdateDataRunnable = new Runnable() {
		@Override
		public void run() {
			mAdapter.clear();
			for (DownloadingEpisode episode: mDownloadsArrayList){
				//Add episodes to the Adapter unless they have been cancelled
				if (!mCancelledDownloads.contains(episode)){
					mAdapter.add(episode);
				}
			}
			mAdapter.notifyDataSetChanged();
		}
		
	};
	
	private void startDownloadProgressThread(){
		new Thread(new Runnable(){
			@Override
			public void run() {
				while (!mInterruptProgressThread){
					mDownloadsArrayList = mDownloader.getDownloadingEpisodes();
					//Update the adapter
					mHandler.post(UpdateDataRunnable);
										
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.e(TAG, "Download Progress thread interuppted", e);
					}
				}
				Log.d(TAG, "Stopping download progress thread");
			}
		}).start();
	}

	
	protected class DownloadingEpisodeAdapter extends ArrayAdapter<DownloadingEpisode>{

		public DownloadingEpisodeAdapter(Context context,
				int textViewResourceId, List<DownloadingEpisode> objects) {
			super(context, textViewResourceId, objects);
		}

		/* (non-Javadoc)
		 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			final int itemPosition = position;
			final DownloadingEpisode episode = getItem(itemPosition);
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            v = vi.inflate(R.layout.downloads_row, null);
			}
			
			TextView episodeTitle = (TextView) v.findViewById(R.id.episode_name);
			episodeTitle.setText(episode.getTitle());
			
			ProgressBar progress = (ProgressBar) v.findViewById(R.id.progress_bar);
			//Check for Oversize episodes
			if (episode.getDownloadPercent() == DownloadingEpisode.OVERSIZE_EPISODE){
				progress.setIndeterminate(true);
			}else{
				progress.setIndeterminate(false);
				progress.setProgress((int) episode.getDownloadPercent());
			}
			
			TextView queueText = (TextView) v.findViewById(R.id.queue_text);
			if (episode.getDownloadProgress() == 0){
				//Epsiode is queued
				queueText.setVisibility(View.VISIBLE);
				progress.setVisibility(View.GONE);
			} else {
				queueText.setVisibility(View.GONE);
				progress.setVisibility(View.VISIBLE);
			}
			
			Button cancelButton = (Button) v.findViewById(R.id.cancel_button);
			cancelButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mDownloader.cancelDownload(
							episode.getTitle());
					mCancelledDownloads.add(episode);
					//remove from playlist if present
					mPonyExpressApp.getDbHelper().removeEpisodeFromPlaylist(episode.getPodcastName(), episode.getTitle());
				}
			});
			
			return v;
		}
		
	}
	
	//This is all responsible for connecting/disconnecting to the Downloader service.
		private ServiceConnection mDownloaderConnection = new ServiceConnection() {
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				// This is called when the connection with the service has been
		        // unexpectedly disconnected -- that is, its process crashed.
		        // Because it is running in our same process, we should never
		        // see this happen.
		        mDownloader = null;
				
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				// This is called when the connection with the service has been
		        // established, giving us the service object we can use to
		        // interact with the service.  Because we have bound to an explicit
		        // service that we know is running in our own process, we can
		        // cast its IBinder to a concrete class and directly access it.
				mDownloader = ((DownloaderService.DownloaderServiceBinder)service).getService();
				//Query Downloader for current downloads
				if (mDownloader.isDownloading()){
					Log.d(TAG, "Currently Downloading");
					startDownloadProgressThread();
				} else {
					Log.d(TAG, "Not Downloading");
				}
			}
		};
		
		protected void doBindDownloaderService() {
		    // Establish a connection with the service.  We use an explicit
		    // class name because we want a specific service implementation that
		    // we know will be running in our own process (and thus won't be
		    // supporting component replacement by other applications).
			
			//getApplicationContext().bindService() called instead of bindService(), as
			//bindService() does not work when called from the child Activity of an ActivityGroup
			//ie:TabActivity
		    bindService(new Intent(this, 
		            DownloaderService.class), mDownloaderConnection, Context.BIND_AUTO_CREATE);
		    mDownloaderBound = true;
		}


		protected void doUnbindDownloaderService() {
		    if (mDownloaderBound) {
		        // Detach our existing connection.
		    	//Must use getApplicationContext.unbindService() as 
		    	//getApplicationContext().bindService was used to bind initially.
		        unbindService(mDownloaderConnection);
		        mDownloaderBound = false;
		    }
		}
		
		
}
