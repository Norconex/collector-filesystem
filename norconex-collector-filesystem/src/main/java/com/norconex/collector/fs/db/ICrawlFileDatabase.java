/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.collector.fs.db;

import java.io.File;
import java.util.Iterator;

import com.norconex.collector.fs.crawler.CrawlFile;


/**
 * <p>Database implementation holding necessary information about all URL 
 * crawling activities, what crawling stages URLs are in.  
 * The few stages a URL can have are:</p>
 * <ul>
 *   <li><b>Queued:</b> URLs extracted from documents are first queued for 
 *       future processing.</li>
 *   <li><b>Active:</b> A URL is being processed.</li>
 *   <li><b>Processed:</b> A URL has been processed.  If the same URL is 
 *       encountered again during the same run, it will be ignored.</li>
 *   <li><b>Cached:</b> When crawling is over, processed URLs will be cached on 
 *       the next run.</li>
 * </ul>
 *    
 * @author Pascal Dimassimo
 */
public interface ICrawlFileDatabase {

    /**
     * <p>
     * Queues a File for future processing. 
     * @param file the file to eventually be processed
     */
    void queue(CrawlFile file);

    /**
     * Whether there are any URLs to process in the queue.
     * @return <code>true</code> if the queue is empty
     */
    boolean isQueueEmpty();
    
    /**
     * Gets the size of the URL queue (number of URLs left to process).
     * @return queue size
     */
    int getQueueSize();

    /**
     * Whether the given URL is in the queue or not (waiting to be processed).
     * @param url url
     * @return <code>true</code> if the URL is in the queue
     */
    boolean isQueued(String filename);
    
    /**
     * Returns the next URL to be processed from the queue and marks it as 
     * being "active" (i.e. currently being processed).  The returned URL
     * is effectively removed from the queue.
     * @return next URL
     */
    CrawlFile nextQueued();
    
    /**
     * Whether the given URL is currently being processed (i.e. active).
     * @param url the url
     * @return <code>true</code> if active
     */
    boolean isActive(String filename);

    /**
     * Gets the number of active URLs (currently being processed).
     * @return number of active URLs.
     */
    int getActiveCount();
    
    /**
     * Gets the cached URL from previous time crawler was run
     * (e.g. for comparison purposes).
     * @param cacheURL URL cached from previous run
     * @return url
     */
    File getCached(String cacheURL);
    
    /**
     * Whether there are any URLs the the cache from a previous crawler 
     * run.
     * @return <code>true</code> if the cache is empty
     */
    boolean isCacheEmpty();

    /**
     * Marks this URL as processed.  Processed URLs will not be processed again
     * in the same crawl run.
     * @param crawlURL
     */
    void processed(CrawlFile file);

    /**
     * Whether the given URL has been processed.
     * @param url url
     * @return <code>true</code> if processed
     */
    boolean isProcessed(String filename);

    /**
     * Gets the number of URLs processed.
     * @return number of URLs processed.
     */
    int getProcessedCount();

    /**
     * Gets the cache iterator.
     * @return cache iterator
     */
    Iterator<CrawlFile> getCacheIterator();

    /**
     * Closes a database connection. This method gets called a the end
     * of a crawling job to give a change to close the underlying connection
     * properly, if applicable.
     */
    void close();
}
