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
import com.norconex.collector.core.data.store.impl.mapdb.MapDBCrawlDataStoreFactory;
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

    private static final long serialVersionUID = 1395707385333823138L;
    private static final Logger LOG = 
            LogManager.getLogger(FilesystemCrawlerConfig.class);
    
    private String[] startPaths;
    
    private boolean keepDownloads;

    private IMetadataChecksummer metadataChecksummer = 
            new FileMetadataChecksummer();
    
    private IFileDocumentProcessor[] preImportProcessors;
    private IFileDocumentProcessor[] postImportProcessors;
//    private IFileFilter[] fileFilters;    
    
    public FilesystemCrawlerConfig() {
        super();
        setCrawlDataStoreFactory(new MapDBCrawlDataStoreFactory());
    }

    public String[] getStartPaths() {
        return startPaths;
    }
    public void setStartPaths(String[] startPaths) {
        this.startPaths = startPaths;
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
    

//    public IFileFilter[] getFileFilters() {
//        return fileFilters;
//    }
//    public void setFileFilters(IFileFilter[] referenceFilters) {
//        this.fileFilters = referenceFilters;
//    }
    public IFileDocumentProcessor[] getPreImportProcessors() {
        return preImportProcessors;
    }
    public void setPreImportProcessors(
            IFileDocumentProcessor[] filePostProcessors) {
        this.preImportProcessors = ArrayUtils.clone(filePostProcessors);
    }
    public IFileDocumentProcessor[] getPostImportProcessors() {
        return postImportProcessors;
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
