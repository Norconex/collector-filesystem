/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Collector Core.
 * 
 * Norconex Collector Core is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Collector Core is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Collector Core. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.collector.core.ref.store;

import java.util.Iterator;

import com.norconex.collector.core.ref.IReference;

/**
 * Holds necessary information about all references (e.g. url, path, etc) 
 * crawling activities.  
 * The few stages a reference should have in most implementations are:</p>
 * <ul>
 *   <li><b>Queued:</b> URLs extracted from documents are first queued for 
 *       future processing.</li>
 *   <li><b>Active:</b> A URL is being processed.</li>
 *   <li><b>Processed:</b> A URL has been processed.  If the same URL is 
 *       encountered again during the same run, it will be ignored.</li>
 *   <li><b>Cached:</b> When crawling is over, processed URLs will be cached on 
 *       the next run.</li>
 * </ul>
 * @author Pascal Essiembre
 * @param <T> the reference type
 */
public interface IReferenceStore<T extends IReference> {

    /**
     * <p>
     * Queues a reference for future processing. 
     * @param reference  the reference to eventually be processed
     */
    void queue(T reference);

    /**
     * Whether there are any references to process in the queue.
     * @return <code>true</code> if the queue is empty
     */
    boolean isQueueEmpty();
    
    /**
     * Gets the size of the reference queue (number of 
     * references left to process).
     * @return queue size
     */
    int getQueueSize();

    /**
     * Whether the given reference is in the queue or not 
     * (waiting to be processed).
     * @param reference the reference 
     * @return <code>true</code> if the reference is in the queue
     */
    boolean isQueued(String reference);
    
    /**
     * Returns the next reference to be processed from the queue and marks it as 
     * being "active" (i.e. currently being processed).  The returned reference 
     * is effectively removed from the queue.
     * @return next reference 
     */
    T nextQueued();
    
    /**
     * Whether the given reference is currently being processed (i.e. active).
     * @param reference the reference
     * @return <code>true</code> if active
     */
    boolean isActive(String reference);

    /**
     * Gets the number of active references (currently being processed).
     * @return number of active references.
     */
    int getActiveCount();
    
    /**
     * Gets the cached reference from previous time crawler was run
     * (e.g. for comparison purposes).
     * @param cacheReference reference cached from previous run
     * @return url
     */
    T getCached(String cacheReference);
    
    /**
     * Whether there are any references the the cache from a previous crawler 
     * run.
     * @return <code>true</code> if the cache is empty
     */
    boolean isCacheEmpty();

    /**
     * Marks this reference as processed.  Processed references will not be 
     * processed again in the same crawl run.
     * @param reference processed reference
     */
    void processed(T reference);

    /**
     * Whether the given reference has been processed.
     * @param referenceId the reference id
     * @return <code>true</code> if processed
     */
    boolean isProcessed(String referenceId);

    /**
     * Gets the number of references processed.
     * @return number of references processed.
     */
    int getProcessedCount();

    /**
     * Gets the cache iterator.
     * @return cache iterator
     */
    Iterator<T> getCacheIterator();
    
    /**
     * Whether a reference has been deleted.  To find this out, the reference 
     * has to be of an invalid state (e.g. NOT_FOUND) and must exists in the 
     * reference cache in a valid state.
     * @param reference the reference
     * @return <code>true</code> if reference has been deleted on site
     */
    boolean isVanished(T reference);
    
    /**
     * Closes a database connection. This method gets called a the end
     * of a crawling job to give a change to close the underlying connection
     * properly, if applicable.
     */
    void close();
}
