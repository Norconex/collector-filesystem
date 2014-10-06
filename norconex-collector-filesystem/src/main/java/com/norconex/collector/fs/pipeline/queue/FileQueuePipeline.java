/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.collector.fs.pipeline.queue;

import com.norconex.collector.core.pipeline.BasePipelineContext;
import com.norconex.collector.core.pipeline.queue.QueueReferenceStage;
import com.norconex.collector.core.pipeline.queue.ReferenceFiltersStage;
import com.norconex.commons.lang.pipeline.Pipeline;

/**
 * Performs path handling logic before actual processing of the document
 * it represents takes place.  That is, before any 
 * document or document properties download is performed.
 * Instances are only valid for the scope of a single path.  
 * This pipeline is responsible for storing potentially valid references
 * to the crawl data store queue.
 * @author Pascal Essiembre
 */
public final class FileQueuePipeline 
        extends Pipeline<BasePipelineContext> {
//        extends AbstractQueuePipeline<BasePipelineContext> {

    public FileQueuePipeline() {
        super();
        addStage(new ReferenceFiltersStage());
        addStage(new QueueReferenceStage());
    }

//    @Override
//    protected void addPipelineStages() {
//        addStage(new ReferenceFiltersStage());
//        addStage(new QueueReferenceStage());
//    }
    
//    //--- Store Next References to process -------------------------------------------
//    private class StoreNextReferenceStage
//            implements IPipelineStage<QueuePipelineContext> {
//        @Override
//        public boolean execute(final QueuePipelineContext ctx) {
//            String ref = ctx.getCrawlData().getReference();
//            if (StringUtils.isBlank(ref)) {
//                return true;
//            }
//            ICrawlDataStore refStore = ctx.getCrawlDataStore();
//            
//            if (refStore.isActive(ref)) {
//                debug("Already being processed: %s", ref);
//            } else if (refStore.isQueued(ref)) {
//                debug("Already queued: %s", ref);
//            } else if (refStore.isProcessed(ref)) {
//                debug("Already processed: %s", ref);
//            } else {
//                refStore.queue(ctx.getCrawlData().clone());
////                refStore.queue(new BaseCrawlData(ref));
//                debug("Queued for processing: %s", ref);
//            }
//            return true;
//        }
//    }
//    
//    private static void debug(String message, Object... values) {
//        if (LOG.isDebugEnabled()) {
//            LOG.debug(String.format(message, values));
//        }
//    }    
}

