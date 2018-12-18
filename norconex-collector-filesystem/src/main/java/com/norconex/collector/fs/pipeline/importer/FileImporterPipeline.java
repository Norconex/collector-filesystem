/* Copyright 2013-2018 Norconex Inc.
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
package com.norconex.collector.fs.pipeline.importer;

import java.util.Date;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;

import com.norconex.collector.core.CollectorException;
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
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.collector.fs.doc.IFileDocumentProcessor;
import com.norconex.collector.fs.fetch.IFileMetadataFetcher;
import com.norconex.collector.fs.pipeline.queue.FileQueuePipeline;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.pipeline.Pipeline;

/**
 * @author Pascal Essiembre
 *
 */
public class FileImporterPipeline extends Pipeline<ImporterPipelineContext> {

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
                                childFile.getName().getURI());
                        BasePipelineContext newContext =
                                new BasePipelineContext(ctx.getCrawler(),
                                        ctx.getCrawlDataStore(), crawlData);
                        new FileQueuePipeline().execute(newContext);
                    }
                    return false;
                }
                return true;
            } catch (Exception e) {
                ctx.getCrawlData().setState(CrawlState.ERROR);
                ctx.fireCrawlerEvent(CrawlerEvent.REJECTED_ERROR,
                        ctx.getCrawlData(), this);
                throw new CollectorException(
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
            if (ctx.getConfig().getMetadataFilters() != null
                    && ImporterPipelineUtil.isHeadersRejected(ctx)) {
                ctx.getCrawlData().setState(CrawlState.REJECTED);
                return false;
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

    //--- File Metadata Fetcher ------------------------------------------------
    private static class FileMetadataFetcherStage
            extends AbstractImporterStage {
        @Override
        public boolean executeStage(FileImporterPipelineContext ctx) {
            BaseCrawlData crawlData = ctx.getCrawlData();
            IFileMetadataFetcher metaFetcher =
                    ctx.getConfig().getMetadataFetcher();
            FileMetadata metadata = ctx.getMetadata();

            //TODO consider passing original metadata instead?
            Properties newMeta = new Properties(
                    metadata.isCaseInsensitiveKeys());
            FileObject fileObject = ctx.getFileObject();

            CrawlState state = metaFetcher.fetchMetadada(fileObject, newMeta);

            metadata.putAll(newMeta);

            //--- Apply Metadata to document ---
            // TODO are there headers to enhance first based on attributes
            // (like http collector)?
            FileDocument doc = ctx.getDocument();
            if (doc.getContentType() == null) {
                doc.setContentType(ContentType.valueOf(metadata.getString(
                        FileMetadata.COLLECTOR_CONTENT_TYPE)));
                doc.setContentEncoding(metadata.getString(
                        FileMetadata.COLLECTOR_CONTENT_ENCODING));
            }

            crawlData.setState(state);
            if (state.isGoodState()) {
                ctx.fireCrawlerEvent(CrawlerEvent.DOCUMENT_METADATA_FETCHED,
                        crawlData, fileObject);
            } else {
                String eventType;
                if (state.isOneOf(CrawlState.NOT_FOUND)) {
                    eventType = CrawlerEvent.REJECTED_NOTFOUND;
                } else {
                    eventType = CrawlerEvent.REJECTED_BAD_STATUS;
                }
                ctx.fireCrawlerEvent(eventType, crawlData, fileObject);
                return false;
            }
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
            FileDocument doc = ctx.getDocument();
            FileObject fileObject = ctx.getFileObject();
            CrawlState state = ctx.getConfig().getDocumentFetcher()
                    .fetchDocument(ctx.getFileObject(), doc);
            crawlData.setCrawlDate(new Date());
            crawlData.setContentType(doc.getContentType());
            crawlData.setState(state);

            if (state.isGoodState()) {
                ctx.fireCrawlerEvent(CrawlerEvent.DOCUMENT_FETCHED,
                        crawlData, fileObject);
            } else {
                String eventType;
                if (state.isOneOf(CrawlState.NOT_FOUND)) {
                    eventType = CrawlerEvent.REJECTED_NOTFOUND;
                } else {
                    eventType = CrawlerEvent.REJECTED_BAD_STATUS;
                }
                ctx.fireCrawlerEvent(eventType, crawlData, fileObject);
                return false;
            }
            return true;
        }
    }
}

