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
package org.sixgun.ponyexpress.fragment;

import org.sixgun.ponyexpress.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


public class AboutDialogFragment extends DialogFragment {

	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.about, null);
		TextView tv = (TextView)view.findViewById(R.id.tv_about);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(R.drawable.pony_icon);
		builder.setCancelable(true);
		
		String versionNumber;
		try {
            String pkg = getActivity().getApplicationContext().getPackageName();
            versionNumber = getActivity().getApplicationContext().getPackageManager().getPackageInfo(pkg, 0).versionName;
        } catch (NameNotFoundException e) {
            versionNumber = "?";
        }
		String title = getActivity().getApplicationContext().
				getString(R.string.about_title) + " " + versionNumber;
		
		builder.setTitle(title);
		
		builder.setPositiveButton(R.string.draw, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                dismiss();
	           }
		});
		builder.setView(view);
		AlertDialog dialog = builder.create();
		return dialog;
	}

	

}
