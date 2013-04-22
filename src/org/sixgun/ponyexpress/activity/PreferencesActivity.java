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

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.sixgun.ponyexpress.PonyExpressApp;
import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.TimePreference;
import org.sixgun.ponyexpress.service.ScheduledDownloadService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


public class PreferencesActivity extends PreferenceActivity {

	private static final String TAG = "PreferencesActivity";
	SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;
	protected PonyExpressApp mPonyExpressApp;
	private CheckBoxPreference mAutoPlaylistCheckBoxPreference;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		mPonyExpressApp = (PonyExpressApp) getApplication();
		
		//Get the version number and set it as the summary of the version pref.
		PackageManager pm = getPackageManager();
		PackageInfo info = null;
		try {
			info = pm.getPackageInfo("org.sixgun.ponyexpress", 0);
		} catch (NameNotFoundException e) {
			Log.e(TAG,"Cannot find package info..");
		}
			
		Preference version = (Preference)findPreference(getString(R.string.version_key));
		version.setSummary(info.versionName);
		
		mAutoPlaylistCheckBoxPreference = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(getString(R.string.auto_playlist_key));
		
		mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs,String key) {
				if (key.equals(getString(R.string.schedule_download_key))) 
				{
					//if scheduled download pref has not been set yet, set it to the default
					if (prefs.getLong(getString(R.string.schedule_download_time_key), 0) == 0){
						Calendar cal = new GregorianCalendar();
						cal.set(Calendar.HOUR_OF_DAY,TimePreference.DEFAULT_HOUR);
						cal.set(Calendar.MINUTE, TimePreference.DEFAULT_MINUTE);
						final long default_time = cal.getTimeInMillis();
						prefs.edit().putLong(getString(R.string.schedule_download_time_key), default_time).commit();
					}
					
					//Set new alarm
					Intent intent = new Intent(getApplicationContext(), ScheduledDownloadService.class);
					intent.putExtra(PonyExpressActivity.SET_ALARM_ONLY, true);
					getApplicationContext().startService(intent);
				} else if (key.equals(getString(R.string.auto_playlist_key))){
					if (prefs.getBoolean(key, false) == true){
						//compile playlist
						if (!mPonyExpressApp.getDbHelper().compileAutoPlaylist()){
							//Could not compile list, probably as no unlistened 
							//undownloaded episodes.
							Editor editor = prefs.edit();
							editor.putBoolean(key, false);
							editor.commit();
							Toast.makeText(mPonyExpressApp, R.string.no_downloaded_unlistened, Toast.LENGTH_LONG).show();
							mAutoPlaylistCheckBoxPreference.setChecked(false);
						} else {
							Toast.makeText(mPonyExpressApp, R.string.auto_playlist_on, Toast.LENGTH_LONG).show();
						}
					} 					
				}
			} 

		};
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(mPrefListener);
	}
	
}
