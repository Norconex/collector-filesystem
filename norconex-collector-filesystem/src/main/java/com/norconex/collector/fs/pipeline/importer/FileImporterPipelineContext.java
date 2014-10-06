/**
 * 
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
              