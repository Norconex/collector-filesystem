/* Copyright 2013-2014 Norconex Inc.
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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;

import com.norconex.collector.core.crawler.AbstractCrawler;
import com.norconex.collector.core.crawler.ICrawler;
import com.norconex.collector.core.data.BaseCrawlData;
import com.norconex.collector.core.data.ICrawlData;
import com.norconex.collector.core.data.store.ICrawlDataStore;
import com.norconex.collector.core.pipeline.BasePipelineContext;
import com.norconex.collector.fs.FilesystemCollectorException;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.pipeline.committer.FileCommitterPipeline;
import com.norconex.collector.fs.pipeline.committer.FileCommitterPipelineContext;
import com.norconex.collector.fs.pipeline.importer.FileImporterPipeline;
import com.norconex.collector.fs.pipeline.importer.FileImporterPipelineContext;
import com.norconex.collector.fs.pipeline.queue.FileQueuePipeline;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.response.ImporterResponse;
import com.norconex.jef4.status.JobStatusUpdater;
import com.norconex.jef4.suite.JobSuite;

/**
 * The Filesystem Crawler.
 * 
 * @author Pascal Dimassimo
 */
public class FilesystemCrawler extends AbstractCrawler {

//    private static final Logger LOG = LogManager
//            .getLogger(FilesystemCrawler.class);

//    private static final int FILE_DELETE_COUNT_MAX = 100;

    private FileSystemManager fileManager;
//    private int deletedDownloadCount;

    /**
     * Constructor.
     * @param crawlerConfig HTTP crawler configuration
     */
    public FilesystemCrawler(FilesystemCrawlerConfig crawlerConfig) {
        super(crawlerConfig);
    }
    
    @Override
    public FilesystemCrawlerConfig getCrawlerConfig() {
        return (FilesystemCrawlerConfig) super.getCrawlerConfig();
    }
    
    /**
     * @return the fileManager
     */
    public FileSystemManager getFileManager() {
        return fileManager;
    }
    
