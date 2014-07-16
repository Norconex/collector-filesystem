/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex HTTP Collector.
 * 
 * Norconex HTTP Collector is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex HTTP Collector is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex HTTP Collector. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.collector.fs.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileContentInfo;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.ref.store.IReferenceStore;
import com.norconex.collector.fs.FilesystemCollectorException;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.committer.ICommitter;
import com.norconex.commons.lang.file.FileUtil;
import com.norconex.importer.Importer;

/**
 * Performs the document processing.  
 * Instances are only valid for the scope of a single URL.
 * <p/>
 * As of 1.3.0, the header and document checksums are added to the document
 * metadata under the keys {@link HttpMetadata#CHECKSUM_HEADER} and 
 * {@link HttpMetadata#CHECKSUM_DOC}.
 * @author Pascal Essiembre
 */
/*default*/ final class DocumentProcessor {

    private static final Logger LOG = 
            LogManager.getLogger(DocumentProcessor.class);
    
    private final FilesystemCrawlerConfig config;
    private final CrawlFile crawlFile;
    private final FileDocument doc;
    private final IReferenceStore<CrawlFile> database;
    private final File outputFile;
    
    // Order is important.
    private final IDocumentProcessingStep[] steps = new IDocumentProcessingStep[] {
        new DocumentMetadataFetcherStep(),
        new FileMetadataChecksumStep(),
        new DocumentFetchStep(),
        new ImportModuleStep(),
        new FileDocumentChecksumStep(),
        new DocumentCommitStep()
    };

    /*default*/ DocumentProcessor(
            FilesystemCrawlerConfig config, 
            IReferenceStore<CrawlFile> database, File outputFile,
            FileDocument doc, CrawlFile crawlURL) {
        this.database = database;
        this.doc = doc;
        this.crawlFile = crawlURL;
        this.config = config;
//        this.hdFetcher = config.getHttpHeadersFetcher();
        this.outputFile = outputFile; 
    }

    public boolean processURL() {
        for (int i = 0; i < steps.length; i++) {
            IDocumentProcessingStep step = steps[i];
            if (!step.processDocument()) {
                return false;
            }
        }
        return true;
    }

    public interface IDocumentProcessingStep {
        // Returns true to continue to next step
        // Returns false to abort, this URL is rejected.
        boolean processDocument();
    }

    //--- IMPORT Module --------------------------------------------------------
    private class DocumentMetadataFetcherStep 
            implements IDocumentProcessingStep {
        @Override
        public boolean processDocument() {
            FileContent content = crawlFile.getContent();
            FileMetadata metadata = doc.getMetadata();
            try {
                metadata.addLong(FileMetadata.DOC_SIZE, content.getSize());
                metadata.addLong(FileMetadata.DOC_LASTMODIFIED,
                        content.getLastModifiedTime());
                FileContentInfo info = content.getContentInfo();
                if (info != null) {
                    metadata.addString(FileMetadata.DOC_CONTENT_ENCODING, 
                            info.getContentEncoding());
                    metadata.addString(FileMetadata.DOC_CONTENT_TYPE, 
                            info.getContentType());
                }
                for (String attrName: content.getAttributeNames()) {
                    Object obj = content.getAttribute(attrName);
                    if (obj != null) {
                        metadata.addString(FileMetadata.COLLECTOR_PREFIX 
                                + "attribute." + attrName, 
                                        Objects.toString(obj));
                    }
                }
            } catch (FileSystemException e) {
                throw new FilesystemCollectorException(
                        "Cannot fetch file metadata: " 
                                + crawlFile.getReference(), e);
            }
            return true;
        }
    }    
    
    //--- IMPORT Module --------------------------------------------------------
    private class ImportModuleStep implements IDocumentProcessingStep {
        @Override
        public boolean processDocument() {
            Importer importer = new Importer(config.getImporterConfig());
            try {
                FileUtil.createDirsForFile(outputFile);
                if (importer.importDocument(
                        doc.getLocalFile(),
                        null,
                        outputFile,
                        doc.getMetadata(),
                        crawlFile.getReference())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("ACCEPTED document import. File="
                                + doc.getReference());
                    }
                    return true;
                }
            } catch (IOException e) {
                throw new FilesystemCollectorException(
                        "Cannot import File: " + crawlFile.getReference(), e);
            }
            crawlFile.setStatus(CrawlStatus.REJECTED);
            return false;
        }
    }    

    //--- HTTP Document Checksum -----------------------------------------------
    private class FileMetadataChecksumStep implements IDocumentProcessingStep {
        @Override
        public boolean processDocument() {
            //TODO only if an INCREMENTAL run... else skip.
            //TODO make checksum configurable

            String newChecksum = doc.getMetadata().getString(
                    FileMetadata.DOC_LASTMODIFIED);
            
            crawlFile.setMetadataChecksum(newChecksum);
            
            doc.getMetadata().setString(
                    FileMetadata.CHECKSUM_METADATA, newChecksum);
            
            String oldChecksum = null;
            CrawlFile cachedFile = database.getCached(crawlFile.getReference());
            if (cachedFile != null) {
                oldChecksum = cachedFile.getMetadataChecksum();
            } else {
                LOG.debug("ACCEPTED metadata checkum (new): File=" 
                        + crawlFile.getReference());
                return true;
            }
            if (StringUtils.isNotBlank(newChecksum) 
                    && Objects.equals(newChecksum, oldChecksum)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("REJECTED metadata checkum (unmodified): File=" 
                            + crawlFile.getReference());
                }
                crawlFile.setStatus(CrawlStatus.UNMODIFIED);
                return false;
            }
            LOG.debug("ACCEPTED metadata checkum (modified): File=" 
                    + crawlFile.getReference());
            return true;
        }
    }   
    
    //--- HTTP Document Checksum -----------------------------------------------
    private class FileDocumentChecksumStep implements IDocumentProcessingStep {
        @Override
        public boolean processDocument() {
            //TODO only if an INCREMENTAL run... else skip.
            //TODO make checksum configurable

            String newDocChecksum = null;
            try {
                FileInputStream is;
                is = new FileInputStream(doc.getLocalFile());
                newDocChecksum = DigestUtils.md5Hex(is);
                is.close();
            } catch (IOException e) {
                throw new FilesystemCollectorException(
                        "Cannot create document checksum: " 
                                + doc.getReference(), e);
            }
            
            
            //TODO this is a temporary fix for the Content Analytics
            //Committer until we move content checksum logic in the importer
            //or provide an options to configure checksum target field
            doc.getMetadata().setString("checksum", newDocChecksum);
            
            
            doc.getMetadata().setString(
                    FileMetadata.CHECKSUM_DOC, newDocChecksum);
            crawlFile.setDocChecksum(newDocChecksum);
            String oldDocChecksum = null;
            CrawlFile cachedFile = database.getCached(crawlFile.getReference());
            if (cachedFile != null) {
                oldDocChecksum = cachedFile.getDocChecksum();
            } else {
                LOG.debug("ACCEPTED document checkum (new): File=" 
                        + crawlFile.getReference());
                return true;
            }
            if (StringUtils.isNotBlank(newDocChecksum) 
                    && Objects.equals(newDocChecksum, oldDocChecksum)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("REJECTED document checkum (unmodified): File=" 
                            + crawlFile.getReference());
                }
                crawlFile.setStatus(CrawlStatus.UNMODIFIED);
                return false;
            }
            LOG.debug("ACCEPTED document checkum (modified): File=" 
                    + crawlFile.getReference());
            return true;
        }
    }   
    
    //--- Document Commit ------------------------------------------------------
    private class DocumentCommitStep implements IDocumentProcessingStep {
        @Override
        public boolean processDocument() {
            ICommitter committer = config.getCommitter();
            if (committer != null) {
                committer.queueAdd(crawlFile.getReference(), 
                        outputFile, doc.getMetadata());
            }
            return true;
        }
    }  

    //--- Document Fetch -------------------------------------------------------
    private class DocumentFetchStep implements IDocumentProcessingStep {
        @Override
        public boolean processDocument() {
            //TODO replace signature with Writer class.
            LOG.debug("Fetching document: " + doc.getReference());
            try {
                if (!crawlFile.getFileObject().exists()) {
                    crawlFile.setStatus(CrawlStatus.NOT_FOUND);
                    return false;
                }
                InputStream is = 
                        crawlFile.getFileObject().getContent().getInputStream();
                //--- Fetch body
                FileOutputStream os = FileUtils.openOutputStream(
                        doc.getLocalFile());
                IOUtils.copy(is, os);
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
                crawlFile.setStatus(CrawlStatus.OK);
                return true;
            } catch (Exception e) {
                crawlFile.setStatus(CrawlStatus.ERROR);
                if (LOG.isDebugEnabled()) {
                    LOG.error("Cannot fetch document: " + doc.getReference()
                            + " (" + e.getMessage() + ")", e);
                } else {
                    LOG.error("Cannot fetch document: " + doc.getReference()
                            + " (" + e.getMessage() + ")");
                }
                throw new FilesystemCollectorException(e);
            }  

        }
    }  

    

}

