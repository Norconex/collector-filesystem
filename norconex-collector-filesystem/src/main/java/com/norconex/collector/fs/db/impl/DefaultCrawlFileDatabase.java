package com.norconex.collector.fs.db.impl;

import java.io.File;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.norconex.collector.fs.crawler.CrawlFile;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.collector.fs.db.ICrawlFileDatabase;

public class DefaultCrawlFileDatabase implements ICrawlFileDatabase {

    private final Queue<CrawlFile> queue = new ConcurrentLinkedQueue<>();

    public DefaultCrawlFileDatabase(
            FilesystemCrawlerConfig config, boolean resume) {
    }

    @Override
    public void queue(CrawlFile file) {
        queue.add(file);
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
    public boolean isQueued(String filename) {
        return queue.contains(new File(filename));
    }

    @Override
    public CrawlFile nextQueued() {
        return queue.poll();
    }

    @Override
    public boolean isActive(String filename) {
        return false;
    }

    @Override
    public int getActiveCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public File getCached(String cacheURL) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCacheEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void processed(CrawlFile file) {
    }

    @Override
    public boolean isProcessed(String filename) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getProcessedCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Iterator<CrawlFile> getCacheIterator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

}