    @Override
    protected void prepareExecution(
            JobStatusUpdater statusUpdater, JobSuite suite, 
            ICrawlDataStore crawlDataStore, boolean resume) {

        try {
            this.fileManager = VFS.getManager();
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
        
        if (!resume) {
            String[] startPaths = getCrawlerConfig().getStartPaths();
            for (int i = 0; i < startPaths.length; i++) {
                String startPath = startPaths[i];
                executeQueuePipeline(
                        new BaseCrawlData(startPath), crawlDataStore);
            }
        }
    }

    @Override
    protected void executeQueuePipeline(
            ICrawlData crawlData, ICrawlDataStore crawlDataStore) {
        BaseCrawlData fsData = (BaseCrawlData) crawlData;
        BasePipelineContext context = 
                new BasePipelineContext(this, crawlDataStore, fsData);
        new FileQueuePipeline().execute(context);
    }

    @Override
    protected ImporterDocument wrapDocument(ICrawlData crawlData,
            ImporterDocument document) {
        return new FileDocument(document);
        //TODO add file metadata from FileObject???
    }

    @Override
    protected ImporterResponse executeImporterPipeline(
            ICrawler crawler, ImporterDocument doc,
            ICrawlDataStore crawlDataStore, BaseCrawlData crawlData) {
        
        //TODO create pipeline context prototype
        //TODO cache the pipeline object?
        FileObject fileObject = null;
        try {
            fileObject = fileManager.resolveFile(crawlData.getReference());
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(
                    "Cannot resolve: " + crawlData.getReference(), e);
        }
        FileImporterPipelineContext context = new FileImporterPipelineContext(
                (FilesystemCrawler) crawler, crawlDataStore, (FileDocument) doc,
                (BaseCrawlData) crawlData, fileObject);
        new FileImporterPipeline(
                getCrawlerConfig().isKeepDownloads()).execute(context);
        ImporterResponse response = context.getImporterResponse();
        return response;        
    }

    @Override
    protected BaseCrawlData createEmbeddedCrawlData(String embeddedReference,
            ICrawlData parentCrawlData) {
        return new BaseCrawlData(embeddedReference);
    }
    

    @Override
    protected void executeCommitterPipeline(ICrawler crawler,
            ImporterDocument doc, ICrawlDataStore crawlDataStore,
            BaseCrawlData crawlData) {
        
        FileCommitterPipelineContext context = new FileCommitterPipelineContext(
                (FilesystemCrawler) crawler, crawlDataStore, (FileDocument) doc, 
                (BaseCrawlData) crawlData);
        new FileCommitterPipeline().execute(context);
    }
    
    @Override
    protected void markReferenceVariationsAsProcessed(BaseCrawlData crawlData,
            ICrawlDataStore crawlDataStore) {
        // Nothing to do (does not support variations).
    }
    
    @Override
    protected void cleanupExecution(JobStatusUpdater statusUpdater,
            JobSuite suite, ICrawlDataStore refStore) {
        //Nothing to clean-up
    }



    // Make this one a Core pipeline stage, burrowed from HTTP Collector
//    private void deleteDownloadDirectory() {
//        if (!crawlerConfig.isKeepDownloads()) {
//            if (LOG.isInfoEnabled()) {
//                LOG.info("Deleting crawler downloads directory: "
//                        + getCrawlerDownloadDir());
//            }
//            try {
//                FileUtil.delete(getCrawlerDownloadDir());
//            } catch (IOException e) {
//                LOG.error(getId() 
//                        + ": Could not delete the crawler downloads directory: "
//                                + getCrawlerDownloadDir(), e);
//            }
//        } else {
//            LOG.debug(getId() + ": Removing empty directories");
//            FileUtil.deleteEmptyDirs(getCrawlerDownloadDir());
//        }
//    }
    
    
//    private void processFiles(
//            final ICrawlDataStore refStore,
//            final JobStatusUpdater statusUpdater, 
//            final JobSuite suite,
//            final boolean delete) {
//
//        int numThreads = getCrawlerConfig().getNumThreads();
//        final CountDownLatch latch = new CountDownLatch(numThreads);
//        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
//
//        for (int i = 0; i < numThreads; i++) {
//            final int threadIndex = i + 1;
//            LOG.debug(getId() 
//                    + ": Crawler thread #" + threadIndex + " started.");
//            pool.execute(new ProcessFilesRunnable(
//                    suite, statusUpdater, refStore, delete, latch));
//        }
//
//        try {
//            latch.await();
//            pool.shutdown();
//        } catch (InterruptedException e) {
//            throw new FilesystemCollectorException(e);
//        }
//    }
//    
//    /**
//     * @return <code>true</code> if more files to process
//     */
//    private boolean processNextFile(
//            final ICrawlDataStore crawlDataStore,
//            final JobStatusUpdater statusUpdater, 
//            final boolean delete) {
//        BaseCrawlData queuedFile = database.nextQueued();
//        if (LOG.isDebugEnabled()) {
//            LOG.debug(getId() 
//                    + " Processing next file from Queue: " + queuedFile);
//        }
//        if (queuedFile != null) {
//            if (isMaxFiles()) {
//                LOG.info(getId() + ": Maximum Files reached: " 
//                        + crawlerConfig.getMaxFiles());
//                return false;
//            }
//            StopWatch watch = new StopWatch();
//            watch.start();
//            int preOKCount = filesCount;
//            processNextQueuedFile(queuedFile, database, delete);
//            if (preOKCount != filesCount) {
//                progress.setMetadata(Integer.toString(filesCount));
//            }
//            setProgress(progress, database);
//            watch.stop();
//            if (LOG.isDebugEnabled()) {
//                LOG.debug(getId() + ": " + watch.toString() 
//                        + " to process: " + queuedFile.getReference());
//            }
//        } else {
//            int activeCount = database.getActiveCount();
//            boolean queueEmpty = database.isQueueEmpty();
//            if (LOG.isDebugEnabled()) {
//                LOG.debug(getId() 
//                        + " File currently being processed: " + activeCount);
//                LOG.debug(getId() + " Is File queue empty? " + queueEmpty);
//            }
//            if (activeCount == 0 && queueEmpty) {
//                return false;
//            }
////            Sleeper.sleepMillis(MINIMUM_DELAY);
//            return false;
//        }
//        return true;
//    }

//    private void setProgress(
//            JobProgress progress, IReferenceStore<BaseCrawlData> db) {
//        int queued = db.getQueueSize();
//        int processed = db.getProcessedCount();
//        int total = queued + processed;
//        if (total == 0) {
//            progress.setProgress(progress.getJobContext().getProgressMaximum());
//        } else {
//            progress.setProgress(BigDecimal.valueOf(processed)
//                    .divide(BigDecimal.valueOf(total), RoundingMode.DOWN)
//                    .multiply(BigDecimal.valueOf(IJobContext.PROGRESS_100))
//                    .intValue());
//        }
//        progress.setNote(
//                NumberFormat.getIntegerInstance().format(processed)
//                + " urls processed out of "
//                + NumberFormat.getIntegerInstance().format(total));
//    }
//    
//    private void deleteFile(
//            BaseCrawlData basicCrawlData, File outputFile, FileDocument doc) {
//        LOG.debug(getId() + ": Deleting File: " + basicCrawlData.getReference());
//        ICommitter committer = crawlerConfig.getCommitter();
//        basicCrawlData.setStatus(FileCrawlState.DELETED);
//        if (committer != null) {
//            committer.queueRemove(
//                    basicCrawlData.getReference(), outputFile, doc.getMetadata());
//        }
//    }
//    
//    private void processNextQueuedFile(BaseCrawlData basicCrawlData, 
//            IReferenceStore<BaseCrawlData> database, boolean delete) {
//        
//        if (basicCrawlData.isFile()) {
//            String url = basicCrawlData.getReference();
//            File outputFile = createLocalFile(basicCrawlData, ".txt");
//            FileDocument doc = new FileDocument(
//                    basicCrawlData.getReference(), createLocalFile(basicCrawlData, ".raw"));
//            setFileMetadata(doc.getMetadata(), basicCrawlData);
//            
//            try {
//                if (delete) {
//                    deleteFile(basicCrawlData, outputFile, doc);
//                    return;
//                } else if (LOG.isDebugEnabled()) {
//                    LOG.debug(getId() + ": Processing File: " + url);
//                }
//                if (!new DocumentProcessor(crawlerConfig, database, 
//                        outputFile, doc, basicCrawlData).processURL()) {
//                    return;
//                }
//            } catch (Exception e) {
//                //TODO Catch too broad? In case we want special treatment to 
//                // the class?
//                basicCrawlData.setStatus(FileCrawlState.ERROR);
//                LOG.error(getId() + ": Could not process document: " + url
//                        + " (" + e.getMessage() + ")", e);
//            } finally {
//                finalizeFileProcessing(
//                        basicCrawlData, database, url, outputFile, doc);
//            }
////            importAndCommitFile(file);
//        } else if (basicCrawlData.isFolder()) {
//            try {
//                for (FileObject fileToQueue : basicCrawlData.listFiles()) {
//                    database.queue(new BaseCrawlData(fileToQueue));
//                }
//                database.processed(basicCrawlData);
//            } catch (FilesystemCollectorException e) {
//                LOG.warn("Skipping: could not list files under folder: \""
//                        + basicCrawlData.toString() + "\". " + e.getMessage());
//            }
//        } else {
//            LOG.warn("Skipping: object not a file or folder: "
//                    + basicCrawlData.toString());
//        }
//        
//    }
//
//    private void finalizeFileProcessing(BaseCrawlData basicCrawlData,
//            IReferenceStore<BaseCrawlData> database, String url, File outputFile,
//            FileDocument doc) {
//        //--- Flag URL for deletion --------------------------------------------
//        try {
//            ICommitter committer = crawlerConfig.getCommitter();
//            if (database.isVanished(basicCrawlData)) {
//                basicCrawlData.setStatus(FileCrawlState.DELETED);
//                if (committer != null) {
//                    committer.queueRemove(url, outputFile, doc.getMetadata());
//                }
//            }
//        } catch (Exception e) {
//            LOG.error(getId() + ": Could not flag URL for deletion: " + url
//                    + " (" + e.getMessage() + ")", e);
//        }
//        
//        //--- Mark URL as Processed --------------------------------------------
//        try {
//            if (basicCrawlData.getStatus() == FileCrawlState.OK) {
//                filesCount++;
//            }
//            database.processed(basicCrawlData);
////            markOriginalURLAsProcessed(crawlFile, database, doc);
//            if (basicCrawlData.getStatus() == null) {
//                LOG.warn("File status is unknown: " + basicCrawlData.getReference());
//                basicCrawlData.setStatus(FileCrawlState.BAD_STATUS);
//            }
//            basicCrawlData.getStatus().logInfo(basicCrawlData);
//        } catch (Exception e) {
//            LOG.error(getId() + ": Could not mark URL as processed: " + url
//                    + " (" + e.getMessage() + ")", e);
//        }
// 
//        try {
//            //--- Delete Local File Download -----------------------------------
//            if (!crawlerConfig.isKeepDownloads()) {
//                LOG.debug("Deleting " + doc.getLocalFile());
//                FileUtils.deleteQuietly(doc.getLocalFile());
//                FileUtils.deleteQuietly(outputFile);
//                deleteDownloadDirIfReady();
//            }
//        } catch (Exception e) {
//            LOG.error(getId() + ": Could not delete local file: "
//                    + doc.getLocalFile() + " (" + e.getMessage() + ")", e);
//        }
//    }
//
//    @Override
//    public void stop(IJobStatus progress, JobSuite suite) {
//        stopped = true;
//        LOG.info("Stopping the crawler \"" + progress.getJobId() +  "\".");
//    }
//    
//    private File getBaseDownloadDir() {
//        return new File(crawlerConfig.getWorkDir().getAbsolutePath()
//                + "/downloads");
//    }
//
//    private File getCrawlerDownloadDir() {
//        return new File(getBaseDownloadDir() + "/" + crawlerConfig.getId());
//    }
//    
//    private File createLocalFile(BaseCrawlData file, String extension) {
//        return new File(getCrawlerDownloadDir().getAbsolutePath() 
//                + SystemUtils.FILE_SEPARATOR 
//                + PathUtils.urlToPath(file.getURL().toString())
//                + extension);
//    }
//
//    private void deleteDownloadDirIfReady() {
//        deletedDownloadCount++;
//        if (deletedDownloadCount % FILE_DELETE_COUNT_MAX == 0) {
//            final long someTimeAgo = System.currentTimeMillis()
//                    - (DateUtils.MILLIS_PER_MINUTE * 2);
//            File dir = getCrawlerDownloadDir();
//            Date date = new Date(someTimeAgo);
//            int dirCount = FileUtil.deleteEmptyDirs(dir, date);
//            if (LOG.isDebugEnabled()) {
//                LOG.debug(getId() + ": Deleted " + dirCount
//                        + " empty directories under " + dir);
//            }
//        }
//    }
//
//
//    private FileObject getFileObject(String path) {
//        try {
//            return managerVfs.resolveFile(path);
//        } catch (FileSystemException e) {
//            Exception exception = e;
//            //Attempt to resolve as relative path using local filesystem
//            if (!path.contains(":")) {
//                try {
//                    return managerVfs.resolveFile(
//                            new File(".").getCanonicalFile(), path);
//                } catch (IOException e1) {
//                    // let original exception be thrown
//                }
//            }
//            throw new FilesystemCollectorException(exception);
//        }
//    }
//    
//    private boolean isMaxFiles() {
//        return crawlerConfig.getMaxFiles() > -1 
//                && filesCount >= crawlerConfig.getMaxFiles();
//    }
//
//    private void setFileMetadata(FileMetadata metadata, BaseCrawlData basicCrawlData) {
//        //TODO shall we store directory depth, last modified, owner, etc?
//    }
//    

}
