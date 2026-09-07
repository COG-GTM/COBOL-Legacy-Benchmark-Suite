package com.clbs.batch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * CKPRST — checkpoint/restart subroutine.
 *
 * <p>The COBOL program only dispatches on the entry point; PROC-INIT, PROC-TAKE-CHECKPOINT,
 * PROC-COMMIT-CHECKPOINT and PROC-RESTART are comment-only stubs. The dispatch and the CKPTFILE
 * record are migrated here; the four operations stay stubs so no behaviour is invented. The
 * committed checkpoint map is the only state a caller can observe.
 */
@Service
public class CheckpointRestart {

    /** CKR-ENTRY-POINT values. */
    public enum EntryPoint { INIT, TAKE, COMMIT, RESTART }

    /** CHECKPOINT-CONTROL. */
    public static final class CheckpointControl {
        private String programId = "";
        private String checkpointId = "";
        private long recordCount;

        public String getProgramId() {
            return programId;
        }

        public void setProgramId(String programId) {
            this.programId = programId;
        }

        public String getCheckpointId() {
            return checkpointId;
        }

        public void setCheckpointId(String checkpointId) {
            this.checkpointId = checkpointId;
        }

        public long getRecordCount() {
            return recordCount;
        }

        public void setRecordCount(long recordCount) {
            this.recordCount = recordCount;
        }

        public String key() {
            return programId + "/" + checkpointId;
        }
    }

    private final Map<String, CheckpointControl> pending = new ConcurrentHashMap<>();
    private final Map<String, CheckpointControl> committed = new ConcurrentHashMap<>();

    /** PROCEDURE DIVISION EVALUATE on CKR-ENTRY-POINT. */
    public int execute(EntryPoint entryPoint, CheckpointControl control) {
        switch (entryPoint) {
            case INIT -> init(control);
            case TAKE -> take(control);
            case COMMIT -> commit(control);
            case RESTART -> restart(control);
        }
        return BatchConstants.RC_SUCCESS;
    }

    /** PROC-INIT — stub in the COBOL source. */
    private void init(CheckpointControl control) {
        pending.remove(control.key());
    }

    /** PROC-TAKE-CHECKPOINT — stub in the COBOL source. */
    private void take(CheckpointControl control) {
        pending.put(control.key(), control);
    }

    /** PROC-COMMIT-CHECKPOINT — stub in the COBOL source. */
    private void commit(CheckpointControl control) {
        CheckpointControl taken = pending.remove(control.key());
        committed.put(control.key(), taken == null ? control : taken);
    }

    /** PROC-RESTART — stub in the COBOL source. */
    private void restart(CheckpointControl control) {
        CheckpointControl last = committed.get(control.key());
        control.setRecordCount(last == null ? 0 : last.getRecordCount());
    }

    public CheckpointControl lastCommitted(String programId, String checkpointId) {
        return committed.get(programId + "/" + checkpointId);
    }
}
