/**
 * 
 */
package com.norconex.collector.fs.pipeline.committer;

import com.norconex.collector.core.data.BaseCrawlData;
import com.norconex.collector.core.data.store.ICrawlDataStore;
import com.norconex.collector.core.pipeline.DocumentPipelineContext;
import com.norconex.collector.fs.crawler.FilesystemCrawler;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.collector.fs.doc.FileDocument;

/**
 * @author Pascal Essiembre
 *
 */
public class FileCommitterPipelineContext extends DocumentPipelineContext {

    public FileCommitterPipelineContext(
            FilesystemCrawler crawler, ICrawlDataStore crawlDataStore,
            FileDocument doc, BaseCrawlData crawlData) {
        super(crawler, crawlDataStore, crawlData, doc);
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
}
