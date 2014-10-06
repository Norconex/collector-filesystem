/**
 * 
 */
package com.norconex.collector.fs.pipeline.committer;

import com.norconex.collector.core.pipeline.DocumentPipelineContext;
import com.norconex.commons.lang.pipeline.IPipelineStage;

/**
 * @author Pascal Essiembre
 *
 */
/*default*/ abstract class AbstractCommitterStage 
        implements IPipelineStage<DocumentPipelineContext> {
    @Override
    public final boolean execute(DocumentPipelineContext context) {
        return executeStage((FileCommitterPipelineContext) context);
    }
    public abstract boolean executeStage(FileCommitterPipelineContext ctx);
}
