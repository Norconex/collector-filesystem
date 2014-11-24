/* Copyright 2013-2014 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
