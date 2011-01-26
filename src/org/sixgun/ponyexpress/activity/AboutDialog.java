/*
 * Copyright (C) 2009-2010 macno.org, Michele Azzolari
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;



public class AboutDialog {
	static AlertDialog create(Context context) {
		View view = LayoutInflater.from(context).inflate(R.layout.about, null);
		TextView tv = (TextView)view.findViewById(R.id.tv_about);
		tv.setMovementMethod(LinkMovementMethod.getInstance());


		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setIcon(R.drawable.pony_icon);
		builder.setView(view);
		builder.setCancelable(true);
		builder.setTitle(R.string.about_title);
		builder.setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                dialog.cancel();
	           }
		});
		AlertDialog dialog = builder.create();
		return dialog;
		
	}
}
