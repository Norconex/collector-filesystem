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
package com.norconex.collector.fs.crawler;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.ref.store.IReferenceStore;
import com.norconex.collector.fs.filter.IFileFilter;
import com.norconex.importer.filter.IOnMatchFilter;
import com.norconex.importer.filter.OnMatch;

/**
 * Performs a URL handling logic before actual processing of the document
 * it represents takes place.  That is, before any 
 * document or document header download is
 * Instances are only valid for the scope of a single URL.  
 * @author Pascal Essiembre
 */
/*default*/ final class FileProcessor {

    private static final Logger LOG = 
            LogManager.getLogger(FileProcessor.class);
    
    private final FilesystemCrawlerConfig config;
    private final CrawlFile crawlFile;
    private final IReferenceStore<CrawlFile> database;
    private CrawlStatus status;
    
    // Order is important.  E.g. Robots must be after URL Filters and before 
    // Delay resolver
    private final IFileProcessingStep[] defaultSteps = new IFileProcessingStep[] {
        new URLFiltersStep(),
        new StoreNextURLStep(),
    };

    /*default*/ FileProcessor(FilesystemCrawlerConfig config,
            IReferenceStore<CrawlFile> database, CrawlFile crawlFile) {
        this.database = database;
        this.config = config;
        this.crawlFile = crawlFile;
    }

    public boolean processFile() {
        return processFile(defaultSteps);
    }

    public interface IFileProcessingStep {
        // Returns true to continue to next step
        // Returns false to abort, this URL is rejected.
        boolean processFile();
    }


    //--- URL Filters ----------------------------------------------------------
    private class URLFiltersStep implements IFileProcessingStep {
        @Override
        public boolean processFile() {
            if (isFileRejected(config.getFileFilters())) {
                status = CrawlStatus.REJECTED;
                return false;
            }
            return true;
        }
    }
    


    //--- Store Next URLs to process -------------------------------------------
    private class StoreNextURLStep implements IFileProcessingStep {
        @Override
        public boolean processFile() {
            String id = crawlFile.getReference();
            if (StringUtils.isBlank(id)) {
                return true;
            }
            if (database.isActive(id)) {
                debug("Already being processed: %s", id);
            } else if (database.isQueued(id)) {
                debug("Already queued: %s", id);
            } else if (database.isProcessed(id)) {
                debug("Already processed: %s", id);
            } else {
                database.queue(new CrawlFile(crawlFile.getFileObject()));
                debug("Queued for processing: %s", id);
            }
            return true;
        }
    }
    
    private static void debug(String message, Object... values) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format(message, values));
        }
    }    

    //=== Utility methods ======================================================
    private boolean isFileRejected(
            IFileFilter[] filters) {
        if (filters == null) {
            return false;
        }
        String type = "";
        boolean hasIncludes = false;
        boolean atLeastOneIncludeMatch = false;
        for (IFileFilter filter : filters) {
            boolean accepted = filter.acceptFile(crawlFile.getFileObject());
            
            // Deal with includes
            if (isIncludeFilter(filter)) {
                hasIncludes = true;
                if (accepted) {
                    atLeastOneIncludeMatch = true;
                }
                continue;
            }

            // Deal with exclude and non-OnMatch filters
            if (accepted) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("ACCEPTED document URL" + type 
                            + ". URL=" + crawlFile.getReference()
                            + " Filter=" + filter);
                }
            } else {
                return true;
            }
        }
        if (hasIncludes && !atLeastOneIncludeMatch) {
            return true;
        }
        return false;
    }
    
    private boolean processFile(IFileProcessingStep... steps) {
        try {
            for (int i = 0; i < steps.length; i++) {
                IFileProcessingStep step = steps[i];
                if (!step.processFile()) {
                    return false;
                }
            }
            status = CrawlStatus.OK;
            return true;
        } catch (Exception e) {
            //TODO do we really want to catch anything other than 
            // HTTPFetchException?  In case we want special treatment to the 
            // class?
            status = CrawlStatus.ERROR;
            LOG.error("Could not process file: " + crawlFile.getReference(), e);
            return false;
        } finally {
            //--- Mark URL as Processed ----------------------------------------
            if (status != CrawlStatus.OK) {
                crawlFile.setStatus(status);
                database.processed(crawlFile);
                status.logInfo(crawlFile);
            }
        }
    }

    private boolean isIncludeFilter(IFileFilter filter) {
        return filter instanceof IOnMatchFilter
                && OnMatch.INCLUDE == ((IOnMatchFilter) filter).getOnMatch();
    }
}

