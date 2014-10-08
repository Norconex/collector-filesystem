/* Copyright 2013-2014 Norconex Inc.
 * 
 * This file is part of Norconex Filesystem Collector.
 * 
 * Norconex Filesystem Collector is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Filesystem Collector is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Filesystem Collector. If not, 
 * see <http://www.gnu.org/licenses/>.
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
