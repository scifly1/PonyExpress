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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;


public class ProgressDialogFragment extends DialogFragment {

	private static final String MESSAGE_TEXT = "text";

	public static ProgressDialogFragment newInstance(CharSequence message){
		ProgressDialogFragment dialog = new ProgressDialogFragment();
		Bundle args = new Bundle();
		args.putCharSequence(MESSAGE_TEXT, message);
		dialog.setArguments(args);
		return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
	    final ProgressDialog dialog = new ProgressDialog(getActivity());
	    Bundle data;
		if (savedInstanceState != null){
			data = savedInstanceState;
		} else {
			data = getArguments();
		}
		if (data == null){
			dialog.setMessage(getText(R.string.setting_up));
		} else {
			CharSequence text = data.getCharSequence(MESSAGE_TEXT);
			dialog.setMessage(text);
		}
	    dialog.setIndeterminate(true);
	    return dialog;
	}
	
	
}
