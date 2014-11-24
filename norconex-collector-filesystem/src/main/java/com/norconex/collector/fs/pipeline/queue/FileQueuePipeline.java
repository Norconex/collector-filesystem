/* Copyright 2013-2014 Norconex Inc.
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
public final class FileQueuePipeline extends Pipeline<BasePipelineContext> {

    public FileQueuePipeline() {
        super();
        addStage(new ReferenceFiltersStage());
        addStage(new QueueReferenceStage());
    }
}

