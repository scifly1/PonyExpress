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

import org.sixgun.ponyexpress.R;
import org.sixgun.ponyexpress.service.IdenticaHandler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Activity to gather login infomation from the user for Identi.ca
 * TODO allow new users to set up new identi.ca account 
 *
 */
public class IdenticaAccountSetupActivity extends Activity {

	EditText mUserNameText;
	EditText mPasswordText;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_setup);
		
		OnClickListener OKButtonListener =  new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra(IdenticaHandler.USERNAME, 
						mUserNameText.getText().toString());
				intent.putExtra(IdenticaHandler.PASSWORD, 
						mPasswordText.getText().toString());
				setResult(RESULT_OK, intent);
				finish();
			}
		};
		OnClickListener CancelButtonListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		};
		
		mUserNameText = (EditText) findViewById(R.id.username_entry);
		mPasswordText = (EditText) findViewById(R.id.password_entry);
		Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(OKButtonListener);
		Button cancelButton = (Button) findViewById(R.id.cancel);
		cancelButton.setOnClickListener(CancelButtonListener);
	}
	
}
