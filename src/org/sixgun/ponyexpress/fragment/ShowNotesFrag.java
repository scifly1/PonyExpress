/*
 * Copyright 2014 Paul Elms
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

import org.sixgun.ponyexpress.EpisodeKeys;
import org.sixgun.ponyexpress.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;


public class ShowNotesFrag extends Fragment {

	
	

	public static final String STANDALONE_NOTES = "standalone";
	private Bundle mData;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mData = getArguments();  
		if (mData == null ){ //Only has Arguments when started as standalone notes.
			mData = getActivity().getIntent().getExtras();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v= null;
		if (mData.getBoolean(ShowNotesFrag.STANDALONE_NOTES)){
			v = inflater.inflate(R.layout.standalone_notes,null);
			TextView episode_title = (TextView) v.findViewById(R.id.episode_title);
			String episode_name = mData.getString(EpisodeKeys.TITLE);
			episode_title.setText(episode_name);
		} else {
			v = inflater.inflate(R.layout.notes, null);
		}
		
		WebView description = (WebView) v.findViewById(R.id.Description);
		String descriptionText = mData.getString(EpisodeKeys.DESCRIPTION);
		//We use loadDataWithBaseURL here with no URL so that it expects URL encoding.
		//loadData does not handle the encoding correctly..
		description.loadDataWithBaseURL(null, descriptionText, "text/html", "UTF-8", null);
		return v;
	}
	
	
}
