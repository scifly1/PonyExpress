/*
 * Copyright 2012 Paul Elms
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

import android.view.View;


public interface PlaylistInterface {

	/**
	 * Starts EpisdodeTabs with the Player etc.. with a playlist
	 * @param v
	 */
	public void startPlaylist(View v);
	
	/**
	 * This method lists the podcasts currently in the playlist.
	 */
	public void listPlaylist();
	
	/**
	 * This method opens the Download overview activity.
	 * @param v
	 */
	public void openDownloadOverview(View v);
}
