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
package com.norconex.collector.fs.pipeline.queue;

import com.norconex.collector.core.pipeline.BasePipelineContext;
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
public final class FileQueuePipeline extends Pipeline<BasePipelineContext> {

    public FileQueuePipeline() {
        super();
        addStage(new ReferenceFiltersStage());
        addStage(new FileQueueReferenceStage()); 
    }
}

