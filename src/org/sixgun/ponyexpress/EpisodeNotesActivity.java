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

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * 
 *
 */
public class EpisodeNotesActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle data = getIntent().getExtras();
		setContentView(R.layout.notes);

		
		WebView description = (WebView) findViewById(R.id.Description);
		String descriptionText = data.getString(EpisodeKeys.DESCRIPTION);
		description.loadData(descriptionText, "text/html", "UTF-8");
	}
}
