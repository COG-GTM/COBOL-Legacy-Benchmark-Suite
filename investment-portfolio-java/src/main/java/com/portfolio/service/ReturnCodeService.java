package com.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class ReturnCodeService {

    private static final Logger log = LoggerFactory.getLogger(ReturnCodeService.class);

    private final Map<String, ProgramReturnCode> returnCodes = new ConcurrentHashMap<>();

    public void initialize(String programId) {
        returnCodes.put(programId, new ProgramReturnCode(programId));
        log.debug("Return code initialized for program: {}", programId);
    }

    public void setCode(String programId, int code) {
        ProgramReturnCode prc = getOrCreate(programId);
        prc.currentCode = code;
        if (code > prc.highestCode) {
            prc.highestCode = code;
        }
        log.debug("Return code set for {}: current={} highest={}", programId, code, prc.highestCode);
    }

    public int getCode(String programId) {
        ProgramReturnCode prc = returnCodes.get(programId);
        return prc != null ? prc.currentCode : 0;
    }

    public int getHighestCode(String programId) {
        ProgramReturnCode prc = returnCodes.get(programId);
        return prc != null ? prc.highestCode : 0;
    }

    public void logCode(String programId) {
        ProgramReturnCode prc = returnCodes.get(programId);
        if (prc != null) {
            log.info("Return code for {}: current={} highest={}",
                    programId, prc.currentCode, prc.highestCode);
        }
    }

    public Map<String, Object> analyze(String programId) {
        ProgramReturnCode prc = returnCodes.get(programId);
        Map<String, Object> analysis = new ConcurrentHashMap<>();
        if (prc != null) {
            analysis.put("programId", programId);
            analysis.put("currentCode", prc.currentCode);
            analysis.put("highestCode", prc.highestCode);
            analysis.put("startTime", prc.startTime);
            analysis.put("status", prc.highestCode == 0 ? "SUCCESS"
                    : prc.highestCode <= 4 ? "WARNING"
                    : prc.highestCode <= 8 ? "ERROR"
                    : "SEVERE");
        }
        return analysis;
    }

    private ProgramReturnCode getOrCreate(String programId) {
        return returnCodes.computeIfAbsent(programId, ProgramReturnCode::new);
    }

    private static class ProgramReturnCode {
        String programId;
        int currentCode;
        int highestCode;
        LocalDateTime startTime;

        ProgramReturnCode(String programId) {
            this.programId = programId;
            this.currentCode = 0;
            this.highestCode = 0;
            this.startTime = LocalDateTime.now();
        }
    }
}
