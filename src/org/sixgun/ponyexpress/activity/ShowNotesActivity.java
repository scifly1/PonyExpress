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

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;


public class ShowNotesActivity extends Activity {

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle data = getIntent().getExtras();
		setContentView(R.layout.standalone_notes);
		
		WebView description = (WebView) findViewById(R.id.Description);
		TextView episode_title = (TextView) findViewById(R.id.episode_title);
		
		String descriptionText = data.getString(EpisodeKeys.DESCRIPTION);
		String episode_name = data.getString(EpisodeKeys.TITLE);
		//We use loadDataWithBaseURL here with no URL so that it expects URL encoding.
		//loadData does not handle the encoding correctly..
		description.loadDataWithBaseURL(null, descriptionText, "text/html", "UTF-8", null);
		episode_title.setText(episode_name);
	}
	
	
}
