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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Service that handles interaction with Identi.ca.
 *
 */
public class IdenticaHandler extends Service {

	
	private static final String TAG = "PonyExpress IdenticaHandler";
	private static final String SEARCH_API = "https://identi.ca/api/search.atom?q=";
	private static final String UPDATE_API = "https://identi.ca/api/statuses/update.xml";
	public static final String LOGINFILE = "IdenticaLogin";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private final IBinder mBinder = new IdenticaHandlerBinder();
	
	private String mUserName = "";
	private String mPassword = "";
	
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
		
		SharedPreferences loginDetails = getSharedPreferences(LOGINFILE,0);
		mUserName = loginDetails.getString(USERNAME, "");
		mPassword = loginDetails.getString(PASSWORD, "");
		
	}
	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "PonyExpress IdenticaHandler stopped");
	}
	
	public ArrayList<Dent> queryIdentica(String query){
		//TODO Start a new thread for the query.
		String q = query;
		String encoded_q = null;
		try {
			encoded_q = URLEncoder.encode(q,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Cannot encode your identi.ca query!", e);
		}
		String url = new String(SEARCH_API + encoded_q);
		Log.d(TAG,"Identica query: "+ url);
		DentParser parser = new DentParser(getApplicationContext(),url);
		ArrayList<Dent> dents = parser.parse();
		return dents;
	}
	/**
	 * Posts the String dent on Identica.
	 * @param dent
	 * @return true if successfull, false if no account set up yet.
	 */
	public boolean postDent(String dent) {
		//TODO Do this in a new thread
		//Check credentials are set up
		if (mUserName.equals("")){
			Log.d(TAG,"No user details found!");			
			return false;
		}
		//Send dent 
		DefaultHttpClient httpClient = setUpClient();
		HttpPost post = setUpPOST(dent);
		
		//TODO Catch errors using the response
		HttpResponse response = null;
		try {
			response = httpClient.execute(post);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String responseBody = null;
		try {
			responseBody = EntityUtils.toString(response.getEntity());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d(TAG, responseBody);
		return true;		
	}
	
	/**
	 * Sets up a new HttpClient with the correct username and password.
	 * @return HttpClient
	 */
	private DefaultHttpClient setUpClient() {
		DefaultHttpClient client = new DefaultHttpClient();
		
		//Give client username and password.
		BasicCredentialsProvider credProvider = new BasicCredentialsProvider();
		credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
				AuthScope.ANY_PORT),new UsernamePasswordCredentials(mUserName, mPassword));
		client.setCredentialsProvider(credProvider);
		client.addRequestInterceptor(preemptiveAuth,0);
		
		return client;
	}

	/**
	 * Creates a new HTTP POST method with the dent to be sent.
	 * @param dent to be sent
	 * @return post method
	 */
	private HttpPost setUpPOST(String dent) {
		HttpPost post = new HttpPost(UPDATE_API);
		//Set up the parameters
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("status", dent));
		params.add(new BasicNameValuePair("source", PonyExpressApp.APPLICATION_NAME));
		//Encode params and set as the POST Entity
		HttpEntity data = null;
		try {
			data = new UrlEncodedFormEntity(params,HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Cannot encode your dent!", e);
		}
		post.setEntity(data);
		return post;
	}


	
	/**
	 * This preemptively handles the request from the server to provide credentials.
	 * This code was taken from HttpManager.java from Mustard http://gitorious.org/mustard.
	 * Copyright  2009-2010 macno.org, Michele Azzolari
	 */
	private HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {
		
		@Override
		public void process(final HttpRequest request, final HttpContext context)
				throws HttpException, IOException {
			
			AuthState authState = (AuthState) context.getAttribute(
					ClientContext.TARGET_AUTH_STATE);
			CredentialsProvider credProvider = (CredentialsProvider) context.getAttribute(
					ClientContext.CREDS_PROVIDER);
			HttpHost targetHost = (HttpHost) context.getAttribute(
					ExecutionContext.HTTP_TARGET_HOST);
			
			//if no AuthScheme has been initialised yet
			if (authState.getAuthScheme() == null){
				AuthScope authScope = new AuthScope(
						targetHost.getHostName(), targetHost.getPort());
				//Obtain credentials matching the target host
				Credentials creds = credProvider.getCredentials(authScope);
				//If found generate BasicScheme preemptively
				if (creds != null){
					authState.setAuthScheme(new BasicScheme());
					authState.setCredentials(creds);
				}
			}
		}
	};
	
}