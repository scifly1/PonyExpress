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
package org.sixgun.ponyexpress;

import java.util.List;

import org.sixgun.ponyexpress.miroguide.model.MiroGuideChannel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Subclass of ArrayAdapter that holds MiroGuideChannel objects for a 
 * list adapter. Requires sub-classing as an enclosed class within the Activity 
 * it is used so that methods within the activity can be called by the 
 * onClickListener defined in the sub-class.
 * 
 */
public class ChannelListAdapter extends ArrayAdapter<MiroGuideChannel> {

	public ChannelListAdapter(Context context, int textViewResourceId,
			List<MiroGuideChannel> channels) {
		super(context, textViewResourceId, channels);
	}
	

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		MiroGuideChannel channel = getItem(position);
		
		if (convertView == null){
			holder = new Holder();
			LayoutInflater li = (LayoutInflater) getContext().
					getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = li.inflate(R.layout.episode_row, null);
			holder.name = (TextView) convertView.findViewById(R.id.episode_text);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		holder.name.setText(channel.getName());
		
		convertView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
		
		return convertView;
	}

	static class Holder{
		TextView name;
	}

}
