package com.clbs.portfolio.batch;

import com.clbs.portfolio.domain.Portfolio;
import com.clbs.portfolio.domain.PortfolioKey;
import com.clbs.portfolio.exception.DuplicateException;
import com.clbs.portfolio.exception.InvalidRequestException;
import com.clbs.portfolio.exception.NotFoundException;
import com.clbs.portfolio.service.PortfolioService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.clbs.portfolio.service.CobolText.pic;
import static com.clbs.portfolio.service.CobolText.trimRight;

/**
 * Sequential-file batch jobs PORTADD, PORTUPDT, PORTDEL and PORTREAD. Inputs use the original record layouts and
 * the output reproduces the programs' DISPLAY lines, so runs can be compared with parity/baseline.
 */
@Service
public class PortfolioBatchService {

    /** PORTFLIO.cpy record length. */
    public static final int PORTFOLIO_RECORD_LENGTH = 148;
    /** PORTUPDT UPDATE-RECORD length. */
    public static final int UPDATE_RECORD_LENGTH = 69;
    /** PORTDEL DELETE-RECORD length. */
    public static final int DELETE_RECORD_LENGTH = 80;

    private static final DateTimeFormatter AUDIT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final PortfolioService portfolioService;
    private final Clock clock;

    public PortfolioBatchService(PortfolioService portfolioService, Clock clock) {
        this.portfolioService = portfolioService;
        this.clock = clock;
    }

    /** PORTADD: create portfolios from PORTFLIO records. */
    public BatchReport add(byte[] input) {
        List<String> out = new ArrayList<>();
        int added = 0;
        int duplicates = 0;
        int errors = 0;
        for (FixedWidthRecord rec : FixedWidthRecord.split(input, PORTFOLIO_RECORD_LENGTH)) {
            String id = rec.text(0, 8);
            try {
                portfolioService.create(decodePortfolio(rec));
                added++;
            } catch (InvalidRequestException e) {
                errors++;
                out.add("Invalid record data: " + id);
            } catch (DuplicateException e) {
                duplicates++;
                out.add("Duplicate record: " + id);
            }
        }
        out.add("Records added:    " + count(added));
        out.add("Duplicate records:" + count(duplicates));
        out.add("Errors occurred:  " + count(errors));
        return new BatchReport("PORTADD", counts("added", added, "duplicates", duplicates, "errors", errors), out,
                List.of());
    }

    /** PORTUPDT: apply S/N/V updates. */
    public BatchReport update(byte[] input) {
        List<String> out = new ArrayList<>();
        int updated = 0;
        int errors = 0;
        for (FixedWidthRecord rec : FixedWidthRecord.split(input, UPDATE_RECORD_LENGTH)) {
            String rawKey = rec.text(0, 18);
            try {
                portfolioService.update(key(rec), rec.text(18, 1).charAt(0), rec.text(19, 50));
                updated++;
            } catch (NotFoundException e) {
                errors++;
                out.add("Record not found: " + rawKey);
            } catch (InvalidRequestException e) {
                errors++;
                out.add("Update failed for: " + rawKey);
            }
        }
        out.add("Updates processed: " + count(updated));
        out.add("Errors occurred:  " + count(errors));
        return new BatchReport("PORTUPDT", counts("updated", updated, "errors", errors), out, List.of());
    }

    /** PORTDEL: delete portfolios and write the 80-byte audit records. */
    public BatchReport delete(byte[] input) {
        List<String> out = new ArrayList<>();
        List<String> audit = new ArrayList<>();
        int deleted = 0;
        int notFound = 0;
        for (FixedWidthRecord rec : FixedWidthRecord.split(input, DELETE_RECORD_LENGTH)) {
            String rawKey = rec.text(0, 18);
            String reason = rec.text(18, 2);
            try {
                Portfolio removed = portfolioService.delete(key(rec), reason);
                deleted++;
                audit.add(LocalDateTime.now(clock).format(AUDIT_TIMESTAMP) + "DELETE" + rawKey + reason
                        + pic(removed.getStatus(), 1) + " ".repeat(27));
            } catch (NotFoundException e) {
                notFound++;
                out.add("Record not found: " + rawKey);
            }
        }
        out.add("Records deleted:  " + count(deleted));
        out.add("Records not found:" + count(notFound));
        out.add("Errors occurred:  " + count(0));
        return new BatchReport("PORTDEL", counts("deleted", deleted, "notFound", notFound, "errors", 0), out, audit);
    }

    /** PORTREAD: list every portfolio in key order. */
    public BatchReport report() {
        List<String> out = new ArrayList<>();
        int n = 0;
        for (Portfolio p : portfolioService.findAll()) {
            n++;
            out.add("Portfolio Record: " + count(n));
            out.add("  ID: " + pic(p.getPortfolioId(), 8));
            out.add("  Account: " + pic(p.getAccountNo(), 10));
            out.add("  Client: " + pic(p.getClientName(), 30));
            out.add("  Status: " + pic(p.getStatus(), 1));
            out.add("  Total Value: " + signedAmount(p.getTotalValue()));
            out.add(" ");
        }
        out.add("Total Records Read: " + count(n));
        return new BatchReport("PORTREAD", counts("read", n), out, List.of());
    }

    static Portfolio decodePortfolio(FixedWidthRecord rec) {
        Portfolio p = new Portfolio(new PortfolioKey(trimRight(rec.text(0, 8)), trimRight(rec.text(8, 10))));
        p.setClientName(trimRight(rec.text(18, 30)));
        p.setClientType(rec.text(48, 1));
        p.setStatus(rec.text(65, 1));
        p.setTotalValue(PackedDecimal.decode(rec.bytes(), 66, 8, 2));
        p.setCashBalance(PackedDecimal.decode(rec.bytes(), 74, 8, 2));
        p.setLastUser(trimRight(rec.text(82, 8)));
        p.setLastTrans(trimRight(rec.text(90, 8)));
        return p;
    }

    private static PortfolioKey key(FixedWidthRecord rec) {
        return new PortfolioKey(trimRight(rec.text(0, 8)), trimRight(rec.text(8, 10)));
    }

    /** DISPLAY of PIC 9(7). */
    private static String count(int n) {
        return String.format("%07d", n);
    }

    /** DISPLAY of PIC S9(13)V99 COMP-3: sign, 13 integer digits, point, 2 decimals. */
    static String signedAmount(BigDecimal value) {
        BigDecimal v = value.setScale(2, java.math.RoundingMode.DOWN);
        String digits = String.format("%016.2f", v.abs());
        return (v.signum() < 0 ? "-" : "+") + digits;
    }

    private static Map<String, Integer> counts(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }
}
