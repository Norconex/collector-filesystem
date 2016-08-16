/* Copyright 2013-2016 Norconex Inc.
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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.checksum.IMetadataChecksummer;
import com.norconex.collector.core.crawler.AbstractCrawlerConfig;
import com.norconex.collector.fs.checksum.impl.FileMetadataChecksummer;
import com.norconex.collector.fs.doc.IFileDocumentProcessor;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;

/**
 * Filesystem Crawler configuration.
 * 
 * @author Pascal Dimassimo
 * @author Pascal Essiembre
 */
public class FilesystemCrawlerConfig extends AbstractCrawlerConfig {

    private static final Logger LOG = 
            LogManager.getLogger(FilesystemCrawlerConfig.class);
    
    private String[] startPaths;
    private String[] pathsFiles;
    
    private boolean keepDownloads;

    private IMetadataChecksummer metadataChecksummer = 
            new FileMetadataChecksummer();
    
    private IFileDocumentProcessor[] preImportProcessors;
    private IFileDocumentProcessor[] postImportProcessors;
    
    public FilesystemCrawlerConfig() {
        super();
    }

    public String[] getStartPaths() {
        return ArrayUtils.clone(startPaths);
    }
    public void setStartPaths(String[] startPaths) {
        this.startPaths = ArrayUtils.clone(startPaths);
    }
    public String[] getPathsFiles() {
        return ArrayUtils.clone(pathsFiles);
    }
    public void setPathsFiles(String[] pathsFiles) {
        this.pathsFiles = ArrayUtils.clone(pathsFiles);
    }
    public boolean isKeepDownloads() {
        return keepDownloads;
    }
    public void setKeepDownloads(boolean keepDownloads) {
        this.keepDownloads = keepDownloads;
    }

    public IMetadataChecksummer getMetadataChecksummer() {
        return metadataChecksummer;
    }
    public void setMetadataChecksummer(
            IMetadataChecksummer metadataChecksummer) {
        this.metadataChecksummer = metadataChecksummer;
    }
    
    public IFileDocumentProcessor[] getPreImportProcessors() {
        return ArrayUtils.clone(preImportProcessors);
    }
    public void setPreImportProcessors(
            IFileDocumentProcessor[] filePostProcessors) {
        this.preImportProcessors = ArrayUtils.clone(filePostProcessors);
    }
    public IFileDocumentProcessor[] getPostImportProcessors() {
        return ArrayUtils.clone(postImportProcessors);
    }
    public void setPostImportProcessors(
            IFileDocumentProcessor[] filePostProcessors) {
        this.postImportProcessors = ArrayUtils.clone(filePostProcessors);
    }

    @Override
    protected void saveCrawlerConfigToXML(Writer out) throws IOException {
        try {
            EnhancedXMLStreamWriter writer = new EnhancedXMLStreamWriter(out);
    
            writer.writeElementBoolean("keepDownloads", isKeepDownloads());
            writer.writeStartElement("startPaths");
            for (String path : getStartPaths()) {
                writer.writeStartElement("path");
                writer.writeCharacters(path);
                writer.writeEndElement();
            }
            for (String path : getPathsFiles()) {
                writer.writeStartElement("pathsFile");
                writer.writeCharacters(path);
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writeObject(out, "metadataChecksummer", getMetadataChecksummer());
            writeArray(out, "preImportProcessors", 
                    "processor", getPreImportProcessors());
            writeArray(out, "postImportProcessors", 
                    "processor", getPostImportProcessors());
        } catch (XMLStreamException e) {
            throw new IOException(
                    "Could not write to XML config: " + getId(), e);
        }
    }

    @Override
    protected void loadCrawlerConfigFromXML(XMLConfiguration xml)
            throws IOException {
        //--- Simple Settings --------------------------------------------------
        loadSimpleSettings(xml);
        
        //--- Metadata Checksummer -----------------------------------------
        setMetadataChecksummer(ConfigurationUtil.newInstance(xml,
                "metadataChecksummer", getMetadataChecksummer()));
        
        //--- HTTP Pre-Processors ----------------------------------------------
        IFileDocumentProcessor[] preProcFilters = loadProcessors(xml,
                "preImportProcessors.processor");
        setPreImportProcessors(defaultIfEmpty(preProcFilters,
                getPreImportProcessors()));

        //--- HTTP Post-Processors ---------------------------------------------
        IFileDocumentProcessor[] postProcFilters = loadProcessors(xml,
                "postImportProcessors.processor");
        setPostImportProcessors(defaultIfEmpty(postProcFilters,
                getPostImportProcessors()));
    }
    
    private void loadSimpleSettings(XMLConfiguration xml) {
        setKeepDownloads(xml.getBoolean("keepDownloads", isKeepDownloads()));

        String[] startPaths = xml.getStringArray("startPaths.path");
        setStartPaths(defaultIfEmpty(startPaths, getStartPaths()));
        
        String[] pathsFiles = xml.getStringArray("startPaths.pathsFile");
        setPathsFiles(defaultIfEmpty(pathsFiles, getPathsFiles()));
    }
    
    private IFileDocumentProcessor[] loadProcessors(XMLConfiguration xml,
            String xmlPath) {
        List<IFileDocumentProcessor> filters = new ArrayList<>();
        List<HierarchicalConfiguration> filterNodes = xml
                .configurationsAt(xmlPath);
        for (HierarchicalConfiguration filterNode : filterNodes) {
            IFileDocumentProcessor filter = ConfigurationUtil
                    .newInstance(filterNode);
            filters.add(filter);
            LOG.info("HTTP document processor loaded: " + filter);
        }
        return filters.toArray(new IFileDocumentProcessor[] {});
    }
    

}
