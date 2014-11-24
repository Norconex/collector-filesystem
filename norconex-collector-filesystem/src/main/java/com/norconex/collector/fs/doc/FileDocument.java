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

import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.importer.doc.ImporterDocument;

//TODO consider dropping since it just brings FileMetadata cast.
public class FileDocument extends ImporterDocument {

    public FileDocument(String reference, CachedInputStream content) {
        super(reference, content, new FileMetadata(reference));
    }

    public FileDocument(ImporterDocument importerDocument) {
        super(importerDocument.getReference(), 
                importerDocument.getContent(),
                new FileMetadata(importerDocument.getMetadata()));
        setReference(importerDocument.getReference());
        setContentType(importerDocument.getContentType());
        setContentEncoding(importerDocument.getContentEncoding());
    }

    public FileMetadata getMetadata() {
        return (FileMetadata) super.getMetadata();
    }
}
