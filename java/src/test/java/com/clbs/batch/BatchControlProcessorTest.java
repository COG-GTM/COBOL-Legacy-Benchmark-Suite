package com.clbs.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.clbs.batch.BatchControlProcessor.ControlRequest;
import com.clbs.common.ErrorProcessor;
import com.clbs.db2.ErrorLogRepository;
import com.clbs.store.DatasetCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchControlProcessorTest {

    private DatasetCatalog datasets;
    private BatchControlProcessor processor;

    @BeforeEach
    void setUp() {
        datasets = new DatasetCatalog();
        datasets.batchControlFile().open();
        processor = new BatchControlProcessor(datasets,
                new ErrorProcessor(mock(ErrorLogRepository.class)));
        datasets.batchControlFile().write(control("HISTLOAD", "0010", BatchControlRecord.READY));
    }

    private static BatchControlRecord control(String job, String sequence, char status) {
        BatchControlRecord record = new BatchControlRecord();
        record.setJobName(job);
        record.setProcessDate("20240320");
        record.setSequenceNo(sequence);
        record.setStatus(status);
        return record;
    }

    private static ControlRequest request(String function, String job, String sequence) {
        return new ControlRequest(function, job, "20240320", sequence);
    }

    @Test
    void initializeMarksProcessActive() {
        assertThat(processor.initialize(request("INIT", "HISTLOAD", "0010")))
                .isEqualTo(BatchConstants.RC_SUCCESS);
        assertThat(datasets.batchControlFile().all().get(0).getStatus())
                .isEqualTo(BatchControlRecord.ACTIVE);
    }

    @Test
    void initializeRejectsAnAlreadyActiveProcess() {
        processor.initialize(request("INIT", "HISTLOAD", "0010"));

        assertThat(processor.initialize(request("INIT", "HISTLOAD", "0010")))
                .isEqualTo(BatchConstants.RC_ERROR);
    }

    @Test
    void missingControlRecordIsAnError() {
        assertThat(processor.initialize(request("INIT", "NOSUCH", "0010")))
                .isEqualTo(BatchConstants.RC_ERROR);
    }

    @Test
    void unknownFunctionIsRejected() {
        assertThat(processor.execute(request("ZZZZ", "HISTLOAD", "0010")))
                .isEqualTo(BatchConstants.RC_ERROR);
    }

    @Test
    void prerequisitesMustBeDoneWithinTheirReturnCode() {
        BatchControlRecord dependant = control("POSUPD", "0020", BatchControlRecord.READY);
        dependant.getPrerequisites().add(new BatchControlRecord.Prerequisite("HISTLOAD", "0010",
                BatchConstants.RC_WARNING));
        datasets.batchControlFile().write(dependant);

        assertThat(processor.checkPrerequisites(request("CHEK", "POSUPD", "0020")))
                .isEqualTo(BatchConstants.RC_WARNING);

        processor.terminate(request("TERM", "HISTLOAD", "0010"), BatchConstants.RC_SUCCESS);
        assertThat(processor.checkPrerequisites(request("CHEK", "POSUPD", "0020")))
                .isEqualTo(BatchConstants.RC_SUCCESS);

        processor.terminate(request("TERM", "HISTLOAD", "0010"), BatchConstants.RC_ERROR);
        assertThat(processor.checkPrerequisites(request("CHEK", "POSUPD", "0020")))
                .isEqualTo(BatchConstants.RC_WARNING);
    }

    @Test
    void terminateFlagsErrorStatusAboveWarning() {
        processor.terminate(request("TERM", "HISTLOAD", "0010"), BatchConstants.RC_SEVERE);

        BatchControlRecord record = datasets.batchControlFile().all().get(0);
        assertThat(record.getStatus()).isEqualTo(BatchControlRecord.ERROR);
        assertThat(record.getReturnCode()).isEqualTo(BatchConstants.RC_SEVERE);
    }
}
