package com.portfolio.batch.listeners;

import com.portfolio.service.CheckpointRestartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

/**
 * Checkpoint Listener.
 * Implements ChunkListener to save checkpoint data after each chunk.
 * Replaces: CKPRST.cpy functionality - program-level checkpointing.
 */
@Component
public class CheckpointListener implements ChunkListener {

    private static final Logger log = LoggerFactory.getLogger(CheckpointListener.class);

    private final CheckpointRestartService checkpointService;

    public CheckpointListener(CheckpointRestartService checkpointService) {
        this.checkpointService = checkpointService;
    }

    @Override
    public void beforeChunk(ChunkContext context) {
        // No action needed before chunk
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long readCount = context.getStepContext().getStepExecution().getReadCount();
        String stepName = context.getStepContext().getStepName();

        checkpointService.saveCheckpointFromContext(context, "record-" + readCount);

        log.debug("Checkpoint saved after chunk: step={}, records={}", stepName, readCount);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        String stepName = context.getStepContext().getStepName();
        log.error("Chunk error in step: {}", stepName);
    }
}
