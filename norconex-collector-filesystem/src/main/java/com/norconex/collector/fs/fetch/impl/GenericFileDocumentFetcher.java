/* Copyright 2017 Norconex Inc.
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.vfs2.FileObject;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.CollectorException;
import com.norconex.collector.core.data.CrawlState;
import com.norconex.collector.fs.data.FileCrawlState;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.fetch.IFileDocumentFetcher;

/**
 * Generic file system document fetcher.
 * @author Pascal Essiembre
 * @since 2.7.0
 */
public class GenericFileDocumentFetcher implements IFileDocumentFetcher {

    private static final Logger LOG =
            LogManager.getLogger(GenericFileDocumentFetcher.class);

    @Override
    public CrawlState fetchDocument(FileObject fileObject, FileDocument doc) {

        LOG.debug("Fetching document: " + doc.getReference());

        try {
            if (!fileObject.exists()) {
                return FileCrawlState.NOT_FOUND;
            }
            doc.setContent(doc.getContent().getStreamFactory().newInputStream(
                    fileObject.getContent().getInputStream()));
            //read a copy to force caching and then close the HTTP stream
            IOUtils.copy(doc.getContent(), new NullOutputStream());
            return CrawlState.NEW;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("Cannot fetch document: " + doc.getReference()
                        + " (" + e.getMessage() + ")", e);
            } else {
                LOG.error("Cannot fetch document: " + doc.getReference()
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
