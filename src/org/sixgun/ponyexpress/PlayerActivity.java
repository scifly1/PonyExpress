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

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

/**
 *
 */
public class PlayerActivity extends EpisodeActivity {

	private CharSequence mTitleText;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle data = getIntent().getExtras();
		setContentView(R.layout.player);

		TextView title = (TextView) findViewById(R.id.TitleText);
		mTitleText = data.getString(EpisodeKeys.TITLE);
		title.setText(mTitleText);
		
		WebView description = (WebView) findViewById(R.id.Description);
		String descriptionText = data.getString(EpisodeKeys.DESCRIPTION);
		description.loadData(descriptionText, "text/html", "UTF-8");
		
		OnClickListener mPlayButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				//TODO Play episdode
			}
		};
		Button download_button = (Button)findViewById(R.id.PlayButton);
		download_button.setOnClickListener(mPlayButtonListener);
		
	}
	
}
