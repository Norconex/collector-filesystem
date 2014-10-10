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
package com.norconex.collector.fs.crawler;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;

import com.norconex.collector.core.CollectorException;
import com.norconex.collector.core.crawler.AbstractCrawler;
import com.norconex.collector.core.crawler.ICrawler;
import com.norconex.collector.core.data.BaseCrawlData;
import com.norconex.collector.core.data.ICrawlData;
import com.norconex.collector.core.data.store.ICrawlDataStore;
import com.norconex.collector.core.pipeline.BasePipelineContext;
import com.norconex.collector.fs.FilesystemCollectorException;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.pipeline.committer.FileCommitterPipeline;
import com.norconex.collector.fs.pipeline.committer.FileCommitterPipelineContext;
import com.norconex.collector.fs.pipeline.importer.FileImporterPipeline;
import com.norconex.collector.fs.pipeline.importer.FileImporterPipelineContext;
import com.norconex.collector.fs.pipeline.queue.FileQueuePipeline;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.response.ImporterResponse;
import com.norconex.jef4.status.JobStatusUpdater;
import com.norconex.jef4.suite.JobSuite;

/**
 * The Filesystem Crawler.
 * 
 * @author Pascal Dimassimo
 */
public class FilesystemCrawler extends AbstractCrawler {

    private FileSystemManager fileManager;

    /**
     * Constructor.
     * @param crawlerConfig HTTP crawler configuration
     */
    public FilesystemCrawler(FilesystemCrawlerConfig crawlerConfig) {
        super(crawlerConfig);
    }
    
    @Override
    public FilesystemCrawlerConfig getCrawlerConfig() {
        return (FilesystemCrawlerConfig) super.getCrawlerConfig();
    }
    
    /**
     * @return the fileManager
     */
    public FileSystemManager getFileManager() {
        return fileManager;
    }
    
    @Override
    protected void prepareExecution(
            JobStatusUpdater statusUpdater, JobSuite suite, 
            ICrawlDataStore crawlDataStore, boolean resume) {

        try {
            this.fileManager = VFS.getManager();
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
        
        if (!resume) {
            queueStartPaths(crawlDataStore);
        }
    }

    private void queueStartPaths(ICrawlDataStore crawlDataStore) {
        // Queue regular start urls
        String[] startPaths = getCrawlerConfig().getStartPaths();
        if (startPaths != null) {
            for (int i = 0; i < startPaths.length; i++) {
                String startPath = startPaths[i];
                executeQueuePipeline(
                        new BaseCrawlData(startPath), crawlDataStore);
            }
        }
        // Queue start urls define in one or more seed files
        String[] pathsFiles = getCrawlerConfig().getPathsFiles();
        if (pathsFiles != null) {
            for (int i = 0; i < pathsFiles.length; i++) {
                String pathsFile = pathsFiles[i];
                LineIterator it = null;
                try {
                    it = IOUtils.lineIterator(
                            new FileInputStream(pathsFile), CharEncoding.UTF_8);
                    while (it.hasNext()) {
                        String startPath = it.nextLine();
                        executeQueuePipeline(
                                new BaseCrawlData(startPath), crawlDataStore);
                    }
                } catch (IOException e) {
                    throw new CollectorException(
                            "Could not process paths file: " + pathsFile, e);
                } finally {
                    LineIterator.closeQuietly(it);;
                }
            }
        }
    }
    
    @Override
    protected void executeQueuePipeline(
            ICrawlData crawlData, ICrawlDataStore crawlDataStore) {
        BaseCrawlData fsData = (BaseCrawlData) crawlData;
        BasePipelineContext context = 
                new BasePipelineContext(this, crawlDataStore, fsData);
        new FileQueuePipeline().execute(context);
    }

    @Override
    protected ImporterDocument wrapDocument(ICrawlData crawlData,
            ImporterDocument document) {
        return new FileDocument(document);
        //TODO add file metadata from FileObject???
    }

    @Override
    protected ImporterResponse executeImporterPipeline(
            ICrawler crawler, ImporterDocument doc,
            ICrawlDataStore crawlDataStore, BaseCrawlData crawlData) {
        
        //TODO create pipeline context prototype
        //TODO cache the pipeline object?
        FileObject fileObject = null;
        try {
            fileObject = fileManager.resolveFile(crawlData.getReference());
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(
                    "Cannot resolve: " + crawlData.getReference(), e);
        }
        FileImporterPipelineContext context = new FileImporterPipelineContext(
                (FilesystemCrawler) crawler, crawlDataStore, (FileDocument) doc,
                (BaseCrawlData) crawlData, fileObject);
        new FileImporterPipeline(
                getCrawlerConfig().isKeepDownloads()).execute(context);
        return context.getImporterResponse();
    }

    @Override
    protected BaseCrawlData createEmbeddedCrawlData(String embeddedReference,
            ICrawlData parentCrawlData) {
        return new BaseCrawlData(embeddedReference);
    }
    

    @Override
    protected void executeCommitterPipeline(ICrawler crawler,
            ImporterDocument doc, ICrawlDataStore crawlDataStore,
            BaseCrawlData crawlData) {
        
        FileCommitterPipelineContext context = new FileCommitterPipelineContext(
                (FilesystemCrawler) crawler, crawlDataStore, (FileDocument) doc, 
                (BaseCrawlData) crawlData);
        new FileCommitterPipeline().execute(context);
    }
    
    @Override
    protected void markReferenceVariationsAsProcessed(BaseCrawlData crawlData,
            ICrawlDataStore crawlDataStore) {
        // Nothing to do (does not support variations).
    }
    
    @Override
    protected void cleanupExecution(JobStatusUpdater statusUpdater,
            JobSuite suite, ICrawlDataStore refStore) {
        //Nothing to clean-up
    }

}
