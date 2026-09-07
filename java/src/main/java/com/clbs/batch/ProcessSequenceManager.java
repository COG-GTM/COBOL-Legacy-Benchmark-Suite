package com.clbs.batch;

import com.clbs.common.ErrorMessage;
import com.clbs.common.ErrorProcessor;
import com.clbs.domain.ErrorCodes;
import com.clbs.store.DatasetCatalog;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * PRCSEQ00 — process sequence manager. WS-PROCESS-TABLE (OCCURS 100) becomes {@link Session}, so a
 * caller drives INIT / NEXT / STAT / TERM against one sequence run exactly as the COBOL program
 * does against its working storage.
 */
@Service
public class ProcessSequenceManager {

    public static final String PROGRAM = "PRCSEQ00";
    public static final int MAX_PROCESSES = 100;

    public static final String ERR_NO_SEQUENCE = "No sequence found for date";
    public static final String ERR_NO_DEFINITION = "Process definition not found";
    public static final String ERR_NO_DEPENDENCY = "Dependency record not found";
    public static final String ERR_NO_PROCESS = "Process record not found";
    public static final String ERR_CREATE = "Error creating control record";
    public static final String ERR_UPDATE = "Error updating control record";

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DatasetCatalog datasets;
    private final ErrorProcessor errorProcessor;

    public ProcessSequenceManager(DatasetCatalog datasets, ErrorProcessor errorProcessor) {
        this.datasets = datasets;
        this.errorProcessor = errorProcessor;
    }

    /** One WS-PROC-ENTRY of WS-PROCESS-TABLE. */
    public static final class Entry {
        private final String processId;
        private final int sequence;
        private char status = BatchControlRecord.READY;
        private int returnCode;

        Entry(String processId, int sequence) {
            this.processId = processId;
            this.sequence = sequence;
        }

        public String getProcessId() {
            return processId;
        }

        public int getSequence() {
            return sequence;
        }

        public char getStatus() {
            return status;
        }

        public int getReturnCode() {
            return returnCode;
        }
    }

    /** WS-PROCESS-TABLE plus the LS-SEQUENCE-REQUEST fields that persist across calls. */
    public static final class Session {
        private final String processDate;
        private final String sequenceType;
        private final List<Entry> entries = new ArrayList<>();
        private String nextProcess = "";
        private int returnCode;

        Session(String processDate, String sequenceType) {
            this.processDate = processDate;
            this.sequenceType = sequenceType;
        }

        public List<Entry> getEntries() {
            return entries;
        }

        public String getNextProcess() {
            return nextProcess;
        }

        public int getReturnCode() {
            return returnCode;
        }

        public String getProcessDate() {
            return processDate;
        }

        public String getSequenceType() {
            return sequenceType;
        }
    }

    /** 1000-INITIALIZE-SEQUENCE: 1200-BUILD-SEQUENCE then 1300-CREATE-CONTROL-RECORDS. */
    public Session initialize(String processDate, String sequenceType) {
        Session session = new Session(processDate, sequenceType);

        // 1200-BUILD-SEQUENCE
        for (ProcessSequenceRecord definition : datasets.processSequenceFile().all()) {
            if (!sequenceType.equals(definition.getType()) || session.entries.size() >= MAX_PROCESSES) {
                continue;
            }
            session.entries.add(new Entry(definition.getProcessId(), session.entries.size() + 1));
        }
        if (session.entries.isEmpty()) {
            session.returnCode = error(ERR_NO_SEQUENCE);
            return session;
        }

        // 1300-CREATE-CONTROL-RECORDS
        for (Entry entry : session.entries) {
            BatchControlRecord control = new BatchControlRecord();
            control.setJobName(entry.processId);
            control.setProcessDate(processDate);
            control.setSequenceNo(String.format("%04d", entry.sequence));
            control.setStatus(BatchControlRecord.READY);
            if (!com.clbs.store.FileStatus.SUCCESS
                    .equals(datasets.batchControlFile().write(control))) {
                session.returnCode = error(ERR_CREATE);
            }
        }
        return session;
    }

