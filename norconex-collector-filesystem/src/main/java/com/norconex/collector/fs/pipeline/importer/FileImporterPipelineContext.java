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
package com.norconex.collector.fs.pipeline.importer;

import org.apache.commons.vfs2.FileObject;

import com.norconex.collector.core.data.BaseCrawlData;
import com.norconex.collector.core.data.store.ICrawlDataStore;
import com.norconex.collector.core.pipeline.importer.ImporterPipelineContext;
import com.norconex.collector.fs.crawler.FilesystemCrawler;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.doc.FileMetadata;

/**
 * @author Pascal Essiembre
 *
 */
public class FileImporterPipelineContext extends ImporterPipelineContext {

    private final FileObject fileObject;
    
    public FileImporterPipelineContext(
            FilesystemCrawler crawler, ICrawlDataStore crawlDataStore, 
            FileDocument doc, BaseCrawlData crawlData, FileObject fileObject) {
        super(crawler, crawlDataStore, crawlData, doc);
        this.fileObject = fileObject;
    }

    public FilesystemCrawler getCrawler() {
        return (FilesystemCrawler) super.getCrawler();
    }

    public FilesystemCrawlerConfig getConfig() {
        return getCrawler().getCrawlerConfig();
    }
    
    public FileDocument getDocument() {
        return (FileDocument) super.getDocument();
    }

    public FileObject getFileObject() {
        return fileObject;
    }
    
    public FileMetadata getMetadata() {
        return getDocument().getMetadata();
    }
    
}
              