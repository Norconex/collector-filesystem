/* Copyright 2013-2017 Norconex Inc.
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

import com.norconex.collector.core.doc.CrawlDoc;
import com.norconex.collector.core.doc.CrawlDocInfo;
import com.norconex.commons.lang.io.CachedInputStream;

//TODO consider dropping since it just brings FileMetadata cast.
public class FileDocument extends CrawlDoc {

    public FileDocument(String reference, CachedInputStream content) {
        super(new CrawlDocInfo(reference), content);
    }

    public FileDocument(CrawlDoc doc) {
        super(doc.getDocInfo(), doc.getCachedDocInfo(),
                doc.getInputStream(), doc.isOrphan());
    }

    @Override
    public CrawlDocInfo getDocInfo() {
        return (CrawlDocInfo) super.getDocInfo();
    }

    @Override
    public String getReference() {
        return getDocInfo().getReference();
    }

    public void setContentType(com.norconex.commons.lang.file.ContentType type) {
        getDocInfo().setContentType(type);
    }

    public void setContentEncoding(String encoding) {
        getDocInfo().setContentEncoding(encoding);
    }

    public com.norconex.commons.lang.file.ContentType getContentType() {
        return getDocInfo().getContentType();
    }

    public String getContentEncoding() {
        return getDocInfo().getContentEncoding();
    }

    public FileMetadata getFileMetadata() {
        return new FileMetadata(getMetadata());
    }
}