    /** 2000-GET-NEXT-PROCESS. */
    public Session next(Session session) {
        session.returnCode = BatchConstants.RC_SUCCESS;

        // 2100-FIND-NEXT-READY
        session.nextProcess = session.entries.stream()
                .filter(entry -> entry.status == BatchControlRecord.READY)
                .map(entry -> entry.processId)
                .findFirst()
                .orElse("");
        if (session.nextProcess.isEmpty()) {
            return session;
        }

        // 2200-CHECK-DEPENDENCIES
        ProcessSequenceRecord definition = findDefinition(session.nextProcess);
        if (definition == null) {
            session.returnCode = error(ERR_NO_DEFINITION);
            return session;
        }
        for (ProcessSequenceRecord.Dependency dependency : definition.getDependencies()) {
            BatchControlRecord control = findControl(dependency.processId(), session.processDate);
            if (control == null) {
                session.returnCode = error(ERR_NO_DEPENDENCY);
                return session;
            }
            if (!control.isDone()) {
                if (dependency.hard()) {
                    session.returnCode = BatchConstants.RC_WARNING;
                    return session;
                }
            } else if (control.getReturnCode() > dependency.maxReturnCode()) {
                session.returnCode = BatchConstants.RC_ERROR;
                return session;
            }
        }

        // 2300-UPDATE-PROCESS-STATUS
        BatchControlRecord control = findControl(session.nextProcess, session.processDate);
        if (control == null) {
            session.returnCode = error(ERR_NO_PROCESS);
            return session;
        }
        control.setStatus(BatchControlRecord.ACTIVE);
        control.setStartTime(LocalDateTime.now().format(TIME));
        if (!com.clbs.store.FileStatus.SUCCESS.equals(datasets.batchControlFile().rewrite(control))) {
            session.returnCode = error(ERR_UPDATE);
        }
        return session;
    }

    /** 3000-CHECK-STATUS: refresh the table entry for LS-NEXT-PROCESS. */
    public Session checkStatus(Session session) {
        BatchControlRecord control = findControl(session.nextProcess, session.processDate);
        if (control == null) {
            session.returnCode = error(ERR_NO_PROCESS);
            return session;
        }
        for (Entry entry : session.entries) {
            if (entry.processId.equals(control.getJobName())) {
                entry.status = control.getStatus();
                entry.returnCode = control.getReturnCode();
                break;
            }
        }
        return session;
    }

    /** 4000-TERMINATE-SEQUENCE / 4100-CHECK-FINAL-STATUS. */
    public int terminate(Session session) {
        long active = session.entries.stream()
                .filter(entry -> entry.status == BatchControlRecord.ACTIVE).count();
        long errors = session.entries.stream()
                .filter(entry -> entry.status == BatchControlRecord.ERROR).count();

        if (errors > 0) {
            session.returnCode = BatchConstants.RC_ERROR;
        } else if (active > 0) {
            session.returnCode = BatchConstants.RC_WARNING;
        } else {
            session.returnCode = BatchConstants.RC_SUCCESS;
        }
        return session.returnCode;
    }

    private ProcessSequenceRecord findDefinition(String processId) {
        return datasets.processSequenceFile().all().stream()
                .filter(record -> record.getProcessId().equals(processId))
                .findFirst()
                .orElse(null);
    }

    private BatchControlRecord findControl(String jobName, String processDate) {
        return datasets.batchControlFile().all().stream()
                .filter(record -> record.getJobName().equals(jobName)
                        && record.getProcessDate().equals(processDate))
                .findFirst()
                .orElse(null);
    }

    /** 9000-ERROR-ROUTINE. */
    private int error(String text) {
        errorProcessor.process(new ErrorMessage(PROGRAM, ErrorCodes.CAT_PROCESSING,
                ErrorCodes.PROCESSING, BatchConstants.RC_ERROR, text, ""));
        return BatchConstants.RC_ERROR;
    }
}
