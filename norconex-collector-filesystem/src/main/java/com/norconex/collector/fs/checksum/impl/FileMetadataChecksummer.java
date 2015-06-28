/* Copyright 2013-2015 Norconex Inc.
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
import com.norconex.collector.core.checksum.impl.GenericMetadataChecksummer;
import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;

/**
 * <p>Default implementation of {@link IMetadataChecksummer} which by default
 * returns the combined values of {@link FileMetadata#COLLECTOR_LASTMODIFIED}
 * and {@link FileMetadata#COLLECTOR_SIZE}, separated with an underscore
 * (e.g. "14125443181234_123").</p>
 * <p>
 * To use different fields (one or several) to constitute a checksum,
 * you can use the {@link GenericMetadataChecksummer}.
 * </p> 
 * <p>
 * Since 2.2.0, this implementation can be disabled in your 
 * configuration by specifying <code>disabled="true"</code>. When disabled,
 * the checksum returned is always <code>null</code>.  
 * </p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;metadataChecksummer 
 *      class="com.norconex.collector.file.checksum.impl.FileMetadataChecksummer"&gt;
 *      disabled="[false|true]"
 *      keep="[false|true]"
 *      targetField="(field to store checksum)" /&gt;
 * </pre>
 * @author Pascal Essiembre
 * @see GenericMetadataChecksummer
 */
public class FileMetadataChecksummer extends AbstractMetadataChecksummer {

    private boolean disabled;
    
    @Override
    protected String doCreateMetaChecksum(Properties metadata) {
        if (disabled) {
            return null;
        }
    	return metadata.getString(FileMetadata.COLLECTOR_LASTMODIFIED)
    	        + "_" + metadata.getString(FileMetadata.COLLECTOR_SIZE);
    }
    
    /**
     * Whether this checksummer is disabled or not. When disabled, not
     * checksum will be created (the checksum will be <code>null</code>).
     * @return <code>true</code> if disabled
     * @since 2.2.0
     */
    public boolean isDisabled() {
        return disabled;
    }
    /**
     * Sets whether this checksummer is disabled or not. When disabled, not
     * checksum will be created (the checksum will be <code>null</code>).
     * @param disabled <code>true</code> if disabled
     * @since 2.2.0
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }       

    @Override
    protected void loadChecksummerFromXML(XMLConfiguration xml) {
        setDisabled(xml.getBoolean("[@disabled]", disabled));
    }

    @Override
    protected void saveChecksummerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttributeBoolean("disabled", isDisabled());
    }
}
