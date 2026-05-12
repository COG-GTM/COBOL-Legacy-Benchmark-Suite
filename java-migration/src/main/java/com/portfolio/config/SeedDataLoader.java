package com.portfolio.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import com.portfolio.entity.PortfolioMaster;
import com.portfolio.repository.PortfolioMasterRepository;

/**
 * Runtime seed data loader that reads the original COBOL-format flat file.
 * Activated only with the "seed" profile: {@code --spring.profiles.active=seed}
 * <p>
 * This is secondary to the Flyway seed migration (V2), which is the primary
 * seeding mechanism. This loader demonstrates runtime parsing of the fixed-width
 * COBOL record layout from PORTFLIO.cpy.
 * <p>
 * Record layout (148 bytes total, excluding COMP-3 binary fields in ASCII):
 * <pre>
 * Offset  Length  Field               PIC clause
 * 0       8       PORT-ID             PIC X(8)
 * 8       10      PORT-ACCOUNT-NO     PIC X(10)
 * 18      30      PORT-CLIENT-NAME    PIC X(30)
 * 48      1       PORT-CLIENT-TYPE    PIC X(1)
 * 49      8       PORT-CREATE-DATE    PIC 9(8)     YYYYMMDD
 * 57      8       PORT-LAST-MAINT     PIC 9(8)     YYYYMMDD
 * 65      1       PORT-STATUS         PIC X(1)
 * 66-81           (COMP-3 fields - skipped in ASCII text mode)
 * 82      8       PORT-LAST-USER      PIC X(8)
 * 90      8       PORT-LAST-TRANS     PIC 9(8)     YYYYMMDD
 * 98      50      PORT-FILLER         PIC X(50)
 * </pre>
 */
@Configuration
@Profile("seed")
public class SeedDataLoader {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);
    private static final DateTimeFormatter COBOL_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Bean
    CommandLineRunner loadSeedData(PortfolioMasterRepository repository) {
        return args -> {
            ClassPathResource resource = new ClassPathResource("seed/portfolio-seed.txt");
            if (!resource.exists()) {
                log.warn("Seed file not found at classpath:seed/portfolio-seed.txt — skipping runtime load");
                return;
            }

            int count = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() < 98) {
                        log.warn("Skipping short record (length {}): {}", line.length(), line);
                        continue;
                    }
                    PortfolioMaster record = parseRecord(line);
                    if (!repository.existsById(record.getPortId())) {
                        repository.save(record);
                        count++;
                    }
                }
            }
            log.info("Loaded {} portfolio records from seed file", count);
        };
    }

    private PortfolioMaster parseRecord(String line) {
        PortfolioMaster p = new PortfolioMaster();
        p.setPortId(line.substring(0, 8).trim());
        p.setPortAccountNo(line.substring(8, 18).trim());
        p.setPortClientName(line.substring(18, 48).trim());
        p.setPortClientType(line.substring(48, 49));
        p.setPortCreateDate(parseDate(line.substring(49, 57)));
        p.setPortLastMaint(parseDate(line.substring(57, 65)));
        p.setPortStatus(line.substring(65, 66));
        // COMP-3 fields (offsets 66-81) are binary and skipped in ASCII text mode
        p.setPortTotalValue(BigDecimal.ZERO);
        p.setPortCashBalance(BigDecimal.ZERO);
        p.setPortLastUser(line.substring(82, 90).trim());
        p.setPortLastTrans(parseDate(line.substring(90, 98)));
        return p;
    }

    private LocalDate parseDate(String yyyymmdd) {
        try {
            return LocalDate.parse(yyyymmdd, COBOL_DATE);
        } catch (Exception e) {
            log.warn("Could not parse date '{}', using epoch", yyyymmdd);
            return LocalDate.of(1900, 1, 1);
        }
    }
}
