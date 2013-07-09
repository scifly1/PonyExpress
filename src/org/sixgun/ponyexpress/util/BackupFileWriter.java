/*
 * Copyright 2012 James Daws
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

package org.sixgun.ponyexpress.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.sixgun.ponyexpress.BuildConfig;
import org.sixgun.ponyexpress.ReturnCodes;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;

public class BackupFileWriter {

private static final String TAG = "PonyExpress BackupFileWriter";

	public int writeBackupOpml(List<String> podcasts, File f){

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "BackupFileWriter started");
		}
		//create a new backup file in the SD card
		Utils.writePodcastPath();
		File opmlfile = f;

		//File Chooser checks if file already exists and if SDcard is mounted
		try{
			opmlfile.createNewFile();
		}catch(IOException e){
			Log.e(TAG, "exception in createNewFile() method");
		}

		FileOutputStream fileos = null;
		try{
			fileos = new FileOutputStream(opmlfile);
		}catch(FileNotFoundException e){
			Log.e("FileNotFoundException", "can't create FileOutputStream");
		}

		XmlSerializer serializer = Xml.newSerializer();

		try {
			serializer.setOutput(fileos, "UTF-8");
			serializer.startDocument("UTF-8", null);
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			serializer.startTag(null, "opml");
			serializer.attribute(null, "version", "2.0");
			serializer.startTag(null, "head");
			serializer.startTag(null, "title");
			serializer.text("PonyExpress");
			serializer.endTag(null, "title");
			serializer.startTag(null, "dateCreated");
			serializer.text(DateFormat.getDateTimeInstance().format(new Date()));
			serializer.endTag(null, "dateCreated");
			serializer.endTag(null, "head");
			serializer.startTag(null, "body");
			for (String url: podcasts){
				serializer.startTag(null, "outline");
				serializer.attribute(null, "xmlUrl", url);
				serializer.endTag(null, "outline");
			}
			serializer.endDocument();
			serializer.flush();
			fileos.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Backup successful");
		}
		return ReturnCodes.ALL_OK;
	}
}