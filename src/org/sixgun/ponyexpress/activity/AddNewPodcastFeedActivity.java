/*
 * Copyright 2011 Paul Elms
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class AddNewPodcastFeedActivity extends Activity {

	private TextView mFeedText;
	private TextView mGroupText;
	private TextView mTagText;
	private PonyExpressApp mPonyExpressApp;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_feed);
		
		OnClickListener OKButtonListener =  new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final String username = mFeedText.getText().toString();
				final String password = mGroupText.getText().toString();
				final String tag = mTagText.getText().toString();
				
				//TODO Check feed valid, check tag and group
				//TODO create a new Podcast, and send to db.AddnewPodcast()
				//TODO Lookup feed and add details to db.
				
				finish();
			}
		};
		OnClickListener CancelButtonListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		};
		
		mPonyExpressApp = (PonyExpressApp)getApplication();
		mFeedText = (EditText) findViewById(R.id.feed_entry);
		mGroupText = (EditText) findViewById(R.id.group_entry);
		mTagText = (EditText) findViewById(R.id.tag_entry);
		Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(OKButtonListener);
		Button cancelButton = (Button) findViewById(R.id.cancel);
		cancelButton.setOnClickListener(CancelButtonListener);
	}
	
	/**
	 * Bring up the Settings (preferences) menu via a button click.
	 * @param v, a reference to the button that was clicked to call this.
	 */
	public void showSettings(View v){
		startActivity(new Intent(
        		mPonyExpressApp,PreferencesActivity.class));
	}
	

}
