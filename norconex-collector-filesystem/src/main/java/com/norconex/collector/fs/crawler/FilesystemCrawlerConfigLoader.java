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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.fs.FilesystemCollectorException;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.ImporterConfigLoader;

/**
 * Filesystem Crawler configuration loader.
 * 
 * @author Pascal Dimassimo
 */
@SuppressWarnings("nls")
public final class FilesystemCrawlerConfigLoader {

    private static final Logger LOG = LogManager
            .getLogger(FilesystemCrawlerConfigLoader.class);

    private FilesystemCrawlerConfigLoader() {
        super();
    }

    public static FilesystemCrawlerConfig[] loadCrawlerConfigs(File configFile,
            File configVariables) {
        ConfigurationLoader configLoader = new ConfigurationLoader();
        XMLConfiguration xml = configLoader
                .loadXML(configFile, configVariables);
        return loadCrawlerConfigs(xml);
    }

    public static FilesystemCrawlerConfig[] loadCrawlerConfigs(
            HierarchicalConfiguration xml) {
        try {
            XMLConfiguration defaults = ConfigurationUtil.getXmlAt(xml,
                    "crawlerDefaults");
            FilesystemCrawlerConfig defaultConfig = new FilesystemCrawlerConfig();
            if (defaults != null) {
                loadCrawlerConfig(defaultConfig, defaults);
            }

            List<HierarchicalConfiguration> nodes = xml
                    .configurationsAt("crawlers.crawler");
            List<FilesystemCrawlerConfig> configs = new ArrayList<FilesystemCrawlerConfig>();
            for (HierarchicalConfiguration node : nodes) {
                FilesystemCrawlerConfig config = (FilesystemCrawlerConfig) defaultConfig
                        .clone();
                loadCrawlerConfig(config, new XMLConfiguration(node));
                configs.add(config);
            }
            return configs.toArray(new FilesystemCrawlerConfig[] {});
        } catch (Exception e) {
            throw new FilesystemCollectorException(
                    "Cannot load crawler configurations.", e);
        }
    }

    /**
     * Loads a crawler configuration, which can be either the default crawler or
     * real crawler configuration instances (keeping defaults).
     * 
     * @param config
     *            crawler configuration to populate/overwrite
     * @param node
     *            the node representing the crawler configuration.
     * @throws HttpCollectorException
     *             problem parsing crawler configuration
     */
    private static void loadCrawlerConfig(FilesystemCrawlerConfig config,
            XMLConfiguration node) {
        // --- General Configuration
        // --------------------------------------------
        if (node == null) {
            LOG.warn("Passing a null configuration for " + config.getId()
                    + ", skipping.");
            return;
        }
        boolean loadingDefaults = "crawlerDefaults".equalsIgnoreCase(node
                .getRootElementName());

        if (!loadingDefaults) {
            String crawlerId = node.getString("[@id]", null);
            if (StringUtils.isBlank(crawlerId)) {
                throw new FilesystemCollectorException(
                        "Crawler ID is missing in configuration.");
            }
            config.setId(crawlerId);
        }

        loadSimpleSettings(config, node);

        config.setCrawlFileDatabaseFactory(ConfigurationUtil.newInstance(node,
                "crawlFileDatabaseFactory",
                config.getCrawlFileDatabaseFactory()));

        // --- IMPORTER ------------------------
        XMLConfiguration importerNode = ConfigurationUtil.getXmlAt(node,
                "importer");
        ImporterConfig importerConfig = ImporterConfigLoader
                .loadImporterConfig(importerNode);
        config.setImporterConfig(ObjectUtils.defaultIfNull(importerConfig,
                config.getImporterConfig()));

        // --- Document Committers -----------------
        config.setCommitter(ConfigurationUtil.newInstance(node, "committer",
                config.getCommitter()));
    }

    private static void loadSimpleSettings(FilesystemCrawlerConfig config,
            XMLConfiguration node) {

        config.setWorkDir(new File(node.getString("workDir", config
                .getWorkDir().toString())));
        config.setMaxFiles(node.getInt("maxFiles", config.getMaxFiles()));
        config.setKeepDownloads(node.getBoolean(
                "keepDownloads", config.isKeepDownloads()));

        String[] startPaths = node.getStringArray("startPaths.path");
        config.setStartPaths(defaultIfEmpty(startPaths, config.getStartPaths()));
        
        config.setNumThreads(node.getInt("numThreads", config.getNumThreads()));
    }

    // TODO consider moving to Norconex Commons Lang
    private static <T> T[] defaultIfEmpty(T[] array, T[] defaultArray) {
        if (ArrayUtils.isEmpty(array)) {
            return defaultArray;
        }
        return array;
    }
}
