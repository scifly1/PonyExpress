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
package org.sixgun.ponyexpress.activity;

import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * This is a base class that all activities that require a basic options menu 
 * (Settings, identica account setup) can inherit from to get the menu. 
 *
 * It extends TabActivity as it is used sub-classed by TabActivitys rather than plain Activitys. 
 */

//FIXME This is only used by the EpisodeTabs activity.  
//All other activites set up options independently.

public class GeneralOptionsMenuActivity extends TabActivity {

	private static final int SETUP_ACCOUNT = 0;
	private PonyExpressApp mPonyExpressApp;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		mPonyExpressApp = (PonyExpressApp) getApplication();
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.general_options_menu, menu);
		    return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.settings_menu:
	        startActivity(new Intent(
	        		mPonyExpressApp,PreferencesActivity.class));
	        return true;
	    case R.id.identica_account_settings:
	    	//Fire off AccountSetup screen
			startActivityForResult(new Intent(
					mPonyExpressApp,IdenticaAccountSetupActivity.class),
					SETUP_ACCOUNT);
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
		}
	}
}
