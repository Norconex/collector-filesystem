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

import com.norconex.commons.lang.map.Properties;

public class FileMetadata extends Properties {

    private static final long serialVersionUID = -562425360774678869L;

    public static final String COLLECTOR_PREFIX = "collector.filesystem.";

    public static final String DOC_PATH = COLLECTOR_PREFIX + "path";
    
    public static final String DOC_SIZE = COLLECTOR_PREFIX + "filesize";
    public static final String DOC_LASTMODIFIED = 
            COLLECTOR_PREFIX + "lastmodified";
    public static final String DOC_CONTENT_ENCODING = 
            COLLECTOR_PREFIX + "content-encoding";
    public static final String DOC_CONTENT_TYPE = 
            COLLECTOR_PREFIX + "content-type";

    public static final String CHECKSUM_METADATA = 
            COLLECTOR_PREFIX + "checksum-metadata";
    public static final String CHECKSUM_DOC = 
            COLLECTOR_PREFIX + "checksum-doc";
    
    
    
//    public static final String DOC_MIMETYPE = COLLECTOR_PREFIX + "mimetype";
//    public static final String DOC_CHARSET = COLLECTOR_PREFIX + "charset";
//    public static final String DOC_DEPTH = COLLECTOR_PREFIX + "depth";

	
	public FileMetadata(String documentPath) {
		super(false);
		addString(DOC_PATH, documentPath);
	}

//	public ContentType getContentType() {
//	    String type = getString(HTTP_CONTENT_TYPE);
//	    if (type != null) {
//	        type = type.replaceFirst("(.*?)(\\;)(.*)", "$1");
//	    }
//		return ContentType.newContentType(type);
//	}
	public String getDocumentUrl() {
	    return getString(DOC_PATH);
	}
//	public Collection<String> getDocumentUrls() {
//	    return getStrings(REFERNCED_URLS);
//	}
	
}
