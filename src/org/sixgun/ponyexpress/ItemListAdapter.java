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

import java.text.DateFormat;
import java.util.List;

import org.sixgun.ponyexpress.miroguide.model.MiroGuideItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class ItemListAdapter extends ArrayAdapter<MiroGuideItem> {
	
	public ItemListAdapter(Context context, int textViewResourceId,
			List<MiroGuideItem> items) {
		super(context, textViewResourceId, items);
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		MiroGuideItem item = getItem(position);
		
		if (convertView == null){
			holder = new Holder();
			LayoutInflater li = (LayoutInflater) getContext().
					getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = li.inflate(R.layout.miro_item_row, null);
			holder.name = (TextView) convertView.findViewById(R.id.episode_name);
			holder.date = (TextView) convertView.findViewById(R.id.episode_date);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}
		holder.name.setText(item.getName());
		DateFormat localFormat = android.text.format.DateFormat.getDateFormat(getContext());
		holder.date.setText(localFormat.format(item.getDate()));
		
		return convertView;
	}

	static class Holder{
		TextView name;
		TextView date;
	}
}
