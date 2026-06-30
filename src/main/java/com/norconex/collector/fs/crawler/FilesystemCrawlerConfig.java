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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.collector.core.checksum.IMetadataChecksummer;
import com.norconex.collector.core.crawler.CrawlerConfig;
import com.norconex.collector.fs.checksum.impl.FileMetadataChecksummer;
import com.norconex.collector.fs.doc.IFileDocumentProcessor;
import com.norconex.collector.fs.fetch.IFileDocumentFetcher;
import com.norconex.collector.fs.fetch.IFileMetadataFetcher;
import com.norconex.collector.fs.fetch.impl.GenericFileDocumentFetcher;
import com.norconex.collector.fs.fetch.impl.GenericFileMetadataFetcher;
import com.norconex.collector.fs.option.IFilesystemOptionsProvider;
import com.norconex.collector.fs.option.impl.GenericFilesystemOptionsProvider;
import com.norconex.commons.lang.xml.XML;

/**
 * Filesystem Crawler configuration.
 * 
 * @author Pascal Essiembre
 */
public class FilesystemCrawlerConfig extends CrawlerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemCrawlerConfig.class);

    private String[] startPaths;
    private String[] pathsFiles;
    private IStartPathsProvider[] startPathsProviders;

    private boolean keepDownloads;

    private IFilesystemOptionsProvider optionsProvider = new GenericFilesystemOptionsProvider();

    private IFileMetadataFetcher metadataFetcher = new GenericFileMetadataFetcher();

    private IMetadataChecksummer metadataChecksummer = new FileMetadataChecksummer();

    private IFileDocumentFetcher documentFetcher = new GenericFileDocumentFetcher();

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

    /**
     * Gets the providers of paths used as starting points for crawling.
     * Use this approach over other methods when paths need to be provided
     * dynamicaly at launch time. Paths obtained by a provider are combined
     * with start paths provided through other methods.
     * 
     * @return a start paths provider
     * @since 2.7.0
     */
    public IStartPathsProvider[] getStartPathsProviders() {
        return startPathsProviders;
    }

    /**
     * Sets the providers of paths used as starting points for crawling.
     * Use this approach over other methods when paths need to be provided
     * dynamicaly at launch time. Paths obtained by a provider are combined
     * with start paths provided through other methods.
     * 
     * @param startPathsProviders start paths provider
     * @since 2.7.0
     */
    public void setStartPathsProviders(
            IStartPathsProvider... startPathsProviders) {
        this.startPathsProviders = startPathsProviders;
    }

    public boolean isKeepDownloads() {
        return keepDownloads;
    }

    public void setKeepDownloads(boolean keepDownloads) {
        this.keepDownloads = keepDownloads;
    }

    /**
     * Gets the file system options provider. Default is
     * {@link GenericFilesystemOptionsProvider}.
     * 
     * @return file system options provider
     * @since 2.7.0
     */
    public IFilesystemOptionsProvider getOptionsProvider() {
        return optionsProvider;
    }

    /**
     * Sets the file system options provider. Cannot be <code>null</code>.
     * 
     * @param filesystemOptionsProvider file system options provider
     * @since 2.7.0
     */
    public void setOptionsProvider(
            IFilesystemOptionsProvider filesystemOptionsProvider) {
        this.optionsProvider = filesystemOptionsProvider;
    }

    /**
     * Gets the document metadata fetcher. Default is
     * {@link GenericFileMetadataFetcher}.
     * 
     * @return metadata fetcher
     * @since 2.7.0
     */
    public IFileMetadataFetcher getMetadataFetcher() {
        return metadataFetcher;
    }

    /**
     * Sets the document metadata fetcher. Cannot be <code>null</code>.
     * 
     * @param metadataFetcher metadata fetcher
     * @since 2.7.0
     */
    public void setMetadataFetcher(IFileMetadataFetcher metadataFetcher) {
        this.metadataFetcher = metadataFetcher;
    }

    public IMetadataChecksummer getMetadataChecksummer() {
        return metadataChecksummer;
    }

    public void setMetadataChecksummer(
            IMetadataChecksummer metadataChecksummer) {
        this.metadataChecksummer = metadataChecksummer;
    }

    /**
     * Gets the document fetcher. Default is
     * {@link GenericFileDocumentFetcher}.
     * 
     * @return document fetcher
     * @since 2.7.0
     */
    public IFileDocumentFetcher getDocumentFetcher() {
        return documentFetcher;
    }

    /**
     * Sets the document fetcher. Cannot be <code>null</code>.
     * 
     * @param documentFetcher document fetcher
     * @since 2.7.0
     */
    public void setDocumentFetcher(IFileDocumentFetcher documentFetcher) {
        this.documentFetcher = documentFetcher;
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
    protected void saveCrawlerConfigToXML(XML xml) {
        xml.addElement("keepDownloads", isKeepDownloads());

        var startPathsXML = xml.addElement("startPaths");
        startPathsXML.addElementList("path", toList(getStartPaths()));
        startPathsXML.addElementList("pathsFile", toList(getPathsFiles()));
        startPathsXML.addElementList(
                "provider", toList(getStartPathsProviders()));

        xml.addElement("optionsProvider", getOptionsProvider());
        xml.addElement("metadataFetcher", getMetadataFetcher());
        xml.addElement("metadataChecksummer", getMetadataChecksummer());
        xml.addElement("documentFetcher", getDocumentFetcher());
        xml.addElementList(
                "preImportProcessors", "processor",
                toList(getPreImportProcessors()));
        xml.addElementList(
                "postImportProcessors", "processor",
                toList(getPostImportProcessors()));
    }

    @Override
    protected void loadCrawlerConfigFromXML(XML xml) {
        setKeepDownloads(xml.getBoolean("keepDownloads", isKeepDownloads()));

        setStartPaths(xml.getStringList("startPaths/path", toList(startPaths))
                .toArray(String[]::new));
        setPathsFiles(xml.getStringList(
                "startPaths/pathsFile", toList(pathsFiles))
                .toArray(String[]::new));

        setStartPathsProviders(xml.getObjectListImpl(
                IStartPathsProvider.class,
                "startPaths/provider",
                toList(startPathsProviders))
                .toArray(IStartPathsProvider[]::new));

        setOptionsProvider(xml.getObjectImpl(IFilesystemOptionsProvider.class,
                "optionsProvider", optionsProvider));
        setMetadataFetcher(xml.getObjectImpl(IFileMetadataFetcher.class,
                "metadataFetcher", metadataFetcher));
        setMetadataChecksummer(xml.getObjectImpl(IMetadataChecksummer.class,
                "metadataChecksummer", metadataChecksummer));
        setDocumentFetcher(xml.getObjectImpl(IFileDocumentFetcher.class,
                "documentFetcher", documentFetcher));

        setPreImportProcessors(xml.getObjectListImpl(
                IFileDocumentProcessor.class,
                "preImportProcessors/processor",
                toList(preImportProcessors))
                .toArray(IFileDocumentProcessor[]::new));

        setPostImportProcessors(xml.getObjectListImpl(
                IFileDocumentProcessor.class,
                "postImportProcessors/processor",
                toList(postImportProcessors))
                .toArray(IFileDocumentProcessor[]::new));
    }

    private static <T> List<T> toList(T[] array) {
        return array == null ? Collections.emptyList() : Arrays.asList(array);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof FilesystemCrawlerConfig)) {
            return false;
        }
        FilesystemCrawlerConfig castOther = (FilesystemCrawlerConfig) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(keepDownloads, castOther.keepDownloads)
                .append(startPaths, castOther.startPaths)
                .append(pathsFiles, castOther.pathsFiles)
                .append(startPathsProviders, castOther.startPathsProviders)
                .append(optionsProvider, castOther.optionsProvider)
                .append(metadataFetcher, castOther.metadataFetcher)
                .append(metadataChecksummer, castOther.metadataChecksummer)
                .append(documentFetcher, castOther.documentFetcher)
                .append(preImportProcessors, castOther.preImportProcessors)
                .append(postImportProcessors, castOther.postImportProcessors)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(keepDownloads)
                .append(startPaths)
                .append(pathsFiles)
                .append(startPathsProviders)
                .append(optionsProvider)
                .append(metadataFetcher)
                .append(metadataChecksummer)
                .append(documentFetcher)
                .append(preImportProcessors)
                .append(postImportProcessors)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("keepDownloads", keepDownloads)
                .append("startPaths", startPaths)
                .append("pathsFiles", pathsFiles)
                .append("startPathsProviders", startPathsProviders)
                .append("optionsProvider", optionsProvider)
                .append("metadataFetcher", metadataFetcher)
                .append("metadataChecksummer", metadataChecksummer)
                .append("documentFetcher", documentFetcher)
                .append("preImportProcessors", preImportProcessors)
                .append("postImportProcessors", postImportProcessors)
                .toString();
    }
}
