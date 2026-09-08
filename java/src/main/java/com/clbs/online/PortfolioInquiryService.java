package com.clbs.online;

import com.clbs.domain.PositionRecord;
import com.clbs.store.DatasetCatalog;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * INQPORT — portfolio position inquiry. The CICS READ FILE('POSFILE') keyed on the account becomes
 * a lookup in the migrated POSFILE dataset; the POSMAP SEND becomes the returned position list.
 *
 * <p>Note: the COBOL source keys POSFILE on {@code POSITION-ACCOUNT}, a field POSREC.cpy does not
 * define (its key is portfolio id + date + investment id). The migration matches on the account
 * the caller supplies against the position's portfolio id, which is the only account-like field the
 * copybook provides.
 */
@Service
public class PortfolioInquiryService {

    public static final String ERR_NOT_FOUND = "Position not found for account";

    /** Result of the inquiry: the commarea plus the positions POSMAP would display. */
    public record InquiryResult(InquiryCommArea commArea, List<PositionRecord> positions) {
    }

    private final DatasetCatalog datasets;

    public PortfolioInquiryService(DatasetCatalog datasets) {
        this.datasets = datasets;
    }

    /** P200-GET-POSITION / P300-FORMAT-DISPLAY / P900-NOT-FOUND. */
    public InquiryResult inquire(InquiryCommArea commArea) {
        String account = commArea.getAccountNo() == null ? "" : commArea.getAccountNo().trim();
        List<PositionRecord> positions = datasets.positionFile().all().stream()
                .filter(position -> position.getPortfolioId().trim().equals(account))
                .toList();

        if (positions.isEmpty()) {
            commArea.setErrorMessage(ERR_NOT_FOUND);
        }
        return new InquiryResult(commArea, positions);
    }
}
