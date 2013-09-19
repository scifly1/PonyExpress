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
package org.sixgun.ponyexpress.activity;

import org.sixgun.ponyexpress.fragment.AddNewPodcastsFragment;
import org.sixgun.ponyexpress.util.PonyLogger;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;


public class AddNewPodcastFragActivity extends FragmentActivity {

	private static final String TAG = "AddNewPodcastFragActivity";
	



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null){
			// During initial setup, plug in the AddNewPodcasts fragment.
			AddNewPodcastsFragment fragment = new AddNewPodcastsFragment();
			fragment.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(android.R.id.content,fragment,"addNew").commit();
		}
		handleIntent(getIntent());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		PonyLogger.d(TAG, "New intent received");
		setIntent(intent);
		handleIntent(intent);
	}



	private void handleIntent(Intent intent){
		//if a search action, get the query and send on to fragment to deal with
		if (Intent.ACTION_SEARCH.equals(intent.getAction())){
			String query = intent.getStringExtra(SearchManager.QUERY);
			AddNewPodcastsFragment fragment = (AddNewPodcastsFragment) getSupportFragmentManager().findFragmentByTag("addNew");
			fragment.startSearch(query);
		}
	}

}
