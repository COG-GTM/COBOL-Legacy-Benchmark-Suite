package com.clbs.portfolio.harness;

import com.clbs.portfolio.model.CobolText;
import com.clbs.portfolio.model.ErrorMessage;
import com.clbs.portfolio.service.ErrorProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test double for {@code ERRPROC}: keeps a snapshot of every error it is handed and returns the
 * severity carried by the message, as the subroutine does.
 *
 * <p>Snapshotting matters. {@code PORTTRAN} logs errors out of a single working-storage area that
 * the next transaction overwrites, so a double that stored references would report the last error
 * many times over.
 */
public class RecordingErrorProcessor implements ErrorProcessor {

    private final List<ErrorMessage> errors = new ArrayList<>();

    @Override
    public int process(ErrorMessage errorMessage) {
        errors.add(new ErrorMessage(errorMessage));
        return errorMessage.getErrSeverity();
    }

    /** Every error logged, in call order. */
    public List<ErrorMessage> errors() {
        return Collections.unmodifiableList(errors);
    }

    /** The {@code ERR-TEXT} of every error logged, trimmed of its pad. */
    public List<String> messages() {
        return errors.stream().map(ErrorMessage::getErrTextTrimmed).collect(Collectors.toList());
    }

    public int count() {
        return errors.size();
    }

    /** The most recent error text, or {@code ""} when nothing has been logged. */
    public String lastMessage() {
        return errors.isEmpty() ? "" : CobolText.trim(errors.get(errors.size() - 1).getErrText());
    }

    public void reset() {
        errors.clear();
    }
}
