package com.clbs.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clbs.db2.ReturnCodeLogEntity;
import com.clbs.db2.ReturnCodeLogRepository;
import com.clbs.domain.ReturnCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReturnCodeHandlerTest {

    private ReturnCodeLogRepository repository;
    private ReturnCodeHandler handler;

    @BeforeEach
    void setUp() {
        repository = mock(ReturnCodeLogRepository.class);
        handler = new ReturnCodeHandler(repository);
    }

    @Test
    void classificationFollowsRtncde00Bands() {
        assertThat(ReturnCode.classify(0)).isEqualTo(ReturnCode.Status.SUCCESS);
        assertThat(ReturnCode.classify(4)).isEqualTo(ReturnCode.Status.WARNING);
        assertThat(ReturnCode.classify(8)).isEqualTo(ReturnCode.Status.ERROR);
        assertThat(ReturnCode.classify(12)).isEqualTo(ReturnCode.Status.SEVERE);
        assertThat(ReturnCode.classify(16)).isEqualTo(ReturnCode.Status.SEVERE);
    }

    @Test
    void setKeepsHighestCodeSeen() {
        handler.initialize("PORTMSTR");
        handler.set("PORTMSTR", ReturnCode.ERROR);
        ReturnCodeArea area = handler.set("PORTMSTR", ReturnCode.WARNING);

        assertThat(area.getCurrentCode()).isEqualTo(ReturnCode.WARNING);
        assertThat(area.getHighestCode()).isEqualTo(ReturnCode.ERROR);
        assertThat(area.getStatus()).isEqualTo(ReturnCode.Status.WARNING);
    }

    @Test
    void logPersistsCurrentAndHighestCode() {
        handler.set("PORTMSTR", ReturnCode.SEVERE);
        handler.log("PORTMSTR", "abend");

        ArgumentCaptor<ReturnCodeLogEntity> captor =
                ArgumentCaptor.forClass(ReturnCodeLogEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReturnCode()).isEqualTo(ReturnCode.SEVERE);
        assertThat(captor.getValue().getHighestCode()).isEqualTo(ReturnCode.SEVERE);
        assertThat(captor.getValue().getStatusCode()).isEqualTo("F");
        assertThat(captor.getValue().getMessage()).isEqualTo("abend");
    }

    @Test
    void analyzeSummarisesLoggedCodes() {
        when(repository.summarizeForProgram("PORTMSTR"))
                .thenReturn(List.<Object[]>of(new Object[] {3L, 12, 0}));

        ReturnCodeArea area = handler.analyze("PORTMSTR");
        assertThat(area.getTotalCodes()).isEqualTo(3);
        assertThat(area.getMaxCode()).isEqualTo(12);
        assertThat(area.getMinCode()).isZero();
    }

    @Test
    void analyzeWithoutRowsIsZeroed() {
        when(repository.summarizeForProgram("PORTMSTR")).thenReturn(List.of());

        ReturnCodeArea area = handler.analyze("PORTMSTR");
        assertThat(area.getTotalCodes()).isZero();
        assertThat(area.getMaxCode()).isZero();
    }
}
