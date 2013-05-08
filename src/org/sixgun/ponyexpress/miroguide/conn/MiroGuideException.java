/*
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
 *  
 *  Copyright (c) 2012 Daniel Oeh
 *  
 *  This file is taken from AntennaPod which iss distributed under the following
 *  license:
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 *  
 */
package org.sixgun.ponyexpress.miroguide.conn;


public class MiroGuideException extends Exception{
	private static final long serialVersionUID = -8834656185748713194L;

	public MiroGuideException() {
		super();
	}

	public MiroGuideException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public MiroGuideException(String arg0) {
		super(arg0);
	}

	public MiroGuideException(Throwable arg0) {
		super(arg0);
	}
}
