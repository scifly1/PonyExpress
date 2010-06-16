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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Service that handles interaction with Identi.ca.
 *
 */
public class IdenticaHandler extends Service {

	
	private static final String TAG = "PonyExpress IdenticaHandler";
	private static final String API = "http://identi.ca/api/";
	private final IBinder mBinder = new IdenticaHandlerBinder();
	
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class IdenticaHandlerBinder extends Binder {
        IdenticaHandler getService() {
            return IdenticaHandler.this;
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}


	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "PonyExpress IdenticaHandler started");
		
	}
	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "PonyExpress IdenticaHandler stopped");
	}
	
	public List<Dent> queryIdentica(String query){
		//TODO Start a new thread for the query.
		String q = query;
		String encoded_q = null;
		try {
			encoded_q = URLEncoder.encode(q,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Catch this better
			throw new RuntimeException(e);
		}
		String url = new String(API + "search.atom?q=" + encoded_q);
		Log.d(TAG,"Identica query: "+ url);
		DentParser parser = new DentParser(url);
		List<Dent> dents = parser.parse();
		return dents;
	}
}
