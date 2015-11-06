/* Copyright 2013-2015 Norconex Inc.
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
            FilesystemCrawler crawler, 
            ICrawlDataStore crawlDataStore,
            FileDocument doc, 
            BaseCrawlData crawlData,
            BaseCrawlData cachedCrawlData) {
        super(crawler, crawlDataStore, crawlData, cachedCrawlData, doc);
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
