/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex HTTP Collector.
 * 
 * Norconex HTTP Collector is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex HTTP Collector is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex HTTP Collector. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.collector.fs.doc;

import com.norconex.collector.core.doc.CollectorMetadata;
import com.norconex.commons.lang.map.Properties;

public class FileMetadata extends CollectorMetadata {

    private static final long serialVersionUID = -562425360774678869L;

    public static final String COLLECTOR_PATH = COLLECTOR_PREFIX + "path";
    
    public static final String COLLECTOR_SIZE = COLLECTOR_PREFIX + "filesize";
    public static final String COLLECTOR_LASTMODIFIED = 
            COLLECTOR_PREFIX + "lastmodified";
    
	public FileMetadata(String documentPath) {
		super();
		addString(COLLECTOR_PATH, documentPath);
	}

    public FileMetadata(Properties metadata) {
        super(metadata);
    }
	
	public String getDocumentPath() {
	    return getString(COLLECTOR_PATH);
	}
}
