/* Copyright 2010-2014 Norconex Inc.
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

	private static final long serialVersionUID = -6759418012119786557L;

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
