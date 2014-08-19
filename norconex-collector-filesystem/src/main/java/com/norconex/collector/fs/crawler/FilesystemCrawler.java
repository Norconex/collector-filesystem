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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.ref.store.IReferenceStore;
import com.norconex.collector.fs.FilesystemCollectorException;
import com.norconex.collector.fs.db.util.PathUtils;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.committer.ICommitter;
import com.norconex.commons.lang.file.FileUtil;
import com.norconex.jef.AbstractResumableJob;
import com.norconex.jef.IJobContext;
import com.norconex.jef.progress.IJobStatus;
import com.norconex.jef.progress.JobProgress;
import com.norconex.jef.suite.JobSuite;

/**
 * The Filesystem Crawler.
 * 
 * @author Pascal Dimassimo
 */
public class FilesystemCrawler extends AbstractResumableJob {

    private static final Logger LOG = LogManager
            .getLogger(FilesystemCrawler.class);

    private static final int FILE_DELETE_COUNT_MAX = 100;

    private final FileSystemManager managerVfs;

    private final FilesystemCrawlerConfig crawlerConfig;
    private boolean stopped;
    private int okFilesCount;
    private int deletedDownloadCount;

    /**
     * Constructor.
     * 
     * @param crawlerConfig
     *            HTTP crawler configuration
     */
    public FilesystemCrawler(FilesystemCrawlerConfig crawlerConfig) {
        super();
        try {
            this.managerVfs = VFS.getManager();
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
        this.crawlerConfig = crawlerConfig;
        crawlerConfig.getWorkDir().mkdirs();
    }

    @Override
    public IJobContext createJobContext() {
        return new IJobContext() {
            private static final long serialVersionUID = 2785476254478043557L;

            @Override
            public long getProgressMinimum() {
                return IJobContext.PROGRESS_ZERO;
            }

            @Override
            public long getProgressMaximum() {
                return IJobContext.PROGRESS_100;
            }

            @Override
            public String getDescription() {
                return "Norconex Filesystem Crawler";
            }
        };
    }

    @Override
    public String getId() {
        return crawlerConfig.getId();
    }

    /**
     * Whether the job was stopped.
     * 
     * @return <code>true</code> if stopped
     */
    public boolean isStopped() {
        return stopped;
    }

    @Override
    protected void resumeExecution(JobProgress progress, JobSuite suite) {
        IReferenceStore<CrawlFile> database = crawlerConfig
                .getCrawlFileDatabaseFactory().createCrawlURLDatabase(
                        crawlerConfig, true);
        okFilesCount = NumberUtils.toInt(progress.getMetadata());
        try {
            execute(database, progress, suite);
        } finally {
            database.close();
        }
    }

    @Override
    protected void startExecution(JobProgress progress, JobSuite suite) {
        IReferenceStore<CrawlFile> database = crawlerConfig
                .getCrawlFileDatabaseFactory().createCrawlURLDatabase(
                        crawlerConfig, false);

        String[] startPaths = crawlerConfig.getStartPaths();
        for (int i = 0; i < startPaths.length; i++) {
            String startPath = startPaths[i];
            new FileProcessor(crawlerConfig, database,
                    new CrawlFile(getFileObject(startPath))).processFile();
        }

        try {
            execute(database, progress, suite);
        } finally {
            database.close();
        }
    }

    private void execute(
            IReferenceStore<CrawlFile> database, JobProgress progress,
            JobSuite suite) {

        StopWatch watch = new StopWatch();
        watch.start();
        
        processFiles(database, progress, suite, false);

        if (!isStopped()) {
            handleOrphans(database, progress, suite);
        }
        
        ICommitter committer = crawlerConfig.getCommitter();
        if (committer != null) {
            LOG.info(getId() + ": Crawler \"" + getId() + "\" "
                    + (stopped ? "stopping" : "finishing")
                    + ": committing documents.");
            committer.commit();
        }

        watch.stop();
        LOG.info(getId() + ": "
                + database.getProcessedCount() + " URLs processed "
                + "in " + watch.toString() + " for \"" + getId() + "\".");

        deleteDownloadDirectory();
        
//        if (!stopped) {
//            HttpCrawlerEventFirer.fireCrawlerFinished(this);
//        }
        LOG.info(getId() + ": Crawler \"" + getId() + "\" "
                + (stopped ? "stopped." : "completed."));
    }

    private void deleteDownloadDirectory() {
        if (!crawlerConfig.isKeepDownloads()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Deleting crawler downloads directory: "
                        + getCrawlerDownloadDir());
            }
            try {
                FileUtil.delete(getCrawlerDownloadDir());
            } catch (IOException e) {
                LOG.error(getId() 
                        + ": Could not delete the crawler downloads directory: "
                                + getCrawlerDownloadDir(), e);
            }
        } else {
            LOG.debug(getId() + ": Removing empty directories");
            FileUtil.deleteEmptyDirs(getCrawlerDownloadDir());
        }
    }
    
    
    private void handleOrphans(IReferenceStore<CrawlFile> database,
            JobProgress progress, JobSuite suite) {
        if (crawlerConfig.isDeleteOrphans()) {
            LOG.info(getId() + ": Deleting orphan Files (if any)...");
            deleteCacheOrphans(database, progress, suite);
        } else {
            if (!isMaxFiles()) {
                LOG.info(getId() + ": Re-processing orphan Files (if any)...");
                reprocessCacheOrphans(database, progress, suite);
            }
            // In case any item remains after we are done re-processing:
            LOG.info(getId() + ": Deleting remaining orphan Files (if any)...");
            deleteCacheOrphans(database, progress, suite);
        }
    }
    
    private void reprocessCacheOrphans(IReferenceStore<CrawlFile> database, 
            JobProgress progress, JobSuite suite) {
        long count = 0;
        Iterator<CrawlFile> it = database.getCacheIterator();
        if (it != null) {
            while (it.hasNext()) {
                CrawlFile crawlFile = it.next();
                new FileProcessor(
                        crawlerConfig, database, crawlFile).processFile();
                count++;
            }
            processFiles(database, progress, suite, false);
        }
        LOG.info(getId() + ": Reprocessed " + count + " orphan Files...");
    }
    
    private void deleteCacheOrphans(
            IReferenceStore<CrawlFile> database, JobProgress progress, JobSuite suite) {
        long count = 0;
        Iterator<CrawlFile> it = database.getCacheIterator();
        if (it != null) {
            while (it.hasNext()) {
                database.queue(it.next());
                count++;
            }
            processFiles(database, progress, suite, true);
        }
        LOG.info(getId() + ": Deleted " + count + " orphan Files...");
    }
    
    private void processFiles(
            final IReferenceStore<CrawlFile> database,
            final JobProgress progress, 
            final JobSuite suite,
            final boolean delete) {

        int numThreads = crawlerConfig.getNumThreads();
        final CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i + 1;
            LOG.debug(getId() + ": Crawler thread #" + threadIndex
                    + " started.");
            pool.execute(new ProcessFilesRunnable(
                    suite, progress, database, delete, latch));
        }

        try {
            latch.await();
            pool.shutdown();
        } catch (InterruptedException e) {
            throw new FilesystemCollectorException(e);
        }
    }
    
    /**
     * @return <code>true</code> if more files to process
     */
    private boolean processNextFile(
            final IReferenceStore<CrawlFile> database,
            final JobProgress progress,
            final boolean delete) {
        CrawlFile queuedFile = database.nextQueued();
        if (LOG.isDebugEnabled()) {
            LOG.debug(getId() 
                    + " Processing next file from Queue: " + queuedFile);
        }
        if (queuedFile != null) {
            if (isMaxFiles()) {
                LOG.info(getId() + ": Maximum Files reached: " 
                        + crawlerConfig.getMaxFiles());
                return false;
            }
            StopWatch watch = new StopWatch();
            watch.start();
            int preOKCount = okFilesCount;
            processNextQueuedFile(queuedFile, database, delete);
            if (preOKCount != okFilesCount) {
                progress.setMetadata(Integer.toString(okFilesCount));
            }
            setProgress(progress, database);
            watch.stop();
            if (LOG.isDebugEnabled()) {
                LOG.debug(getId() + ": " + watch.toString() 
                        + " to process: " + queuedFile.getReference());
            }
        } else {
            int activeCount = database.getActiveCount();
            boolean queueEmpty = database.isQueueEmpty();
            if (LOG.isDebugEnabled()) {
                LOG.debug(getId() 
                        + " File currently being processed: " + activeCount);
                LOG.debug(getId() + " Is File queue empty? " + queueEmpty);
            }
            if (activeCount == 0 && queueEmpty) {
                return false;
            }
//            Sleeper.sleepMillis(MINIMUM_DELAY);
            return false;
        }
        return true;
    }

    private void setProgress(
            JobProgress progress, IReferenceStore<CrawlFile> db) {
        int queued = db.getQueueSize();
        int processed = db.getProcessedCount();
        int total = queued + processed;
        if (total == 0) {
            progress.setProgress(progress.getJobContext().getProgressMaximum());
        } else {
            progress.setProgress(BigDecimal.valueOf(processed)
                    .divide(BigDecimal.valueOf(total), RoundingMode.DOWN)
                    .multiply(BigDecimal.valueOf(IJobContext.PROGRESS_100))
                    .intValue());
        }
        progress.setNote(
                NumberFormat.getIntegerInstance().format(processed)
                + " urls processed out of "
                + NumberFormat.getIntegerInstance().format(total));
    }
    
    private void deleteFile(
            CrawlFile crawlFile, File outputFile, FileDocument doc) {
        LOG.debug(getId() + ": Deleting File: " + crawlFile.getReference());
        ICommitter committer = crawlerConfig.getCommitter();
        crawlFile.setStatus(CrawlStatus.DELETED);
        if (committer != null) {
            committer.queueRemove(
                    crawlFile.getReference(), outputFile, doc.getMetadata());
        }
    }
    
    private void processNextQueuedFile(CrawlFile crawlFile, 
            IReferenceStore<CrawlFile> database, boolean delete) {
        
        if (crawlFile.isFile()) {
            String url = crawlFile.getReference();
            File outputFile = createLocalFile(crawlFile, ".txt");
            FileDocument doc = new FileDocument(
                    crawlFile.getReference(), createLocalFile(crawlFile, ".raw"));
            setFileMetadata(doc.getMetadata(), crawlFile);
            
            try {
                if (delete) {
                    deleteFile(crawlFile, outputFile, doc);
                    return;
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug(getId() + ": Processing File: " + url);
                }
                if (!new DocumentProcessor(crawlerConfig, database, 
                        outputFile, doc, crawlFile).processURL()) {
                    return;
                }
            } catch (Exception e) {
                //TODO Catch too broad? In case we want special treatment to 
                // the class?
                crawlFile.setStatus(CrawlStatus.ERROR);
                LOG.error(getId() + ": Could not process document: " + url
                        + " (" + e.getMessage() + ")", e);
            } finally {
                finalizeFileProcessing(
                        crawlFile, database, url, outputFile, doc);
            }
//            importAndCommitFile(file);
        } else if (crawlFile.isFolder()) {
            try {
                for (FileObject fileToQueue : crawlFile.listFiles()) {
                    database.queue(new CrawlFile(fileToQueue));
                }
                database.processed(crawlFile);
            } catch (FilesystemCollectorException e) {
                LOG.warn("Skipping: could not list files under folder: \""
                        + crawlFile.toString() + "\". " + e.getMessage());
            }
        } else {
            LOG.warn("Skipping: object not a file or folder: "
                    + crawlFile.toString());
        }
        
    }

    private void finalizeFileProcessing(CrawlFile crawlFile,
            IReferenceStore<CrawlFile> database, String url, File outputFile,
            FileDocument doc) {
        //--- Flag URL for deletion --------------------------------------------
        try {
            ICommitter committer = crawlerConfig.getCommitter();
            if (database.isVanished(crawlFile)) {
                crawlFile.setStatus(CrawlStatus.DELETED);
                if (committer != null) {
                    committer.queueRemove(url, outputFile, doc.getMetadata());
                }
            }
        } catch (Exception e) {
            LOG.error(getId() + ": Could not flag URL for deletion: " + url
                    + " (" + e.getMessage() + ")", e);
        }
        
        //--- Mark URL as Processed --------------------------------------------
        try {
            if (crawlFile.getStatus() == CrawlStatus.OK) {
                okFilesCount++;
            }
            database.processed(crawlFile);
//            markOriginalURLAsProcessed(crawlFile, database, doc);
            if (crawlFile.getStatus() == null) {
                LOG.warn("File status is unknown: " + crawlFile.getReference());
                crawlFile.setStatus(CrawlStatus.BAD_STATUS);
            }
            crawlFile.getStatus().logInfo(crawlFile);
        } catch (Exception e) {
            LOG.error(getId() + ": Could not mark URL as processed: " + url
                    + " (" + e.getMessage() + ")", e);
        }
 
        try {
            //--- Delete Local File Download -----------------------------------
            if (!crawlerConfig.isKeepDownloads()) {
                LOG.debug("Deleting " + doc.getLocalFile());
                FileUtils.deleteQuietly(doc.getLocalFile());
                FileUtils.deleteQuietly(outputFile);
                deleteDownloadDirIfReady();
            }
        } catch (Exception e) {
            LOG.error(getId() + ": Could not delete local file: "
                    + doc.getLocalFile() + " (" + e.getMessage() + ")", e);
        }
    }

    @Override
    public void stop(IJobStatus progress, JobSuite suite) {
        stopped = true;
        LOG.info("Stopping the crawler \"" + progress.getJobId() +  "\".");
    }
    
    private File getBaseDownloadDir() {
        return new File(crawlerConfig.getWorkDir().getAbsolutePath()
                + "/downloads");
    }

    private File getCrawlerDownloadDir() {
        return new File(getBaseDownloadDir() + "/" + crawlerConfig.getId());
    }
    
    private File createLocalFile(CrawlFile file, String extension) {
        return new File(getCrawlerDownloadDir().getAbsolutePath() 
                + SystemUtils.FILE_SEPARATOR 
                + PathUtils.urlToPath(file.getURL().toString())
                + extension);
    }

    private void deleteDownloadDirIfReady() {
        deletedDownloadCount++;
        if (deletedDownloadCount % FILE_DELETE_COUNT_MAX == 0) {
            final long someTimeAgo = System.currentTimeMillis()
                    - (DateUtils.MILLIS_PER_MINUTE * 2);
            File dir = getCrawlerDownloadDir();
            Date date = new Date(someTimeAgo);
            int dirCount = FileUtil.deleteEmptyDirs(dir, date);
            if (LOG.isDebugEnabled()) {
                LOG.debug(getId() + ": Deleted " + dirCount
                        + " empty directories under " + dir);
            }
        }
    }

    /*default*/ FilesystemCrawlerConfig getCrawlerConfig() {
        return crawlerConfig;
    }
    
    private FileObject getFileObject(String path) {
        try {
            return managerVfs.resolveFile(path);
        } catch (FileSystemException e) {
            Exception exception = e;
            //Attempt to resolve as relative path using local filesystem
            if (!path.contains(":")) {
                try {
                    return managerVfs.resolveFile(
                            new File(".").getCanonicalFile(), path);
                } catch (IOException e1) {
                    // let original exception be thrown
                }
            }
            throw new FilesystemCollectorException(exception);
        }
    }
    
    private boolean isMaxFiles() {
        return crawlerConfig.getMaxFiles() > -1 
                && okFilesCount >= crawlerConfig.getMaxFiles();
    }

    private void setFileMetadata(FileMetadata metadata, CrawlFile crawlFile) {
        //TODO shall we store directory depth, last modified, owner, etc?
    }
    
    private final class ProcessFilesRunnable implements Runnable {

        private final JobSuite suite;
        private final JobProgress progress;
        private final IReferenceStore<CrawlFile> database;
        private final boolean delete;
        private final CountDownLatch latch;

        private ProcessFilesRunnable(JobSuite suite, JobProgress progress,
                IReferenceStore<CrawlFile> database, boolean delete,
                CountDownLatch latch) {
            this.suite = suite;
            this.progress = progress;
            this.database = database;
            this.delete = delete;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                while (!isStopped()) {
                    try {
                        if (!processNextFile(database, progress, delete)) {
                            break;
                        }
                    } catch (Exception e) {
                        LOG.fatal(getId() + ": "
                                + "An error occured that could compromise "
                                + "the stability of the crawler. Stopping "
                                + "excution to avoid further issues...", e);
                        stop(progress, suite);
                    }
                }
            } catch (Exception e) {
                LOG.error(getId() + ": Problem in thread execution.", e);
            } finally {
                latch.countDown();
            }
        }

    }

}
