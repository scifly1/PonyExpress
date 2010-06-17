/*
 * Copyright 2010 Paul Elms
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
package org.sixgun.ponyexpress;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 *
 */
public class DownloadActivity extends EpisodeActivity {

	private Button mDownloadButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.downloader);
		
		
		OnClickListener mDownloadButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(DownloadActivity.this,Downloader.class);
				i.putExtras(getIntent()); //pass though the Extras with the URL etc...
				startService(i);
				mDownloadButton.setEnabled(false);
				
			}
		};
		mDownloadButton = (Button)findViewById(R.id.DownloadButton);
		//Only make Download button available if we have internet connectivity.
		if (mPonyExpressApp.getInternetHelper().checkConnectivity()){
			mDownloadButton.setOnClickListener(mDownloadButtonListener);
		}else{
			mDownloadButton.setEnabled(false);
		}
		
		
		
	}
	
}
