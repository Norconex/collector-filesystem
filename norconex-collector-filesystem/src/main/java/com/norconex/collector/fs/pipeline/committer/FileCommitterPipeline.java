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
package com.norconex.collector.fs.pipeline.committer;

import com.norconex.collector.core.crawler.event.CrawlerEvent;
import com.norconex.collector.core.pipeline.DocumentPipelineContext;
import com.norconex.collector.core.pipeline.committer.CommitModuleStage;
import com.norconex.collector.core.pipeline.committer.DocumentChecksumStage;
import com.norconex.collector.fs.doc.IFileDocumentProcessor;
import com.norconex.commons.lang.pipeline.Pipeline;

/**
 * @author Pascal Essiembre
 *
 */
public class FileCommitterPipeline extends Pipeline<DocumentPipelineContext> {

    public FileCommitterPipeline() {
        addStage(new DocumentChecksumStage());
        addStage(new DocumentPostProcessingStage());
        addStage(new CommitModuleStage());
    }
    
    //--- Document Post-Processing ---------------------------------------------
    private static class DocumentPostProcessingStage 
            extends AbstractCommitterStage {
        @Override
        public boolean executeStage(FileCommitterPipelineContext ctx) {
            if (ctx.getConfig().getPostImportProcessors() != null) {
                for (IFileDocumentProcessor postProc :
                        ctx.getConfig().getPostImportProcessors()) {
                    postProc.processDocument(
                            ctx.getCrawler().getFileManager(),
                            ctx.getDocument());
                    ctx.getCrawler().fireCrawlerEvent(
                            CrawlerEvent.DOCUMENT_POSTIMPORTED, 
                            ctx.getCrawlData(), postProc);
                }            
            }
            return true;
        }
    }  
}
