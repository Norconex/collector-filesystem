/* Copyright 2013-2017 Norconex Inc.
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
package com.norconex.collector.fs.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.CollectorException;
import com.norconex.collector.core.crawler.AbstractCrawler;
import com.norconex.collector.core.crawler.ICrawler;
import com.norconex.collector.core.data.BaseCrawlData;
import com.norconex.collector.core.data.ICrawlData;
import com.norconex.collector.core.data.store.ICrawlDataStore;
import com.norconex.collector.core.pipeline.BasePipelineContext;
import com.norconex.collector.core.pipeline.importer.ImporterPipelineContext;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.option.IFilesystemOptionsProvider;
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
 * @author Pascal Essiembre
 */
public class FilesystemCrawler extends AbstractCrawler {

    private static final Logger LOG =
            LogManager.getLogger(FilesystemCrawler.class);

    private StandardFileSystemManager fileManager;
    private IFilesystemOptionsProvider optionsProvider;

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

        initializeFileSystemManager();

        if (!resume) {
            queueStartPaths(crawlDataStore);
        }
    }

    private void initializeFileSystemManager() {
        try {
            optionsProvider = getCrawlerConfig().getOptionsProvider();
            fileManager = new StandardFileSystemManager();
            fileManager.setClassLoader(getClass().getClassLoader());
//            if (getCrawlerConfig().getWorkDir() != null) {
//                fileManager.setTemporaryFileStore(new DefaultFileReplicator(
//                       new File(getCrawlerConfig().getWorkDir(), "fvs_cache")));
//            }
            fileManager.init();
        } catch (FileSystemException e) {
            throw new CollectorException("Could not initialize filesystem.", e);
        }
    }

    private void queueStartPaths(ICrawlDataStore crawlDataStore) {
        int urlCount = 0;
        urlCount += queueStartPathsRegular(crawlDataStore);
        urlCount += queueStartPathsSeedFiles(crawlDataStore);
        urlCount += queueStartPathsProviders(crawlDataStore);
        LOG.info(NumberFormat.getNumberInstance().format(urlCount)
                + " start paths identified.");
    }
    private int queueStartPathsRegular(final ICrawlDataStore crawlDataStore) {
        // Queue regular start urls
        String[] startPaths = getCrawlerConfig().getStartPaths();
        if (startPaths == null) {
            return 0;
        }

        for (int i = 0; i < startPaths.length; i++) {
            String startPath = startPaths[i];
            // No protocol specified: we assume local file, and we get
            // the absolute version.
            if (!startPath.contains("://")) {
                startPath = new File(startPath).getAbsolutePath();
            }
            executeQueuePipeline(new BaseCrawlData(startPath), crawlDataStore);
        }
        return startPaths.length;
    }
    private int queueStartPathsSeedFiles(final ICrawlDataStore crawlDataStore) {
        String[] pathsFiles = getCrawlerConfig().getPathsFiles();
        if (pathsFiles == null) {
            return 0;
        }
        int pathCount = 0;
        for (int i = 0; i < pathsFiles.length; i++) {
            String pathsFile = pathsFiles[i];
            LineIterator it = null;
            try (InputStream is = new FileInputStream(pathsFile)) {
                it = IOUtils.lineIterator(is, StandardCharsets.UTF_8);
                while (it.hasNext()) {
                    String startPath = it.nextLine();
                    executeQueuePipeline(
                            new BaseCrawlData(startPath), crawlDataStore);
                    pathCount++;
                }
            } catch (IOException e) {
                throw new CollectorException(
                        "Could not process paths file: " + pathsFile, e);
            } finally {
                LineIterator.closeQuietly(it);
            }
        }
        return pathCount;
    }

    private int queueStartPathsProviders(final ICrawlDataStore crawlDataStore) {
        IStartPathsProvider[] providers =
                getCrawlerConfig().getStartPathsProviders();
        if (providers == null) {
            return 0;
        }
        int count = 0;
        for (IStartPathsProvider provider : providers) {
            if (provider == null) {
                continue;
            }
            Iterator<String> it = provider.provideStartPaths();
            while (it.hasNext()) {
                executeQueuePipeline(
                        new BaseCrawlData(it.next()), crawlDataStore);
                count++;
            }
        }
        return count;
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
            ImporterPipelineContext importerContext) {

        ICrawlData crawlData = importerContext.getCrawlData();

        String ref = fixEncoding(crawlData.getReference());
        FileObject fileObject = null;
        try {
            if (optionsProvider == null) {
                fileObject = fileManager.resolveFile(ref);
            } else {
                fileObject = fileManager.resolveFile(ref,
                        optionsProvider.getFilesystemOptions(fileObject));
            }
        } catch (FileSystemException e) {
            resolveFileException(crawlData.getReference(), e);
        }
        FileImporterPipelineContext fileContext =
                new FileImporterPipelineContext(importerContext);
        fileContext.setFileObject(fileObject);
        new FileImporterPipeline(
                getCrawlerConfig().isKeepDownloads()).execute(fileContext);
        return fileContext.getImporterResponse();
    }

    // Trick-method to ensure reference can be converted to a valid URI if
    // a local path. Else it can fail (e.g. windows path having % in them).
    private String fixEncoding(String ref) {
        // We consider the reference a local file path (absolute or relative)
        // if it matches any of these conditions:
        //     - no scheme
        //     - scheme is "file"
        //     - scheme is one letter (e.g., windows drive letter)
        // If a local file, we properly encode all segments.
        String scheme = UriParser.extractScheme(ref);
        if (scheme == null
                || scheme.length() <= 1 || "file".equalsIgnoreCase(scheme)) {
            // encode segments
            StringBuilder b = new StringBuilder();
            Matcher m = Pattern.compile("([^\\/:]+|[\\/:]+)").matcher(ref);
            while (m.find()) {
                if (StringUtils.containsAny(m.group(), "\\/:")) {
                    b.append(m.group());
                } else {
                    b.append(uriEncode(m.group()));
                }
            }
            return b.toString();
        }
        return ref;
    }
    private String uriEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            //NOOP, return original value and hope for the best.
        }
        return value;
    }

    @Override
    protected BaseCrawlData createEmbeddedCrawlData(String embeddedReference,
            ICrawlData parentCrawlData) {
        return new BaseCrawlData(embeddedReference);
    }


    @Override
    protected void executeCommitterPipeline(ICrawler crawler,
            ImporterDocument doc, ICrawlDataStore crawlDataStore,
            BaseCrawlData crawlData, BaseCrawlData cachedCrawlData) {

        FileCommitterPipelineContext context = new FileCommitterPipelineContext(
                (FilesystemCrawler) crawler, crawlDataStore, (FileDocument) doc,
                crawlData, cachedCrawlData);
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
        fileManager.close();
    }

    private void resolveFileException(String ref, Exception e) {
        Throwable t = ExceptionUtils.getRootCause(e);
        if (t instanceof MalformedURLException) {
            if (StringUtils.containsIgnoreCase(t.getMessage(), "smb")) {
                LOG.error("SMB protocol requires you to have this library in "
                        + "your classpath (e.g. \"lib\" folder): "
                        + "http://central.maven.org/maven2/jcifs/jcifs/"
                        + "1.3.17/jcifs-1.3.17.jar");
            } else if (StringUtils.containsIgnoreCase(
                    t.getMessage(), "unknown protocol")) {
                LOG.error("The protocol used may be unsupported or requires "
                        + "you to install missing dependencies.");
            }
        }
        throw new CollectorException("Cannot resolve: " + ref, e);
    }
}
