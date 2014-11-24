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
package com.norconex.collector.fs.checksum.impl;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;

import com.norconex.collector.core.checksum.AbstractMetadataChecksummer;
import com.norconex.collector.core.checksum.IMetadataChecksummer;
import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;

/**
 * Default implementation of {@link IMetadataChecksummer} which by default
 * returns the combined values of {@link FileMetadata#COLLECTOR_LASTMODIFIED}
 * and {@link FileMetadata#COLLECTOR_SIZE}, separated with an underscore
 * (e.g. "14125443181234_123").
 * <p/>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;metadataChecksummer 
 *      class="com.norconex.collector.file.checksum.impl.FileMetadataChecksummer"&gt;
 *      keep="[false|true]"
 *      targetField="(field to store checksum)" /&gt;
 * </pre>
 * @author Pascal Essiembre
 */
public class FileMetadataChecksummer extends AbstractMetadataChecksummer {

    @Override
    protected String doCreateMetaChecksum(Properties metadata) {
    	return metadata.getString(FileMetadata.COLLECTOR_LASTMODIFIED)
    	        + "_" + metadata.getString(FileMetadata.COLLECTOR_SIZE);
    }

    @Override
    protected void loadChecksummerFromXML(XMLConfiguration xml) {
        // Nothing more to load
    }

    @Override
    protected void saveChecksummerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        // Nothing more to save
    }
}
