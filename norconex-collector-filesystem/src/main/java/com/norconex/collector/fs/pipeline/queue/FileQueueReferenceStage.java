/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Collector Core.
 * 
 * Norconex Collector Core is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Collector Core is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Collector Core. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.collector.fs.pipeline.queue;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.data.BaseCrawlData;
import com.norconex.collector.core.data.store.ICrawlDataStore;
import com.norconex.collector.core.pipeline.BasePipelineContext;
import com.norconex.commons.lang.pipeline.IPipelineStage;

/**
 * Common pipeline stage for queuing documents.
 * @author Pascal Essiembre
 */
public class FileQueueReferenceStage 
implements IPipelineStage<BasePipelineContext> {

    private static final Logger LOG = 
            LogManager.getLogger(FileQueueReferenceStage.class);

    /**
     * Constructor.
     */
    public FileQueueReferenceStage() {
        super();
    }

    @Override
    public boolean execute(BasePipelineContext ctx) {
        String ref = ctx.getCrawlData().getReference();
        if (StringUtils.isBlank(ref)) {
            return true;
        }
        ICrawlDataStore refStore = ctx.getCrawlDataStore();

        if (refStore.isActive(ref)) {
            debug("Already being processed: %s", ref);
        } else if (refStore.isQueued(ref)) {
            debug("Already queued: %s", ref);
        } else if (refStore.isProcessed(ref)) {
            debug("Already processed: %s", ref);
        } else if (new File(ref).isDirectory()) {
            File[] files = new File(ref).listFiles();
            for (File file : files) {            
                BasePipelineContext context = new BasePipelineContext(
                        ctx.getCrawler(), ctx.getCrawlDataStore(), 
                        new BaseCrawlData(file.getAbsolutePath()));
                execute(context);
            }
        } else {
            refStore.queue(ctx.getCrawlData().clone());
            debug("Queued for processing: %s", ref);
        }
        return true;
    }

    private void debug(String message, Object... values) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format(message, values));
        }
    }   
}
