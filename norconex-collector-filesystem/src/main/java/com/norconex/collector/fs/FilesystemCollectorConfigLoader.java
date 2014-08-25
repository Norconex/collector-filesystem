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

import java.io.File;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfigLoader;
import com.norconex.commons.lang.config.ConfigurationLoader;

/**
 * Filesystem Collector configuration loader.  Configuration options are defined
 * as part of general product documentation.
 * @author Pascal Dimassimo
 */
public final class FilesystemCollectorConfigLoader {

    private static final Logger LOG = LogManager.getLogger(
            FilesystemCollectorConfigLoader.class);
    
    private FilesystemCollectorConfigLoader() {
        super();
    }

    /**
     * Loads a collection configuration from file.
     * @param configFile configuration file
     * @param configVariables configuration variables file
     * @return HTTP Collector Configuration
     */
    public static FilesystemCollectorConfig loadCollectorConfig(
            File configFile, File configVariables) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading configuration file: " + configFile);
        }
        if (!configFile.exists()) {
            return null;
        }
        
        ConfigurationLoader configLoader = new ConfigurationLoader();
        XMLConfiguration xml = configLoader.loadXML(
                configFile, configVariables);

        String collectorID = xml.getString("[@id]");
        FilesystemCrawlerConfig[] crawlers = 
                FilesystemCrawlerConfigLoader.loadCrawlerConfigs(xml);

        FilesystemCollectorConfig config = new FilesystemCollectorConfig(collectorID);
        config.setCrawlerConfigs(crawlers);

        config.setLogsDir(xml.getString("logsDir", config.getLogsDir()));
        config.setProgressDir(
                xml.getString("progressDir", config.getProgressDir()));

        if (LOG.isInfoEnabled()) {
            LOG.info("Configuration loaded: id=" + collectorID
                    + "; logsDir=" + config.getLogsDir()
                    + "; progressDir=" + config.getProgressDir());
        }
        return config;
    }

    
}
