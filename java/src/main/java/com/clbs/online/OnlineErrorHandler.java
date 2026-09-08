package com.clbs.online;

import com.clbs.db2.ErrorLogEntity;
import com.clbs.db2.ErrorLogRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * ERRHNDL — centralised online error handler. The ERRLOG INSERT becomes a JPA save; the CICS
 * commarea (ERRHND.cpy) becomes {@link ErrorArea}.
 */
@Service
public class OnlineErrorHandler {

    public static final char SEVERITY_FATAL = 'F';
    public static final char SEVERITY_WARNING = 'W';
    public static final char SEVERITY_INFO = 'I';

    public static final char ACTION_RETURN = 'R';
    public static final char ACTION_CONTINUE = 'C';
    public static final char ACTION_ABEND = 'A';

    public static final String ERR_LOG_FAILED = "Error logging failed";

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    /** ERROR-HANDLING commarea. */
    public record ErrorArea(String program, String paragraph, int sqlCode, int cicsResp,
            char severity, String message, char action, String traceId, String timestamp) {
    }

    private final ErrorLogRepository repository;

    public OnlineErrorHandler(ErrorLogRepository repository) {
        this.repository = repository;
    }

    /** The full P100–P400 sequence. */
    public ErrorArea handle(ErrorArea input) {
        ErrorArea area = initialize(input);
        boolean logged = logError(area);
        char severity = logged ? area.severity() : SEVERITY_FATAL;
        String message = logged ? formatMessage(area) : ERR_LOG_FAILED;
        return new ErrorArea(area.program(), area.paragraph(), area.sqlCode(), area.cicsResp(),
                severity, message, determineAction(severity), area.traceId(), area.timestamp());
    }

    /** P100-INIT-ERROR-HANDLER: stamp the time and assign a trace id when absent. */
    ErrorArea initialize(ErrorArea input) {
        String traceId = input.traceId() == null || input.traceId().isBlank()
                ? UUID.randomUUID().toString().substring(0, 16)
                : input.traceId();
        return new ErrorArea(input.program(), input.paragraph(), input.sqlCode(),
                input.cicsResp(), input.severity(), input.message(), input.action(), traceId,
                LocalDateTime.now().format(TIMESTAMP));
    }

    /** P200-LOG-ERROR. */
    boolean logError(ErrorArea area) {
        try {
            ErrorLogEntity entity = new ErrorLogEntity();
            entity.setTimestamp(area.timestamp());
            entity.setProgramId(area.program());
            entity.setParagraphName(area.paragraph());
            entity.setSqlCode(area.sqlCode());
            entity.setCicsResp(area.cicsResp());
            entity.setSeverity(String.valueOf(area.severity()));
            entity.setMessage(area.message());
            entity.setTraceId(area.traceId());
            repository.save(entity);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** P300-FORMAT-MESSAGE. */
    static String formatMessage(ErrorArea area) {
        return "Error in " + area.program().trim() + " - " + area.message() + " ("
                + area.traceId() + ")";
    }

    /** P400-DETERMINE-ACTION. */
    static char determineAction(char severity) {
        return switch (severity) {
            case SEVERITY_FATAL -> ACTION_ABEND;
            case SEVERITY_WARNING, SEVERITY_INFO -> ACTION_CONTINUE;
            default -> ACTION_RETURN;
        };
    }
}
