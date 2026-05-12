package com.portfolio.service.common;

import com.portfolio.model.entity.ReturnCodeEntry;
import com.portfolio.repository.ReturnCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReturnCodeService {

    private static final Logger log = LoggerFactory.getLogger(ReturnCodeService.class);

    private final ReturnCodeRepository returnCodeRepository;

    public ReturnCodeService(ReturnCodeRepository returnCodeRepository) {
        this.returnCodeRepository = returnCodeRepository;
    }

    @Transactional
    public void logReturnCode(String programId, int returnCode, int highestCode,
                              char statusCode, String message) {
        ReturnCodeEntry entry = new ReturnCodeEntry();
        entry.setEntryTimestamp(LocalDateTime.now());
        entry.setProgramId(programId);
        entry.setReturnCode(returnCode);
        entry.setHighestCode(highestCode);
        entry.setStatusCode(statusCode);
        entry.setMessageText(message);
        returnCodeRepository.save(entry);
        log.debug("Return code logged: program={} rc={} highest={}", programId, returnCode, highestCode);
    }
}
