package com.benchmark.portfolio.common.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Round-trip persistence test: creates the schema from the baseline DDL
 * (java/db/ddl/V1__baseline_schema.sql), then persists and reloads each entity
 * through Hibernate to prove the mappings work against the real schema
 * (composite @EmbeddedId keys, identity keys, BigDecimal precision/scale).
 */
class EntityPersistenceTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:entity_persistence;MODE=PostgreSQL;DATABASE_TO_LOWER=FALSE;DB_CLOSE_DELAY=-1";

    private static Connection keepAlive;
    private static SessionFactory sessionFactory;

    @BeforeAll
    static void setUp() throws Exception {
        keepAlive = DriverManager.getConnection(JDBC_URL);
        String sql = Files.readString(Path.of("..", "db", "ddl", "V1__baseline_schema.sql"));
        try (Statement stmt = keepAlive.createStatement()) {
            stmt.execute(sql);
        }
        Configuration configuration = new Configuration()
                .addAnnotatedClass(PortfolioMaster.class)
                .addAnnotatedClass(PortfolioTransaction.class)
                .addAnnotatedClass(PortfolioPosition.class)
                .addAnnotatedClass(HistoryRecord.class)
                .addAnnotatedClass(ErrorLog.class)
                .addAnnotatedClass(AuditLog.class)
                .setProperty("hibernate.connection.url", JDBC_URL)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "none");
        sessionFactory = configuration.buildSessionFactory();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        keepAlive.close();
    }

    @Test
    void persistsAndReloadsAllEntities() {
        PortfolioMasterId masterId = new PortfolioMasterId("PORT0001", "ACCT000001");

        sessionFactory.inTransaction(session -> {
            PortfolioMaster master = new PortfolioMaster();
            master.setId(masterId);
            master.setClientName("JOHN DOE");
            master.setClientType("I");
            master.setCreateDate(LocalDate.of(2024, 1, 15));
            master.setLastMaintDate(LocalDate.of(2024, 6, 1));
            master.setStatus("A");
            master.setTotalValue(new BigDecimal("1234567890123.45"));
            master.setCashBalance(new BigDecimal("-9876543.21"));
            master.setLastMaintUser("OPER01");
            master.setLastTransNo(12345678L);
            session.persist(master);

            PortfolioTransaction transaction = new PortfolioTransaction();
            transaction.setId(new PortfolioTransactionId(
                    LocalDate.of(2024, 6, 1), LocalTime.of(9, 30, 0), "PORT0001", "000001"));
            transaction.setInvestmentId("INVEST0001");
            transaction.setTransType("BU");
            transaction.setQuantity(new BigDecimal("100.2500"));
            transaction.setPrice(new BigDecimal("12345678901.2345"));
            transaction.setAmount(new BigDecimal("1237354.04"));
            transaction.setCurrencyCode("USD");
            transaction.setStatus("D");
            transaction.setProcessDate(LocalDateTime.of(2024, 6, 1, 9, 30, 5));
            transaction.setProcessUser("OPER01");
            session.persist(transaction);

            PortfolioPosition position = new PortfolioPosition();
            position.setId(new PortfolioPositionId("PORT0001", LocalDate.of(2024, 6, 1), "INVEST0001"));
            position.setQuantity(new BigDecimal("100.2500"));
            position.setCostBasis(new BigDecimal("1237354.04"));
            position.setMarketValue(new BigDecimal("1300000.00"));
            position.setCurrencyCode("USD");
            position.setStatus("A");
            position.setLastMaintDate(LocalDateTime.of(2024, 6, 1, 18, 0, 0));
            position.setLastMaintUser("BATCH01");
            session.persist(position);

            HistoryRecord history = new HistoryRecord();
            history.setId(new HistoryRecordId(
                    "PORT0001", LocalDate.of(2024, 6, 1), LocalTime.of(9, 30, 0), "0001"));
            history.setRecordType("TR");
            history.setActionCode("A");
            history.setBeforeImage(null);
            history.setAfterImage("AFTER-IMAGE-DATA");
            history.setReasonCode("RC01");
            history.setProcessDate(LocalDateTime.of(2024, 6, 1, 9, 30, 5));
            history.setProcessUser("OPER01");
            session.persist(history);

            ErrorLog errorLog = new ErrorLog();
            errorLog.setErrorDate(LocalDate.of(2024, 6, 1));
            errorLog.setErrorTime(LocalTime.of(10, 15, 30));
            errorLog.setProgramId("PORTTRAN");
            errorLog.setErrorCategory("VS");
            errorLog.setErrorCode("E001");
            errorLog.setErrorSeverity((short) 8);
            errorLog.setErrorText("VSAM RECORD NOT FOUND");
            errorLog.setErrorDetails("KEY=PORT0001ACCT000001");
            session.persist(errorLog);

            AuditLog auditLog = new AuditLog();
            auditLog.setAuditTimestamp(LocalDateTime.of(2024, 6, 1, 9, 30, 5));
            auditLog.setSystemId("CICSPROD");
            auditLog.setUserId("OPER01");
            auditLog.setProgramId("PORTADD");
            auditLog.setTerminalId("TERM0001");
            auditLog.setAuditType("TRAN");
            auditLog.setAuditAction("CREATE");
            auditLog.setAuditStatus("SUCC");
            auditLog.setPortfolioId("PORT0001");
            auditLog.setAccountNo("ACCT000001");
            auditLog.setBeforeImage(null);
            auditLog.setAfterImage("NEW PORTFOLIO");
            auditLog.setAuditMessage("PORTFOLIO CREATED");
            session.persist(auditLog);
        });

        sessionFactory.inTransaction(session -> {
            PortfolioMaster master = session.find(PortfolioMaster.class, masterId);
            assertThat(master).isNotNull();
            assertThat(master.getClientName()).isEqualTo("JOHN DOE");
            assertThat(master.getTotalValue()).isEqualByComparingTo("1234567890123.45");
            assertThat(master.getCashBalance()).isEqualByComparingTo("-9876543.21");
            assertThat(master.getLastTransNo()).isEqualTo(12345678L);

            PortfolioTransaction transaction = session.find(PortfolioTransaction.class,
                    new PortfolioTransactionId(LocalDate.of(2024, 6, 1), LocalTime.of(9, 30, 0),
                            "PORT0001", "000001"));
            assertThat(transaction).isNotNull();
            assertThat(transaction.getQuantity()).isEqualByComparingTo("100.2500");
            assertThat(transaction.getPrice()).isEqualByComparingTo("12345678901.2345");
            assertThat(transaction.getTransType()).isEqualTo("BU");

            PortfolioPosition position = session.find(PortfolioPosition.class,
                    new PortfolioPositionId("PORT0001", LocalDate.of(2024, 6, 1), "INVEST0001"));
            assertThat(position).isNotNull();
            assertThat(position.getMarketValue()).isEqualByComparingTo("1300000.00");

            HistoryRecord history = session.find(HistoryRecord.class,
                    new HistoryRecordId("PORT0001", LocalDate.of(2024, 6, 1),
                            LocalTime.of(9, 30, 0), "0001"));
            assertThat(history).isNotNull();
            assertThat(history.getRecordType()).isEqualTo("TR");
            assertThat(history.getAfterImage()).isEqualTo("AFTER-IMAGE-DATA");

            ErrorLog errorLog = session
                    .createQuery("from ErrorLog where programId = 'PORTTRAN'", ErrorLog.class)
                    .getSingleResult();
            assertThat(errorLog.getErrorLogId()).isNotNull();
            assertThat(errorLog.getErrorSeverity()).isEqualTo((short) 8);

            AuditLog auditLog = session
                    .createQuery("from AuditLog where userId = 'OPER01'", AuditLog.class)
                    .getSingleResult();
            assertThat(auditLog.getAuditLogId()).isNotNull();
            assertThat(auditLog.getAuditAction().trim()).isEqualTo("CREATE");
        });
    }
}
