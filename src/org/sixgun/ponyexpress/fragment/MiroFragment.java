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

import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.activity.PonyExpressFragsActivity;
import org.sixgun.ponyexpress.activity.PreferencesActivity;
import org.sixgun.ponyexpress.miroguide.conn.MiroGuideService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

//Base class for MiroCategories, MiroChannels, PodcastEpisodes fragments
public abstract class MiroFragment extends ListFragment implements OnClickListener{

	protected PonyExpressApp mPonyExpressApp;
	protected MiroGuideService mMiroService;
	protected ProgressDialogFragment mProgDialog;

	
	/**
	 * Important to always return a view here, unlike what the docs say.
	 * If not then other methods called later eg:onResume() may try to use 
	 * the listview which won't have been created. The fragment will still
	 * be recreated after oriention change even when not needed.
	 */
	public abstract View onCreateView (LayoutInflater inflater, ViewGroup container,
	Bundle savedInstanceState);
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		mMiroService = new MiroGuideService();
		mPonyExpressApp = (PonyExpressApp) getActivity().getApplication();
		setHasOptionsMenu(true);
		
		mProgDialog = ProgressDialogFragment.newInstance(mPonyExpressApp.getString(R.string.please_wait_simple));
		
	}
	

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.miro_options_menu, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.go_home:
			//FIXME crashes
//			Intent intent = new Intent(mPonyExpressApp, PonyExpressFragsActivity.class);
//			startActivity(intent);
			return true;
		case R.id.settings_menu:
			startActivity(new Intent(mPonyExpressApp, PreferencesActivity.class));
			return true;
		case R.id.search:
			getActivity().onSearchRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
