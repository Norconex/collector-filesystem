/**
 * 
 */
package com.norconex.collector.fs.pipeline.importer;

import java.util.Objects;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileContentInfo;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.checksum.IMetadataChecksummer;
import com.norconex.collector.core.crawler.event.CrawlerEvent;
import com.norconex.collector.core.data.BaseCrawlData;
import com.norconex.collector.core.data.CrawlState;
import com.norconex.collector.core.pipeline.ChecksumStageUtil;
import com.norconex.collector.core.pipeline.importer.ImportModuleStage;
import com.norconex.collector.core.pipeline.importer.ImporterPipelineContext;
import com.norconex.collector.core.pipeline.importer.SaveDocumentStage;
import com.norconex.collector.fs.FilesystemCollectorException;
import com.norconex.collector.fs.data.FileCrawlState;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.collector.fs.doc.IFileDocumentProcessor;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.pipeline.Pipeline;

/**
 * @author Pascal Essiembre
 *
 */
public class FileImporterPipeline 
        extends Pipeline<ImporterPipelineContext> {

    private static final Logger LOG = 
            LogManager.getLogger(FileImporterPipeline.class);
    
    public FileImporterPipeline(boolean isKeepDownloads) {
        addStage(new DocumentMetadataFetcherStage());
        addStage(new FileMetadataChecksumStage());
        addStage(new DocumentFetchStage());
        if (isKeepDownloads) {
            addStage(new SaveDocumentStage());
        }
        addStage(new DocumentPreProcessingStage());
        addStage(new ImportModuleStage());
    }

    //--- Document Pre-Processing ----------------------------------------------
    private class DocumentPreProcessingStage extends AbstractImporterStage {
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
    
    
//    // Order is important.
//    private final IDocumentProcessingStep[] steps = new IDocumentProcessingStep[] {
//        new DocumentMetadataFetcherStage(),
//        new FileMetadataChecksumStage(),
//        new DocumentFetchStage(),
//        new ImportModuleStep(),
//        new FileDocumentChecksumStep(),
//        new DocumentCommitStep()
//    };

//    /*default*/ DocumentProcessor(
//            FilesystemCrawlerConfig config, 
//            IReferenceStore<BaseCrawlData> database, File outputFile,
//            FileDocument doc, BaseCrawlData crawlURL) {
//        this.database = database;
//        this.doc = doc;
//        this.baseCrawlData = crawlURL;
//        this.config = config;
////        this.hdFetcher = config.getHttpHeadersFetcher();
//        this.outputFile = outputFile; 
//    }

//    public boolean processURL() {
//        for (int i = 0; i < steps.length; i++) {
//            IDocumentProcessingStep step = steps[i];
//            if (!step.processDocument()) {
//                return false;
//            }
//        }
//        return true;
//    }

//    public interface IDocumentProcessingStep {
//        // Returns true to continue to next step
//        // Returns false to abort, this URL is rejected.
//        boolean processDocument();
//    }
//
//    private FileImporterPipelineContext cast(ImporterPipelineContext ctx) {
//        return (FileImporterPipelineContext) ctx;
//    }
    
    //--- IMPORT Module --------------------------------------------------------
    private class DocumentMetadataFetcherStage extends AbstractImporterStage {
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
    
//    //--- IMPORT Module --------------------------------------------------------
//    private class ImportModuleStep implements IDocumentProcessingStep {
//        @Override
//        public boolean processDocument() {
//            Importer importer = new Importer(config.getImporterConfig());
//            try {
//                FileUtil.createDirsForFile(outputFile);
//                if (importer.importDocument(
//                        doc.getLocalFile(),
//                        null,
//                        outputFile,
//                        doc.getMetadata(),
//                        baseCrawlData.getReference())) {
//                    if (LOG.isDebugEnabled()) {
//                        LOG.debug("ACCEPTED document import. File="
//                                + doc.getReference());
//                    }
//                    return true;
//                }
//            } catch (IOException e) {
//                throw new FilesystemCollectorException(
//                        "Cannot import File: " + baseCrawlData.getReference(), e);
//            }
//            baseCrawlData.setStatus(FileCrawlState.REJECTED);
//            return false;
//        }
//    }    

    //--- HTTP Document Checksum -----------------------------------------------
    private class FileMetadataChecksumStage extends AbstractImporterStage {
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
    private class DocumentFetchStage extends AbstractImporterStage {
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
