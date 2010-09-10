/* * 
 * Copyright (C) 2009-2010 macno.org, Michele Azzolari
 *
 * This file is part of PonyExpress.
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

import android.graphics.Bitmap;

public interface ImageCache {
  public Bitmap get(String url);
  public void put(String url, Bitmap bitmap);
}