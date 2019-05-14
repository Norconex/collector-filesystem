/* Copyright 2017-2019 Norconex Inc.
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
package com.norconex.collector.fs.fetch.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileContentInfo;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.local.LocalFile;
import org.apache.commons.vfs2.provider.smb.SmbFileObject;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.CollectorException;
import com.norconex.collector.core.data.CrawlState;
import com.norconex.collector.fs.data.FileCrawlState;
import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.collector.fs.fetch.IFileMetadataFetcher;
import com.norconex.collector.fs.vfs2.provider.cmis.atom.CmisAtomFileObject;
import com.norconex.commons.lang.map.Properties;

/**
 * Generic file system document metadata fetcher.
 * @author Pascal Essiembre
 * @since 2.7.0
 */
public class GenericFileMetadataFetcher implements IFileMetadataFetcher {

    private static final Logger LOG =
            LogManager.getLogger(GenericFileMetadataFetcher.class);

    private static final Map<Class<? extends FileObject>,
            IFileSpecificMetaFetcher> FILE_SPECIFICS = new HashMap<>();
    static {
        FILE_SPECIFICS.put(SmbFileObject.class, new SpecificSmbFetcher());
        FILE_SPECIFICS.put(
                CmisAtomFileObject.class, new SpecificCmisAtomFetcher());
        FILE_SPECIFICS.put(LocalFile.class, new SpecificLocalFileFetcher());
    }

    @Override
    public CrawlState fetchMetadada(
            FileObject fileObject, Properties metadata) {

        LOG.debug("Fetching file headers: " + fileObject);

        try {
            if (!fileObject.exists()) {
                return FileCrawlState.NOT_FOUND;
            }

            IFileSpecificMetaFetcher specificFetcher = FILE_SPECIFICS.get(
                    fileObject.getClass());
            if (specificFetcher != null) {
                specificFetcher.fetchFileSpecificMeta(fileObject, metadata);
            }

            FileContent content = fileObject.getContent();
            //--- Enhance Metadata ---
            metadata.addLong(
                    FileMetadata.COLLECTOR_SIZE, content.getSize());
            metadata.addLong(FileMetadata.COLLECTOR_LASTMODIFIED,
                    content.getLastModifiedTime());
            FileContentInfo info = content.getContentInfo();
            if (info != null) {
                metadata.addString(FileMetadata.COLLECTOR_CONTENT_ENCODING,
                        info.getContentEncoding());
                metadata.addString(FileMetadata.COLLECTOR_CONTENT_TYPE,
                        info.getContentType());
            }
            for (String attrName: content.getAttributeNames()) {
                Object obj = content.getAttribute(attrName);
                if (obj != null) {
                    metadata.addString(FileMetadata.COLLECTOR_PREFIX
                            + "attribute." + attrName,
                                    Objects.toString(obj));
                }
            }

            //TODO support prefixes like http collector?
            //TODO do we really want to prefix attributes with
            // "collector.attribute." or just store as is (like HTTP headers)?

            return FileCrawlState.NEW;
        } catch (FileSystemException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("Cannot fetch metadata: " + fileObject, e);
            } else {
                LOG.error("Cannot fetch metadata: " + fileObject
                        + " (" + e.getMessage() + ")");
            }
            throw new CollectorException(e);
        }
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other, false);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(
                this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
