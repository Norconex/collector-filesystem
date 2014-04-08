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
import java.io.Serializable;

import org.apache.commons.beanutils.BeanUtils;

import com.norconex.collector.fs.FilesystemCollectorException;
import com.norconex.collector.fs.db.ICrawlFileDatabaseFactory;
import com.norconex.collector.fs.db.impl.DefaultCrawlFileDatabaseFactory;
import com.norconex.committer.ICommitter;
import com.norconex.importer.ImporterConfig;

/**
 * Filesystem Crawler configuration.
 * 
 * @author Pascal Dimassimo
 */
public class FilesystemCrawlerConfig implements Cloneable, Serializable {

    private static final long serialVersionUID = 1395707385333823138L;

    private String id;
    private File workDir = new File("./work");
    private String[] startPaths;
    private int numThreads = 2;
    private int maxFiles = -1;
    
    private boolean keepDownloads;

    private ICrawlFileDatabaseFactory crawlFileDatabaseFactory = new DefaultCrawlFileDatabaseFactory();
    private ImporterConfig importerConfig = new ImporterConfig();
    private ICommitter committer;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public File getWorkDir() {
        return workDir;
    }

    public void setWorkDir(File workDir) {
        this.workDir = workDir;
    }

    public String[] getStartPaths() {
        return startPaths;
    }

    public void setStartPaths(String[] startPaths) {
        this.startPaths = startPaths;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public boolean isKeepDownloads() {
        return keepDownloads;
    }

    public void setKeepDownloads(boolean keepDownloads) {
        this.keepDownloads = keepDownloads;
    }

    public ICrawlFileDatabaseFactory getCrawlFileDatabaseFactory() {
        return crawlFileDatabaseFactory;
    }

    public void setCrawlFileDatabaseFactory(
            ICrawlFileDatabaseFactory crawlFileDatabaseFactory) {
        this.crawlFileDatabaseFactory = crawlFileDatabaseFactory;
    }

    public ImporterConfig getImporterConfig() {
        return importerConfig;
    }

    public void setImporterConfig(ImporterConfig importerConfig) {
        this.importerConfig = importerConfig;
    }

    public ICommitter getCommitter() {
        return committer;
    }

    public void setCommitter(ICommitter committer) {
        this.committer = committer;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            return (FilesystemCrawlerConfig) BeanUtils.cloneBean(this);
        } catch (Exception e) {
            throw new FilesystemCollectorException(
                    "Cannot clone crawler configuration.", e);
        }
    }
}
