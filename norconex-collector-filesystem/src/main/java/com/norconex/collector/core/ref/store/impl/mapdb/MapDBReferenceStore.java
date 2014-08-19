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
package com.norconex.collector.core.ref.store.impl.mapdb;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.norconex.collector.core.ref.IReference;
import com.norconex.collector.core.ref.store.IReferenceStore;

public abstract class MapDBReferenceStore<T extends IReference> 
        implements IReferenceStore<T> {

    private static final Logger LOG = 
            LogManager.getLogger(MapDBReferenceStore.class);

    //TODO make configurable
    private static final int COMMIT_SIZE = 1000;

    private static final String STORE_QUEUE = "queue";
    private static final String STORE_ACTIVE = "active";
    private static final String STORE_CACHE = "cache";
    private static final String STORE_PROCESSED_VALID = "valid";
    private static final String STORE_PROCESSED_INVALID = "invalid";
    private static final String STORE_SITEMAP = "sitemap";
    
    //private final String crawlerId; 
    private final String path;
    private final DB db;
    private Queue<T> queue;
    private Map<String, T> active;
    private Map<String, T> cache;
    private Map<String, T> processedValid;
    private Map<String, T> processedInvalid;
    private Set<String> sitemap;
    
    private long commitCounter;
    
    private final Serializer<T> valueSerializer;
    
    
    public MapDBReferenceStore(String path, boolean resume) {
        this(path, resume, null);
    }
    public MapDBReferenceStore(String path, boolean resume,
            Serializer<T> valueSerializer) {
        super();
        
        this.valueSerializer = valueSerializer;
        this.path = path;

        //this.crawlerId = crawlerId;
        
        LOG.info("Initializing reference store " + path);
        //String dbDir = path + "/store/" + crawlerId + "/";
        
        new File(path).mkdirs();
        File dbFile = new File(path + "/mapdb");
        boolean create = !dbFile.exists() || !dbFile.isFile();
        
        // Configure and open database
        this.db = createDB(dbFile);
    
        initDB(create);
        if (resume) {
            LOG.debug(path
                    + " Resuming: putting active URLs back in the queue...");
            for (T reference : active.values()) {
                queue.add(reference);
            }
            LOG.debug(path + ": Cleaning active database...");
            active.clear();
        } else if (!create) {
            LOG.debug(path + ": Cleaning queue database...");
            queue.clear();
            LOG.debug(path + ": Cleaning active database...");
            active.clear();
            LOG.debug(path + ": Cleaning invalid URLs database...");
            processedInvalid.clear();
            LOG.debug(path + ": Cleaning sitemap database...");
            sitemap.clear();
            LOG.debug(path + ": Cleaning cache database...");
            db.delete(STORE_CACHE);
            LOG.debug(path 
                    + ": Caching valid URLs from last run (if applicable)...");
            db.rename(STORE_PROCESSED_VALID, STORE_CACHE);
            cache = processedValid;
            processedValid = db.createHashMap(
                    STORE_PROCESSED_VALID).counterEnable().make();
            db.commit();
        } else {
            LOG.debug(path + ": New databases created.");
        }
        LOG.info(path + ": Done initializing databases.");
    }
    
    private DB createDB(File dbFile) {
        return DBMaker.newFileDB(dbFile)
                .asyncWriteEnable()
                .cacheSoftRefEnable()
                .closeOnJvmShutdown()
                .make();
    }
    
    
    private void initDB(boolean create) {
        queue = new MappedQueue<>(db, STORE_QUEUE, create);
        if (create) {
            active = db.createHashMap(STORE_ACTIVE)
                    .valueSerializer(valueSerializer).counterEnable().make();
            cache = db.createHashMap(STORE_CACHE)
                    .valueSerializer(valueSerializer).counterEnable().make();
            processedValid = db.createHashMap(
                    STORE_PROCESSED_VALID)
                    .valueSerializer(valueSerializer).counterEnable().make();
            processedInvalid = db.createHashMap(
                    STORE_PROCESSED_INVALID)
                    .valueSerializer(valueSerializer).counterEnable().make();
        } else {
            active = db.getHashMap(STORE_ACTIVE);
            cache = db.getHashMap(STORE_CACHE);
            processedValid = db.getHashMap(STORE_PROCESSED_VALID);
            processedInvalid = db.getHashMap(STORE_PROCESSED_INVALID);
        }
        sitemap = db.getHashSet(STORE_SITEMAP);
    }
    
    @Override
    public void queue(T reference) {
        // Short of being immutable, make a defensive copy of crawl URL.
        @SuppressWarnings("unchecked")
        T crawlUrlCopy = (T) reference.clone();
        queue.add(crawlUrlCopy);
    }

    @Override
    public boolean isQueueEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int getQueueSize() {
        return queue.size();
    }

    @Override
    public boolean isQueued(String url) {
        return queue.contains(url);
    }

    @Override
    public synchronized T nextQueued() {
        T reference = (T) queue.poll();
        if (reference != null) {
            active.put(reference.getReference(), reference);
        }
        return reference;
    }

    @Override
    public boolean isActive(String url) {
        return active.containsKey(url);
    }

    @Override
    public int getActiveCount() {
        return active.size();
    }

    @Override
    public T getCached(String cacheURL) {
        return cache.get(cacheURL);
    }

    @Override
    public boolean isCacheEmpty() {
        return cache.isEmpty();
    }

    @Override
    public synchronized void processed(T reference) {
        // Short of being immutable, make a defensive copy of crawl URL.
        @SuppressWarnings("unchecked")
        T referenceCopy = (T) reference.clone();
        if (isValid(referenceCopy)) {
//        if (isValidStatus(referenceCopy)) {
            processedValid.put(referenceCopy.getReference(), referenceCopy);
        } else {
            processedInvalid.put(referenceCopy.getReference(), referenceCopy);
        }
        if (!active.isEmpty()) {
            active.remove(referenceCopy.getReference());
        }
        if (!cache.isEmpty()) {
            cache.remove(referenceCopy.getReference());
        }
        commitCounter++;
        if (commitCounter % COMMIT_SIZE == 0) {
            LOG.debug("Committing reference store to disk: " + path);
            db.commit();
        }
        //TODO Compact database and LOG the event once MapDB fixed issue #160
        //TODO call db.compact(); when commit counter modulus commit size
        //     10 is encountered.
    }

    @Override
    public boolean isProcessed(String url) {
        return processedValid.containsKey(url)
                || processedInvalid.containsKey(url);
    }

    @Override
    public int getProcessedCount() {
        return processedValid.size() + processedInvalid.size();
    }

    public Iterator<T> getCacheIterator() {
        return cache.values().iterator();
    };
    
    @Override
    public boolean isVanished(T reference) {
        T cachedReference = getCached(reference.getReference());
        if (cachedReference == null) {
            return false;
        }
        return isVanished(reference, cachedReference);
//        ReferenceStatus cur = reference.getStatus();
//        ReferenceStatus last = cachedURL.getStatus();
//        return cur != ReferenceStatus.OK && cur != ReferenceStatus.UNMODIFIED
//              && (last == ReferenceStatus.OK ||  last == ReferenceStatus.UNMODIFIED);
    }

    protected abstract boolean isVanished(
            T currentReference, T cachedReference);
    
//    @Override
//    public void sitemapResolved(String urlRoot) {
//        sitemap.add(urlRoot);
//    }
//
//    @Override
//    public boolean isSitemapResolved(String urlRoot) {
//        return sitemap.contains(urlRoot);
//    }
    
    @Override
    public synchronized void close() {
        if (!db.isClosed()) {
            LOG.info("Closing reference store: " + path);
            db.commit();
            db.close();
        }
    }
    
    protected abstract boolean isValid(T reference);
//    private boolean isValidStatus(T reference) {
//        return reference.getStatus() == ReferenceStatus.OK
//                || reference.getStatus() == ReferenceStatus.UNMODIFIED;
//    }
    
    @Override
    protected void finalize() throws Throwable {
        if (!db.isClosed()) {
            LOG.info("Closing reference store: " + path);
            db.commit();
            db.close();
        }
        super.finalize();
    }
}
