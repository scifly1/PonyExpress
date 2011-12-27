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
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity to gather login infomation from the user for Identi.ca
 *
 */
public class IdenticaAccountSetupActivity extends Activity {

	private VerifyCredentials mTask;
	private ProgressDialog mProgDialog;
	private static final String TAG = "IdenticaAccountSetup";
	EditText mUserNameText;
	EditText mPasswordText;
	protected IdenticaHandler mIdenticaHandler;
	private boolean mIdenticaHandlerBound;
	
	//This is all responsible for connecting/disconnecting to the IdenticaHandler service.
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mIdenticaHandler = null;
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to an explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
			mIdenticaHandler = ((IdenticaHandler.IdenticaHandlerBinder)service).getService();
		}
	};
	
	protected void doBindIdenticaHandler() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
		
		//getApplicationContext().bindService() called instead of bindService(), as
		//bindService() does not work when called from the child Activity of an ActivityGroup
		//ie:TabActivity
	    getApplicationContext().bindService(new Intent(this, 
	            IdenticaHandler.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIdenticaHandlerBound = true;
	}


	protected void doUnbindIdenticaHandler() {
	    if (mIdenticaHandlerBound) {
	        // Detach our existing connection.
	    	//Must use getApplicationContext.unbindService() as 
	    	//getApplicationContext().bindService was used to bind initially.
	        getApplicationContext().unbindService(mConnection);
	        mIdenticaHandlerBound = false;
	    }
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_setup);
		doBindIdenticaHandler();
		
		//Create Progress Dialogs for later use.
		mProgDialog = new ProgressDialog(this);
		mProgDialog.setMessage(getString(R.string.verify_creds));
		
		OnClickListener OKButtonListener =  new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final String username = mUserNameText.getText().toString();
				final String password = mPasswordText.getText().toString();
				mIdenticaHandler.setCredentials(username, password);
				mTask = new VerifyCredentials();
				mTask.execute();
			}
		};
		
		OnClickListener CancelButtonListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
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


	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Dismiss dialog now or it will leak.
		if (mProgDialog.isShowing()){
			mProgDialog.dismiss();
		}
		doUnbindIdenticaHandler();
	}
	

	private class VerifyCredentials extends AsyncTask<Void, Void, Integer> {
		
		@Override
		protected void onPreExecute() {
			mProgDialog.show();
		}
		
		@Override
		protected Integer doInBackground(Void... unused) {
			Integer status = mIdenticaHandler.verifyCredentials();
			return status;
		}
		
		@Override
		protected void onPostExecute(Integer status) {
			mProgDialog.hide();
			switch (status) {
			case 999:
				Log.d(TAG,"Credentials verified");
				Toast.makeText(IdenticaAccountSetupActivity.this, R.string.login_successful,Toast.LENGTH_LONG).show();
				finish();
				break;
			case 1:
				Log.d(TAG,"ClientProtocolException thrown");
				Toast.makeText(IdenticaAccountSetupActivity.this, "ClientProtocolException",Toast.LENGTH_LONG).show();
				break;
			case 2:
				Log.d(TAG,"Identi.ca is offline");
				Toast.makeText(IdenticaAccountSetupActivity.this, R.string.identica_offline,Toast.LENGTH_LONG).show();
				break;
			case 3:
				Log.d(TAG,"Credentials Not Verified");
				Toast.makeText(IdenticaAccountSetupActivity.this, R.string.credentials_not_verified,Toast.LENGTH_LONG).show();
				break;
			case 4:
				Log.d(TAG,"No internet connection");
				Toast.makeText(IdenticaAccountSetupActivity.this, R.string.no_internet_connection,Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
}
