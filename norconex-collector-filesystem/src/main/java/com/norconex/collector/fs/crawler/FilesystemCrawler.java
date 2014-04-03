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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.fs.FilesystemCollectorException;
import com.norconex.collector.fs.db.ICrawlFileDatabase;
import com.norconex.collector.fs.db.util.PathUtils;
import com.norconex.committer.ICommitter;
import com.norconex.commons.lang.io.FileUtil;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.Importer;
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
                return "Norconex HTTP Crawler";
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
        ICrawlFileDatabase database = crawlerConfig
                .getCrawlFileDatabaseFactory().createCrawlURLDatabase(
                        crawlerConfig, true);
        try {
            execute(database, progress, suite);
        } finally {
            database.close();
        }
    }

    @Override
    protected void startExecution(JobProgress progress, JobSuite suite) {
        ICrawlFileDatabase database = crawlerConfig
                .getCrawlFileDatabaseFactory().createCrawlURLDatabase(
                        crawlerConfig, false);

        String[] startPaths = crawlerConfig.getStartPaths();
        for (int i = 0; i < startPaths.length; i++) {
            String startPath = startPaths[i];
            database.queue(new CrawlFile(getFileObject(startPath)));
        }

        try {
            execute(database, progress, suite);
        } finally {
            database.close();
        }
    }

    @Override
    public void stop(IJobStatus arg0, JobSuite arg1) {
    }

    private void execute(ICrawlFileDatabase database, JobProgress progress,
            JobSuite suite) {
        processFiles(database, progress, suite);

        ICommitter committer = crawlerConfig.getCommitter();
        if (committer != null) {
            LOG.info(getId() + ": Crawler \"" + getId() + "\" "
                    + (stopped ? "stopping" : "finishing")
                    + ": committing documents.");
            committer.commit();
        }

        deleteDownloadDirectory();

        LOG.info(getId() + ": Crawler \"" + getId() + "\" "
                + (stopped ? "stopped." : "completed."));
    }

    private void processFiles(final ICrawlFileDatabase database,
            final JobProgress progress, final JobSuite suite) {

        int numThreads = crawlerConfig.getNumThreads();
        final CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i + 1;
            LOG.debug(getId() + ": Crawler thread #" + threadIndex
                    + " started.");
            pool.execute(new ProcessFilesRunnable(suite, progress, database,
                    latch));
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
    private boolean processNextFile(final ICrawlFileDatabase database,
            final JobProgress progress) {

        CrawlFile queuedFile = database.nextQueued();
        if (queuedFile != null) {
            processNextQueuedFile(queuedFile, database);
            return true;
        } else {
            LOG.info("No more file to process");
            return false;
        }
    }

    private void processNextQueuedFile(CrawlFile file,
            ICrawlFileDatabase database) {
        if (file.isFile()) {
            importAndCommitFile(file);
        } else {
            for (FileObject fileToQueue : file.listFiles()) {
                database.queue(new CrawlFile(fileToQueue));
            }
        }
    }

    /**
     * This is a temporary method to import and commit a file.
     * 
     * TODO use a list of steps.
     * 
     * @param file
     */
    private void importAndCommitFile(CrawlFile file) {
        LOG.info("IMPORT & COMMIT: " + file);

        // Download
        File localFile;
        Properties metadata;
        try {
            localFile = createLocalFile(file, ".raw");
            FileUtil.createDirsForFile(localFile);
            FileContent content = file.getContent();
            FileOutputStream fos = new FileOutputStream(localFile);
            IOUtils.copy(content.getInputStream(), fos);
            fos.close();

            metadata = new Properties();
            metadata.addLong("collector.fs.filesize", content.getSize());
            metadata.addLong("collector.fs.lastmodified",
                    content.getLastModifiedTime());
        
        } catch (IOException e) {
            throw new FilesystemCollectorException("Cannot download file: "
                    + file, e);
        }

        // Import
        File outputFile = createLocalFile(file, ".txt");
        Importer importer = new Importer(crawlerConfig.getImporterConfig());
        try {
            importer.importDocument(
                    localFile, null, outputFile, metadata, file.getURL().toString());
        } catch (IOException e) {
            throw new FilesystemCollectorException("Cannot import file: "
                    + file, e);
        }

        // Commit
        ICommitter committer = crawlerConfig.getCommitter();
        if (committer != null) {
            committer.queueAdd(file.toString(), outputFile, metadata);
        }

        if (!crawlerConfig.isKeepDownloads()) {
            LOG.debug("Deleting " + localFile);
            FileUtils.deleteQuietly(localFile);
            deleteDownloadDirIfReady();
        }
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

    private void deleteDownloadDirectory() {
        if (!crawlerConfig.isKeepDownloads()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Deleting crawler downloads directory: "
                        + getCrawlerDownloadDir());
            }
            try {
                FileUtil.deleteFile(getCrawlerDownloadDir());
            } catch (IOException e) {
                LOG.error(
                        getId()
                                + ": Could not delete the crawler downloads directory: "
                                + getCrawlerDownloadDir(), e);
            }
        }
    }

    private FileObject getFileObject(String path) {
        try {
            return managerVfs.resolveFile(path);
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }

    private final class ProcessFilesRunnable implements Runnable {

        private final JobSuite suite;
        private final JobProgress progress;
        private final ICrawlFileDatabase database;
        private final CountDownLatch latch;

        private ProcessFilesRunnable(JobSuite suite, JobProgress progress,
                ICrawlFileDatabase database, CountDownLatch latch) {
            this.suite = suite;
            this.progress = progress;
            this.database = database;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                while (!isStopped()) {
                    try {
                        if (!processNextFile(database, progress)) {
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
