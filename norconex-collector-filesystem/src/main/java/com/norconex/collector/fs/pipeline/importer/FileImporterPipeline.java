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

import java.util.Objects;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileContentInfo;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.checksum.IMetadataChecksummer;
import com.norconex.collector.core.crawler.event.CrawlerEvent;
import com.norconex.collector.core.data.BaseCrawlData;
import com.norconex.collector.core.data.CrawlState;
import com.norconex.collector.core.pipeline.BasePipelineContext;
import com.norconex.collector.core.pipeline.ChecksumStageUtil;
import com.norconex.collector.core.pipeline.importer.DocumentFiltersStage;
import com.norconex.collector.core.pipeline.importer.ImportModuleStage;
import com.norconex.collector.core.pipeline.importer.ImporterPipelineContext;
import com.norconex.collector.core.pipeline.importer.ImporterPipelineUtil;
import com.norconex.collector.core.pipeline.importer.SaveDocumentStage;
import com.norconex.collector.fs.FilesystemCollectorException;
import com.norconex.collector.fs.data.FileCrawlState;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.collector.fs.doc.IFileDocumentProcessor;
import com.norconex.collector.fs.pipeline.queue.FileQueuePipeline;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.pipeline.Pipeline;

/**
 * @author Pascal Essiembre
 *
 */
public class FileImporterPipeline extends Pipeline<ImporterPipelineContext> {

    private static final Logger LOG = 
            LogManager.getLogger(FileImporterPipeline.class);
    
    public FileImporterPipeline(boolean isKeepDownloads) {
        addStage(new FolderPathsExtractorStage());
        addStage(new FileMetadataFetcherStage());
        addStage(new FileMetadataFiltersStage());
        addStage(new FileMetadataChecksumStage());
        addStage(new DocumentFetchStage());
        if (isKeepDownloads) {
            addStage(new SaveDocumentStage());
        }
        addStage(new DocumentFiltersStage());
        addStage(new DocumentPreProcessingStage());
        addStage(new ImportModuleStage());
    }

    //--- Folder Path Extractor ------------------------------------------------
    // Extract paths to queue them and stop processing this folder
    private static class FolderPathsExtractorStage 
            extends AbstractImporterStage {
        @Override
        public boolean executeStage(FileImporterPipelineContext ctx) {
            try {
                FileObject file = ctx.getFileObject();
                if (file.getType() == FileType.FOLDER) {
                    FileObject[] files = file.getChildren();
                    for (FileObject childFile : files) {
                        BaseCrawlData crawlData = new BaseCrawlData(
                                childFile.getURL().toString());
                        BasePipelineContext newContext = 
                                new BasePipelineContext(ctx.getCrawler(), 
                                        ctx.getCrawlDataStore(), crawlData);
                        new FileQueuePipeline().execute(newContext);
                    }
                    return false;
                }
                return true;
            } catch (FileSystemException e) {
                ctx.getCrawlData().setState(CrawlState.ERROR);
                ctx.fireCrawlerEvent(CrawlerEvent.REJECTED_ERROR, 
                        ctx.getCrawlData(), this);
                throw new FilesystemCollectorException(
                        "Cannot extract folder paths: " 
                                + ctx.getCrawlData().getReference(), e);
            }
        }
    }    

    
    //--- Metadata filters -----------------------------------------------------
    private static class FileMetadataFiltersStage 
            extends AbstractImporterStage {
        @Override
        public boolean executeStage(FileImporterPipelineContext ctx) {
            if (ctx.getConfig().getMetadataFilters() == null) {
                if (ImporterPipelineUtil.isHeadersRejected(ctx)) {
                    ctx.getCrawlData().setState(CrawlState.REJECTED);
                    return false;
                }
            }
            return true;
        }
    }    

    
    //--- Document Pre-Processing ----------------------------------------------
    private static class DocumentPreProcessingStage 
            extends AbstractImporterStage {
        @Override
        public boolean executeStage(FileImporterPipelineContext ctx) {
            if (ctx.getConfig().getPreImportProcessors() != null) {
                for (IFileDocumentProcessor preProc :
                        ctx.getConfig().getPreImportProcessors()) {
                    preProc.processDocument(
                            ctx.getCrawler().getFileManager(),
                            ctx.getDocument());
                    ctx.getCrawler().fireCrawlerEvent(
                            CrawlerEvent.DOCUMENT_PREIMPORTED, 
                            ctx.getCrawlData(), preProc);
                }
            }
            return true;
        }
    }    

