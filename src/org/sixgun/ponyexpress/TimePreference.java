/*
 * Copyright 2012 CommonsWare, Destil , Paul Elms
 * Code modified from http://stackoverflow.com/questions/5533078/timepicker-in-preferencescreen
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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.sixgun.ponyexpress.activity.PonyExpressActivity;
import org.sixgun.ponyexpress.service.ScheduledDownloadService;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference {

	private Calendar calendar;
    private TimePicker picker = null;
    public static final int DEFAULT_HOUR = 2;
    public static final int DEFAULT_MINUTE = 0;
	private long mDefaultTime;
	
	public TimePreference(Context contxt) {
        this(contxt, null);
    }
	
	public TimePreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
		
	}
	
	public TimePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		setPositiveButtonText(R.string.ok);
        setNegativeButtonText(R.string.cancel);
        calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, DEFAULT_HOUR);
        calendar.set(Calendar.MINUTE, DEFAULT_MINUTE);
        mDefaultTime = calendar.getTimeInMillis();
        
	}
	@Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        return (picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        final long time = getPersistedLong(mDefaultTime);
        calendar.setTimeInMillis(time);
        picker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        picker.setCurrentMinute(calendar.get(Calendar.MINUTE));
    }
    
    /**
     * This method is required to make sure the hour field is 
     * re-populated after orientation changes.
     * FIXME Previous changes to the fields are not persisted however. 
     */
    @Override
    protected void showDialog(Bundle state){
    	super.showDialog(state);
    	picker.setCurrentHour(picker.getCurrentHour());
    	picker.setCurrentMinute(picker.getCurrentMinute());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
    	super.onDialogClosed(positiveResult);

    	if (positiveResult) {
    		calendar.set(Calendar.HOUR_OF_DAY, picker.getCurrentHour());
    		calendar.set(Calendar.MINUTE, picker.getCurrentMinute());

    		setSummary(getSummary());

    		if (callChangeListener(calendar.getTimeInMillis())) {
    			persistLong(calendar.getTimeInMillis());
    			notifyChanged();

    			//Set new alarm
    			Intent intent = new Intent(getContext(), ScheduledDownloadService.class);
    			intent.putExtra(PonyExpressActivity.SET_ALARM_ONLY, true);
    			getContext().startService(intent);
    		}
    	}
    }
    //This is needed to ensure that the summary is correctly set from preferences.
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if (restoreValue) {
            if (defaultValue == null) {
                calendar.setTimeInMillis(getPersistedLong(mDefaultTime));
            } else {
                calendar.setTimeInMillis(Long.parseLong(getPersistedString((String) defaultValue)));
            }
        } else {
            if (defaultValue == null) {
                calendar.setTimeInMillis(mDefaultTime);
            } else {
                calendar.setTimeInMillis(Long.parseLong((String) defaultValue));
            }
        }
        setSummary(getSummary());
    }
        @Override
        public CharSequence getSummary() {
            if (calendar == null) {
                return null;
            }
            return DateFormat.getTimeFormat(getContext()).format(new Date(calendar.getTimeInMillis()));
    }
	
   
}
