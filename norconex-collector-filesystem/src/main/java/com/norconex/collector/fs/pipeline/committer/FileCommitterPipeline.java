/**
 * 
 */
package com.norconex.collector.fs.pipeline.committer;

import com.norconex.collector.core.crawler.event.CrawlerEvent;
import com.norconex.collector.core.pipeline.DocumentPipelineContext;
import com.norconex.collector.core.pipeline.committer.CommitModuleStage;
import com.norconex.collector.core.pipeline.committer.DocumentChecksumStage;
import com.norconex.collector.fs.doc.IFileDocumentProcessor;
import com.norconex.commons.lang.pipeline.Pipeline;

/**
 * @author Pascal Essiembre
 *
 */
public class FileCommitterPipeline extends Pipeline<DocumentPipelineContext> {

    public FileCommitterPipeline() {
        addStage(new DocumentChecksumStage());
        addStage(new DocumentPostProcessingStage());
        addStage(new CommitModuleStage());
    }
    
    //--- Document Post-Processing ---------------------------------------------
    private class DocumentPostProcessingStage extends AbstractCommitterStage {
        @Override
        public boolean executeStage(FileCommitterPipelineContext ctx) {
            if (ctx.getConfig().getPostImportProcessors() != null) {
                for (IFileDocumentProcessor postProc :
                        ctx.getConfig().getPostImportProcessors()) {
                    postProc.processDocument(
                            ctx.getCrawler().getFileManager(),
                            ctx.getDocument());
                    ctx.getCrawler().fireCrawlerEvent(
                            CrawlerEvent.DOCUMENT_POSTIMPORTED, 
                            ctx.getCrawlData(), postProc);
                }            
            }
            return true;
        }
    }  
    
//    //--- HTTP Document Checksum -----------------------------------------------
//    private class FileDocumentChecksumStep implements IDocumentProcessingStep {
//        @Override
//        public boolean processDocument() {
//            //TODO only if an INCREMENTAL run... else skip.
//            //TODO make checksum configurable
//
//            String newDocChecksum = null;
//            try {
//                FileInputStream is;
//                is = new FileInputStream(doc.getLocalFile());
//                newDocChecksum = DigestUtils.md5Hex(is);
//                is.close();
//            } catch (IOException e) {
//                throw new FilesystemCollectorException(
//                        "Cannot create document checksum: " 
//                                + doc.getReference(), e);
//            }
//            
//            
//            //TODO this is a temporary fix for the Content Analytics
//            //Committer until we move content checksum logic in the importer
//            //or provide an options to configure checksum target field
//            doc.getMetadata().setString("checksum", newDocChecksum);
//            
//            
//            doc.getMetadata().setString(
//                    FileMetadata.COLLECTOR_CHECKSUM_DOC, newDocChecksum);
//            baseCrawlData.setDocChecksum(newDocChecksum);
//            String oldDocChecksum = null;
//            BaseCrawlData cachedFile = database.getCached(baseCrawlData.getReference());
//            if (cachedFile != null) {
//                oldDocChecksum = cachedFile.getDocChecksum();
//            } else {
//                LOG.debug("ACCEPTED document checkum (new): File=" 
//                        + baseCrawlData.getReference());
//                return true;
//            }
//            if (StringUtils.isNotBlank(newDocChecksum) 
//                    && Objects.equals(newDocChecksum, oldDocChecksum)) {
//                if (LOG.isDebugEnabled()) {
//                    LOG.debug("REJECTED document checkum (unmodified): File=" 
//                            + baseCrawlData.getReference());
//                }
//                baseCrawlData.setStatus(FileCrawlState.UNMODIFIED);
//                return false;
//            }
//            LOG.debug("ACCEPTED document checkum (modified): File=" 
//                    + baseCrawlData.getReference());
//            return true;
//        }
//    }   
    
//    //--- Document Commit ------------------------------------------------------
//    private class DocumentCommitStep implements IDocumentProcessingStep {
//        @Override
//        public boolean processDocument() {
//            ICommitter committer = config.getCommitter();
//            if (committer != null) {
//                committer.queueAdd(baseCrawlData.getReference(), 
//                        outputFile, doc.getMetadata());
//            }
//            return true;
//        }
//    }  

    
    
//    //--- Document Post-Processing ---------------------------------------------
//    private class DocumentPostProcessingStage extends DocStage {
//        @Override
//        public boolean execute(HttpCommitterPipelineContext ctx) {
//            if (ctx.getConfig().getPostImportProcessors() != null) {
//                for (IHttpDocumentProcessor postProc :
//                        ctx.getConfig().getPostImportProcessors()) {
//                    postProc.processDocument(
//                            ctx.getHttpClient(), ctx.getDocument());
//                    
//                    ctx.getCrawler().fireCrawlerEvent(
//                            HttpCrawlerEvent.DOCUMENT_POSTIMPORTED, 
//                            ctx.getDocCrawl(), postProc);
//                }            
//            }
//            return true;
//        }
//    }  
//    
//    //--- Document Commit ------------------------------------------------------
//    private class CommitModuleStage extends DocStage {
//        @Override
//        public boolean execute(HttpCommitterPipelineContext ctx) {
//            ICommitter committer = ctx.getConfig().getCommitter();
//            if (committer != null) {
//                HttpDocument doc = ctx.getDocument();
//                committer.add(doc.getReference(), 
//                        doc.getContent(), doc.getMetadata());
//            }
//            ctx.getCrawler().fireCrawlerEvent(
//                    HttpCrawlerEvent.DOCUMENT_COMMITTED, 
//                    ctx.getDocCrawl(), committer);
//            return true;
//        }
//    }  

}