    //--- IMPORT Module --------------------------------------------------------
    private static class FileMetadataFetcherStage 
            extends AbstractImporterStage {
        @Override
        public boolean executeStage(FileImporterPipelineContext ctx) {
            FileDocument doc = ctx.getDocument();
            FileMetadata metadata = ctx.getMetadata();
            try {
                FileContent content = ctx.getFileObject().getContent();
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
                
                //--- Apply Metadata to document ---
                if (doc.getContentType() == null) {
                    doc.setContentType(ContentType.valueOf(
                            doc.getMetadata().getString(
                                    FileMetadata.COLLECTOR_CONTENT_TYPE)));
                    doc.setContentEncoding(doc.getMetadata().getString(
                            FileMetadata.COLLECTOR_CONTENT_ENCODING));
                }
                
            } catch (FileSystemException e) {
                ctx.getCrawlData().setState(CrawlState.ERROR);
                ctx.fireCrawlerEvent(CrawlerEvent.REJECTED_ERROR, 
                        ctx.getCrawlData(), this);

                throw new FilesystemCollectorException(
                        "Cannot fetch file metadata: " 
                                + ctx.getCrawlData().getReference(), e);
            }
            ctx.fireCrawlerEvent(CrawlerEvent.DOCUMENT_METADATA_FETCHED, 
                    ctx.getCrawlData(), this);
            return true;
        }
    }    

    //--- HTTP Document Checksum -----------------------------------------------
    private static class FileMetadataChecksumStage 
            extends AbstractImporterStage {
        @Override
        public boolean executeStage(FileImporterPipelineContext ctx) {
            //TODO only if an INCREMENTAL run... else skip.
            
            IMetadataChecksummer check = 
                    ctx.getConfig().getMetadataChecksummer();
            if (check != null) {
                String newChecksum = 
                        check.createMetadataChecksum(ctx.getMetadata());
                return ChecksumStageUtil.resolveMetaChecksum(
                        newChecksum, ctx, this);
            }
            return true;
        }
    }   
    

    //--- Document Fetch -------------------------------------------------------
    private static class DocumentFetchStage 
            extends AbstractImporterStage {
        @Override
        public boolean executeStage(FileImporterPipelineContext ctx) {
            BaseCrawlData crawlData = ctx.getCrawlData();
            //TODO replace signature with Writer class.
            LOG.debug("Fetching document: " + ctx.getDocument().getReference());
            try {
                if (!ctx.getFileObject().exists()) {
                    crawlData.setState(FileCrawlState.NOT_FOUND);
                    ctx.fireCrawlerEvent(CrawlerEvent.REJECTED_NOTFOUND, 
                            crawlData, ctx.getFileObject());
                    return false;
                }
                ctx.getDocument().setContent(
                        ctx.getCrawler().getStreamFactory().newInputStream(
                                ctx.getFileObject().getContent()
                                        .getInputStream()));
                // if not set to new or modified already, make it new
                if (!crawlData.getState().isCommittable()) {
                    crawlData.setState(CrawlState.NEW);
                }
                ctx.fireCrawlerEvent(
                        CrawlerEvent.DOCUMENT_FETCHED, crawlData, this);
                return true;
            } catch (Exception e) {
                crawlData.setState(CrawlState.ERROR);
                ctx.fireCrawlerEvent(CrawlerEvent.REJECTED_ERROR, 
                        crawlData, this);
                if (LOG.isDebugEnabled()) {
                    LOG.error("Cannot fetch document: " 
                            + crawlData.getReference()
                            + " (" + e.getMessage() + ")", e);
                } else {
                    LOG.error("Cannot fetch document: " 
                            + crawlData.getReference()
                            + " (" + e.getMessage() + ")");
                }
                throw new FilesystemCollectorException(e);
            }  

        }
    }  
}
