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
package com.norconex.collector.fs.pipeline.importer;

import org.apache.commons.vfs2.FileObject;

import com.norconex.collector.core.pipeline.importer.ImporterPipelineContext;
import com.norconex.collector.fs.crawler.FilesystemCrawler;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.collector.fs.doc.FileDocument;
import com.norconex.collector.fs.doc.FileMetadata;

/**
 * @author Pascal Essiembre
 *
 */
public class FileImporterPipelineContext extends ImporterPipelineContext {

    private FileObject fileObject;

    /**
     * Constructor creating a copy of supplied context.
     * 
     * @param copiable the item to be copied
     * @since 2.7.2
     */
    public FileImporterPipelineContext(ImporterPipelineContext copiable) {
        super(copiable.getCrawler(), new FileDocument(copiable.getDocument()));
        setImporterResponse(copiable.getImporterResponse());
    }

    @Override
    public FilesystemCrawler getCrawler() {
        return (FilesystemCrawler) super.getCrawler();
    }

    @Override
    public FilesystemCrawlerConfig getConfig() {
        return getCrawler().getCrawlerConfig();
    }

    @Override
    public FileDocument getDocument() {
        return (FileDocument) super.getDocument();
    }

    public FileObject getFileObject() {
        return fileObject;
    }

    /**
     * Sets file object.
     * 
     * @param fileObject file object
     * @since 2.7.2
     */
    public void setFileObject(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    public FileMetadata getMetadata() {
        return new FileMetadata(getDocument().getMetadata());
    }

}
