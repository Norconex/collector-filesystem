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
package com.norconex.collector.fs;

import java.io.Serializable;

import org.apache.commons.lang3.ArrayUtils;

import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;

/**
 * Filesystem Collector configuration.
 * @author Pascal Dimassimo
 */
public class FilesystemCollectorConfig implements Cloneable, Serializable {

    /** Default relative directory where logs from Log4j are stored. */
    public static final String DEFAULT_LOGS_DIR = "./logs";
    /** Default relative directory where progress files are stored. */
    public static final String DEFAULT_PROGRESS_DIR = "./progress";
    
    private static final long serialVersionUID = -3350877963428801802L;
    private String id;
    private FilesystemCrawlerConfig[] crawlerConfigs;
    private String progressDir = DEFAULT_PROGRESS_DIR;
    private String logsDir = DEFAULT_LOGS_DIR;
    
    /**
     * Creates a new collector with the given unique id.  It is important
     * the id of the collector is unique amongst your collectors.  This
     * facilitates integration with different systems and facilitates
     * tracking.
     * @param id unique identifier
     */
	public FilesystemCollectorConfig(String id) {
        super();
        this.id = id;
    }

	/**
	 * Gets this collector unique identifier.
	 * @return unique identifier
	 */
	public String getId() {
        return id;
    }
    /**
     * Sets this collector unique identifier.
     * @param id unique identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets all crawler configurations.
     * @return crawler configurations
     */
    public FilesystemCrawlerConfig[] getCrawlerConfigs() {
        return crawlerConfigs;
    }
    /**
     * Sets crawler configurations.
     * @param crawlerConfigs crawler configurations
     */
    public void setCrawlerConfigs(FilesystemCrawlerConfig[] crawlerConfigs) {
        this.crawlerConfigs = ArrayUtils.clone(crawlerConfigs);
    }

    /**
     * Gets the directory location where progress files (from JEF API)
     * are stored.
     * @return progress directory path
     */
    public String getProgressDir() {
        return progressDir;
    }
    /**
     * Sets the directory location where progress files (from JEF API)
     * are stored.
     * @param progressDir progress directory path
     */
    public void setProgressDir(String progressDir) {
        this.progressDir = progressDir;
    }

    /**
     * Gets the directory location of generated log files.
     * @return logs directory path
     */
    public String getLogsDir() {
        return logsDir;
    }
    /**
     * Sets the directory location of generated log files.
     * @param logsDir logs directory path
     */
    public void setLogsDir(String logsDir) {
        this.logsDir = logsDir;
    }
}
