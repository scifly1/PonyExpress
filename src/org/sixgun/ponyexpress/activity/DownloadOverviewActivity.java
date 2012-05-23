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

import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.service.DownloaderService;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

public class DownloadOverviewActivity extends ListActivity {

	protected static final String TAG = "DownloadOverviewActivity";
	private DownloaderService mDownloader;
	private boolean mDownloaderBound;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_overview);
		
		
	}
		
	/* (non-Javadoc)
	 * @see android.app.ListActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindDownloaderService();
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
					//TODO Get downloading episode data
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
