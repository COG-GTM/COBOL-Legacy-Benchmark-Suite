package com.clbs.common;

import com.clbs.db2.ReturnCodeLogEntity;
import com.clbs.db2.ReturnCodeLogRepository;
import com.clbs.domain.ReturnCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * RTNCDE00 — standard return code handler. Each program keeps its own RETURN-CODE-AREA, keyed by
 * program id, replacing the per-run LINKAGE area of the COBOL subroutine.
 */
@Service
public class ReturnCodeHandler {

    private final Map<String, ReturnCodeArea> areas = new ConcurrentHashMap<>();
    private final ReturnCodeLogRepository repository;

    public ReturnCodeHandler(ReturnCodeLogRepository repository) {
        this.repository = repository;
    }

    /** P100-INIT-RETURN-CODES. */
    public ReturnCodeArea initialize(String programId) {
        ReturnCodeArea area = new ReturnCodeArea();
        area.setProgramId(programId);
        area.setStatus(ReturnCode.Status.SUCCESS);
        areas.put(programId, area);
        return area;
    }

    public ReturnCodeArea area(String programId) {
        return areas.computeIfAbsent(programId, id -> {
            ReturnCodeArea area = new ReturnCodeArea();
            area.setProgramId(id);
            return area;
        });
    }

    /** P200-SET-RETURN-CODE. */
    public ReturnCodeArea set(String programId, int newCode) {
        ReturnCodeArea area = area(programId);
        if (newCode > area.getHighestCode()) {
            area.setHighestCode(newCode);
        }
        area.setCurrentCode(newCode);
        area.setStatus(ReturnCode.classify(newCode));
        area.setResponseCode(0);
        return area;
    }

    /** P300-GET-RETURN-CODE. */
    public ReturnCodeArea get(String programId) {
        ReturnCodeArea area = area(programId);
        area.setResponseCode(0);
        return area;
    }

    /** P400-LOG-RETURN-CODE. */
    public int log(String programId, String message) {
        ReturnCodeArea area = area(programId);
        area.setMessage(message);
        ReturnCodeLogEntity entity = new ReturnCodeLogEntity();
        entity.setTimestamp(LocalDateTime.now().format(AuditProcessor.TIMESTAMP));
        entity.setProgramId(programId);
        entity.setReturnCode(area.getCurrentCode());
        entity.setHighestCode(area.getHighestCode());
        entity.setStatusCode(String.valueOf(area.getStatus().code()));
        entity.setMessage(message);
        repository.save(entity);
        area.setResponseCode(0);
        return area.getResponseCode();
    }

    /** P500-ANALYZE-CODES. */
    public ReturnCodeArea analyze(String programId) {
        ReturnCodeArea area = area(programId);
        List<Object[]> rows = repository.summarizeForProgram(programId);
        if (rows.isEmpty() || rows.get(0)[1] == null) {
            area.setTotalCodes(0);
            area.setMaxCode(0);
            area.setMinCode(0);
        } else {
            Object[] row = rows.get(0);
            area.setTotalCodes(((Number) row[0]).longValue());
            area.setMaxCode(((Number) row[1]).intValue());
            area.setMinCode(((Number) row[2]).intValue());
        }
        area.setResponseCode(0);
        return area;
    }

    public void reset() {
        areas.clear();
    }
}
