# Phase 2: Batch Processing Migration - Detailed Implementation Plan

Version: 1.0  
Last Updated: 2026-01-15  
Duration: 4-5 months (Weeks 1-20)

## Executive Summary

This document provides a detailed implementation plan for Phase 2 of the COBOL Legacy Benchmark Suite migration, focusing on the Batch Processing layer. Building on the foundation established in Phase 1 (Database Migration, Copybook Modernization, and ORM Layer Implementation), this phase migrates the core batch processing programs to a modern Java/Spring Batch architecture while preserving critical checkpoint/restart capabilities and error handling patterns.

## Table of Contents

1. [Overview and Objectives](#1-overview-and-objectives)
2. [Source System Analysis](#2-source-system-analysis)
3. [Target Architecture](#3-target-architecture)
4. [Migration Strategy by Program](#4-migration-strategy-by-program)
5. [Batch Control Framework Migration](#5-batch-control-framework-migration)
6. [Checkpoint/Restart Modernization](#6-checkpointrestart-modernization)
7. [Error Handling Modernization](#7-error-handling-modernization)
8. [Week-by-Week Implementation Timeline](#8-week-by-week-implementation-timeline)
9. [Risk Mitigation Strategies](#9-risk-mitigation-strategies)
10. [Testing Strategy](#10-testing-strategy)
11. [Appendices](#11-appendices)

---

## 1. Overview and Objectives

### 1.1 Phase 2 Scope

Phase 2 encompasses the migration of six core batch programs and their supporting infrastructure:

| Program | Description | Lines of Code | Complexity |
|---------|-------------|---------------|------------|
| TRNVAL00 | Transaction Validation | ~300 | Medium |
| POSUPD00 | Position Updates | ~350 | High |
| HISTLD00 | History Loading to DB2 | 234 | Medium |
| BCHCTL00 | Batch Controller | 128 | High |
| PRCSEQ00 | Process Sequencer | 346 | High |
| RCVPRC00 | Recovery Processor | 303 | High |

### 1.2 Key Objectives

The migration must achieve the following objectives:

**Functional Parity**: All business logic from the COBOL programs must be preserved exactly, including validation rules, calculation logic, and processing sequences.

**Checkpoint/Restart Preservation**: The ability to restart failed batch jobs from the last successful checkpoint must be maintained, ensuring no data loss or duplicate processing.

**Dependency Management**: The process sequencing and dependency checking capabilities must be modernized while maintaining the same logical flow.

**Error Handling Enhancement**: The ERRPROC error handling must be converted to modern exception handling with improved logging, alerting, and recovery capabilities.

**Performance Optimization**: The migrated batch jobs should perform at least as well as the original COBOL programs, with opportunities for parallel processing where appropriate.

### 1.3 Dependencies on Phase 1

This phase assumes the following Phase 1 deliverables are complete:

- Database schema migrated to PostgreSQL/Oracle with equivalent table structures
- Copybook data structures converted to Java POJOs/entities
- ORM layer (JPA/Hibernate) implemented for all database tables
- VSAM file access patterns converted to database operations

---

## 2. Source System Analysis

### 2.1 Batch Processing Flow

The current COBOL batch processing follows this sequence:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  TRNVAL00   │────▶│  POSUPD00   │────▶│  HISTLD00   │
│ Transaction │     │  Position   │     │   History   │
│ Validation  │     │   Updates   │     │    Load     │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────┐
│                    BCHCTL00                         │
│              Batch Control Processor                │
│  (Status Management, Prerequisite Checking)         │
└─────────────────────────────────────────────────────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────┐
│                    PRCSEQ00                         │
│              Process Sequence Manager               │
│  (Dependency Resolution, Process Scheduling)        │
└─────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│                    RCVPRC00                         │
│              Recovery Processor                     │
│  (Restart, Bypass, Terminate Actions)              │
└─────────────────────────────────────────────────────┘
```

### 2.2 Current Program Analysis

#### 2.2.1 TRNVAL00 - Transaction Validation

**Purpose**: Validates incoming financial transactions before processing.

**Current Implementation Pattern** (based on PORTTRAN.cbl):

```cobol
       2100-VALIDATE-TRANSACTION.
           MOVE SPACES TO ERR-TEXT
           
           PERFORM 2110-CHECK-PORTFOLIO
           IF ERR-TEXT = SPACES
               PERFORM 2120-CHECK-TRANSACTION-TYPE
           END-IF
           IF ERR-TEXT = SPACES
               PERFORM 2130-CHECK-AMOUNTS
           END-IF
           
           IF ERR-TEXT = SPACES
               ADD 1 TO WS-PROCESS-COUNT
           ELSE
               PERFORM 9000-ERROR-ROUTINE
           END-IF
           .
```

**Validation Rules**:
- Portfolio ID must exist and be valid
- Transaction type must be BU (Buy), SL (Sell), TR (Transfer), or FE (Fee)
- Quantity must be greater than zero
- Price must be greater than zero (except for transfers)
- Amount must be greater than zero (except for transfers)

**Key Data Structures**:
- Input: TRANSACTION-RECORD (TRNREC.cpy)
- Output: Validated transactions, error records

#### 2.2.2 POSUPD00 - Position Updates

**Purpose**: Updates portfolio positions based on validated transactions.

**Processing Logic**:
- BUY: Add quantity to position, add amount to cost basis
- SELL: Subtract quantity from position, calculate gain/loss
- TRANSFER: Move position between portfolios
- FEE: Subtract fee amount from cost basis

**Key Considerations**:
- Must maintain referential integrity
- Requires checkpoint after each N records
- Must handle insufficient balance scenarios

#### 2.2.3 HISTLD00 - History Loading

**Purpose**: Loads transaction history from VSAM to DB2 for reporting.

**Current Implementation**:

```cobol
       2200-LOAD-TO-DB2.
           INITIALIZE POSHIST-RECORD
           
           MOVE TH-ACCOUNT-NO    TO PH-ACCOUNT-NO
           MOVE TH-PORTFOLIO-ID  TO PH-PORTFOLIO-ID
           ...
           
           EXEC SQL
               INSERT INTO POSHIST
               VALUES (:POSHIST-RECORD)
           END-EXEC
           
           IF SQLCODE = 0
               ADD 1 TO WS-RECORDS-WRITTEN
           ELSE
               IF SQLCODE = -803
                   CONTINUE
               ELSE
                   ADD 1 TO WS-ERROR-COUNT
                   PERFORM DB2-ERROR-ROUTINE
               END-IF
           END-IF
           .
```

**Commit Strategy**:
- Commits every 1000 records (WS-COMMIT-THRESHOLD)
- Updates checkpoint after each commit
- Rolls back on error

#### 2.2.4 BCHCTL00 - Batch Controller

**Purpose**: Manages job-level control and process sequencing.

**Functions**:
| Function | Description |
|----------|-------------|
| INIT | Initialize process, open files, validate |
| CHEK | Check prerequisites are satisfied |
| UPDT | Update process status |
| TERM | Terminate process, close files |

**Control Record Structure** (BCHCTL.cpy):

```cobol
       01  BATCH-CONTROL-RECORD.
           05  BCT-KEY.
               10  BCT-JOB-NAME      PIC X(8).
               10  BCT-PROCESS-DATE  PIC X(8).
               10  BCT-SEQUENCE-NO   PIC 9(4).
           05  BCT-DATA.
               10  BCT-STATUS        PIC X(1).
                   88  BCT-STATUS-READY    VALUE 'R'.
                   88  BCT-STATUS-ACTIVE   VALUE 'A'.
                   88  BCT-STATUS-WAITING  VALUE 'W'.
                   88  BCT-STATUS-DONE     VALUE 'D'.
                   88  BCT-STATUS-ERROR    VALUE 'E'.
               10  BCT-PROCESS-CONTROL.
                   15  BCT-STEP-NAME    PIC X(8).
                   15  BCT-PROGRAM-NAME PIC X(8).
                   15  BCT-START-TIME   PIC X(8).
                   15  BCT-END-TIME     PIC X(8).
               10  BCT-DEPENDENCIES.
                   15  BCT-PREREQ-COUNT PIC 9(2) COMP.
                   15  BCT-PREREQ-JOBS  OCCURS 10 TIMES.
                       20  BCT-PREREQ-NAME  PIC X(8).
                       20  BCT-PREREQ-SEQ   PIC 9(4).
                       20  BCT-PREREQ-RC    PIC S9(4) COMP.
```

#### 2.2.5 PRCSEQ00 - Process Sequencer

**Purpose**: Manages process dependencies and execution order.

**Key Features**:
- Builds process sequence from definition file
- Creates control records for each process
- Checks hard and soft dependencies
- Tracks active and error counts

**Dependency Types**:
- Hard (H): Must complete successfully before dependent process starts
- Soft (S): Should complete but not blocking

#### 2.2.6 RCVPRC00 - Recovery Processor

**Purpose**: Handles recovery from failed batch processes.

**Recovery Modes**:
| Mode | Description |
|------|-------------|
| P (Process) | Recover single process |
| S (Sequence) | Recover all processes for a date |
| A (All) | Recover all failed processes |

**Recovery Actions**:
| Action | Description |
|--------|-------------|
| Restart | Reset status to READY, increment restart count |
| Bypass | Mark as DONE with WARNING return code |
| Terminate | Mark as ERROR, stop processing |

---

## 3. Target Architecture

### 3.1 Technology Stack

| Component | Technology | Rationale |
|-----------|------------|-----------|
| Batch Framework | Spring Batch 5.x | Industry standard, robust checkpoint/restart |
| Workflow Orchestration | Temporal.io | Modern, durable workflow execution |
| Database Access | Spring Data JPA | Consistent with Phase 1 ORM layer |
| Messaging | Apache Kafka | Event-driven processing, audit trail |
| Monitoring | Micrometer + Prometheus | Comprehensive metrics and alerting |
| Logging | SLF4J + Logback | Structured logging with correlation IDs |

### 3.2 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Workflow Orchestration Layer                  │
│                         (Temporal.io)                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Daily Batch  │  │  Recovery    │  │   Ad-hoc     │          │
│  │  Workflow    │  │  Workflow    │  │  Workflow    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Batch Jobs Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Transaction  │  │  Position    │  │   History    │          │
│  │ Validation   │  │   Update     │  │    Load      │          │
│  │    Job       │  │    Job       │  │    Job       │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Service Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Validation   │  │  Position    │  │   History    │          │
│  │   Service    │  │   Service    │  │   Service    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Batch      │  │  Recovery    │  │    Error     │          │
│  │  Control     │  │   Service    │  │   Handler    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Data Access Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Transaction  │  │  Position    │  │   History    │          │
│  │ Repository   │  │ Repository   │  │ Repository   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐                            │
│  │BatchControl  │  │  Process     │                            │
│  │ Repository   │  │  Sequence    │                            │
│  └──────────────┘  └──────────────┘                            │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 Package Structure

```
com.portfolio.batch
├── config/
│   ├── BatchConfiguration.java
│   ├── TemporalConfiguration.java
│   └── DataSourceConfiguration.java
├── job/
│   ├── transaction/
│   │   ├── TransactionValidationJob.java
│   │   ├── TransactionItemReader.java
│   │   ├── TransactionItemProcessor.java
│   │   └── TransactionItemWriter.java
│   ├── position/
│   │   ├── PositionUpdateJob.java
│   │   ├── PositionItemReader.java
│   │   ├── PositionItemProcessor.java
│   │   └── PositionItemWriter.java
│   └── history/
│       ├── HistoryLoadJob.java
│       ├── HistoryItemReader.java
│       ├── HistoryItemProcessor.java
│       └── HistoryItemWriter.java
├── workflow/
│   ├── DailyBatchWorkflow.java
│   ├── DailyBatchWorkflowImpl.java
│   ├── RecoveryWorkflow.java
│   └── RecoveryWorkflowImpl.java
├── service/
│   ├── BatchControlService.java
│   ├── ProcessSequenceService.java
│   ├── RecoveryService.java
│   └── ValidationService.java
├── domain/
│   ├── entity/
│   │   ├── Transaction.java
│   │   ├── Position.java
│   │   ├── PositionHistory.java
│   │   ├── BatchControl.java
│   │   └── ProcessSequence.java
│   └── enums/
│       ├── BatchStatus.java
│       ├── TransactionType.java
│       └── RecoveryAction.java
├── repository/
│   ├── TransactionRepository.java
│   ├── PositionRepository.java
│   ├── PositionHistoryRepository.java
│   ├── BatchControlRepository.java
│   └── ProcessSequenceRepository.java
├── exception/
│   ├── BatchProcessingException.java
│   ├── ValidationException.java
│   ├── RecoveryException.java
│   └── GlobalExceptionHandler.java
└── listener/
    ├── JobExecutionListener.java
    ├── StepExecutionListener.java
    └── ChunkListener.java
```

---

## 4. Migration Strategy by Program

### 4.1 TRNVAL00 - Transaction Validation Migration

#### 4.1.1 COBOL Source Analysis

The transaction validation program performs the following checks:
1. Portfolio existence validation
2. Transaction type validation
3. Amount and quantity validation
4. Business rule validation

#### 4.1.2 Target Implementation

**Entity Class**:

```java
@Entity
@Table(name = "TRANSACTION")
public class Transaction {
    
    @EmbeddedId
    private TransactionKey key;
    
    @Column(name = "INVESTMENT_ID", length = 10)
    private String investmentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "TRANS_TYPE", length = 2)
    private TransactionType transactionType;
    
    @Column(name = "QUANTITY", precision = 15, scale = 4)
    private BigDecimal quantity;
    
    @Column(name = "PRICE", precision = 15, scale = 4)
    private BigDecimal price;
    
    @Column(name = "AMOUNT", precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "CURRENCY", length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 1)
    private TransactionStatus status;
    
    @Column(name = "PROCESS_DATE")
    private LocalDateTime processDate;
    
    @Column(name = "PROCESS_USER", length = 8)
    private String processUser;
}

@Embeddable
public class TransactionKey implements Serializable {
    
    @Column(name = "TRANS_DATE", length = 8)
    private String transDate;
    
    @Column(name = "TRANS_TIME", length = 6)
    private String transTime;
    
    @Column(name = "PORTFOLIO_ID", length = 8)
    private String portfolioId;
    
    @Column(name = "SEQUENCE_NO", length = 6)
    private String sequenceNo;
}
```

**Validation Service**:

```java
@Service
@Slf4j
public class TransactionValidationService {
    
    private final PortfolioRepository portfolioRepository;
    private final ValidationRulesEngine rulesEngine;
    
    public TransactionValidationService(
            PortfolioRepository portfolioRepository,
            ValidationRulesEngine rulesEngine) {
        this.portfolioRepository = portfolioRepository;
        this.rulesEngine = rulesEngine;
    }
    
    /**
     * Validates a transaction record.
     * Equivalent to COBOL 2100-VALIDATE-TRANSACTION.
     */
    public ValidationResult validate(Transaction transaction) {
        ValidationResult result = new ValidationResult();
        
        // 2110-CHECK-PORTFOLIO equivalent
        validatePortfolio(transaction, result);
        if (result.hasErrors()) {
            return result;
        }
        
        // 2120-CHECK-TRANSACTION-TYPE equivalent
        validateTransactionType(transaction, result);
        if (result.hasErrors()) {
            return result;
        }
        
        // 2130-CHECK-AMOUNTS equivalent
        validateAmounts(transaction, result);
        
        return result;
    }
    
    /**
     * Equivalent to COBOL 2110-CHECK-PORTFOLIO.
     */
    private void validatePortfolio(Transaction transaction, 
                                   ValidationResult result) {
        String portfolioId = transaction.getKey().getPortfolioId();
        
        if (portfolioId == null || portfolioId.isBlank()) {
            result.addError("E001", "Portfolio ID is required");
            return;
        }
        
        if (!portfolioRepository.existsById(portfolioId)) {
            result.addError("E002", 
                String.format("Invalid Portfolio ID: %s", portfolioId));
        }
    }
    
    /**
     * Equivalent to COBOL 2120-CHECK-TRANSACTION-TYPE.
     */
    private void validateTransactionType(Transaction transaction,
                                         ValidationResult result) {
        TransactionType type = transaction.getTransactionType();
        
        if (type == null) {
            result.addError("E003", "Transaction type is required");
            return;
        }
        
        if (!EnumSet.of(TransactionType.BUY, TransactionType.SELL,
                        TransactionType.TRANSFER, TransactionType.FEE)
                    .contains(type)) {
            result.addError("E003", 
                String.format("Invalid Transaction Type: %s", type));
        }
    }
    
    /**
     * Equivalent to COBOL 2130-CHECK-AMOUNTS.
     */
    private void validateAmounts(Transaction transaction,
                                 ValidationResult result) {
        BigDecimal quantity = transaction.getQuantity();
        BigDecimal price = transaction.getPrice();
        BigDecimal amount = transaction.getAmount();
        TransactionType type = transaction.getTransactionType();
        
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("E004", 
                "Quantity must be greater than zero");
            return;
        }
        
        // Price validation (not required for transfers)
        if (type != TransactionType.TRANSFER) {
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                result.addError("E005", 
                    "Price must be greater than zero");
                return;
            }
            
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                result.addError("E006", 
                    "Amount must be greater than zero");
            }
        }
    }
}
```

**Spring Batch Job Configuration**:

```java
@Configuration
@EnableBatchProcessing
public class TransactionValidationJobConfig {
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionValidationService validationService;
    private final DataSource dataSource;
    
    @Bean
    public Job transactionValidationJob(
            Step transactionValidationStep,
            JobExecutionListener jobListener) {
        return new JobBuilder("transactionValidationJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(jobListener)
            .start(transactionValidationStep)
            .build();
    }
    
    @Bean
    public Step transactionValidationStep(
            ItemReader<Transaction> reader,
            ItemProcessor<Transaction, Transaction> processor,
            ItemWriter<Transaction> writer,
            StepExecutionListener stepListener) {
        return new StepBuilder("transactionValidationStep", jobRepository)
            .<Transaction, Transaction>chunk(1000, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .listener(stepListener)
            .faultTolerant()
            .skipLimit(100)
            .skip(ValidationException.class)
            .retryLimit(3)
            .retry(TransientDataAccessException.class)
            .build();
    }
    
    @Bean
    @StepScope
    public JdbcCursorItemReader<Transaction> transactionReader(
            @Value("#{jobParameters['processDate']}") String processDate) {
        return new JdbcCursorItemReaderBuilder<Transaction>()
            .name("transactionReader")
            .dataSource(dataSource)
            .sql("""
                SELECT * FROM TRANSACTION 
                WHERE TRANS_DATE = ? 
                AND STATUS = 'P'
                ORDER BY TRANS_TIME, SEQUENCE_NO
                """)
            .preparedStatementSetter(ps -> ps.setString(1, processDate))
            .rowMapper(new TransactionRowMapper())
            .build();
    }
    
    @Bean
    public ItemProcessor<Transaction, Transaction> transactionProcessor() {
        return transaction -> {
            ValidationResult result = validationService.validate(transaction);
            
            if (result.hasErrors()) {
                transaction.setStatus(TransactionStatus.FAILED);
                throw new ValidationException(result.getErrors());
            }
            
            transaction.setStatus(TransactionStatus.VALIDATED);
            return transaction;
        };
    }
    
    @Bean
    public JdbcBatchItemWriter<Transaction> transactionWriter() {
        return new JdbcBatchItemWriterBuilder<Transaction>()
            .dataSource(dataSource)
            .sql("""
                UPDATE TRANSACTION 
                SET STATUS = :status, 
                    PROCESS_DATE = :processDate,
                    PROCESS_USER = :processUser
                WHERE TRANS_DATE = :key.transDate 
                AND TRANS_TIME = :key.transTime
                AND PORTFOLIO_ID = :key.portfolioId
                AND SEQUENCE_NO = :key.sequenceNo
                """)
            .beanMapped()
            .build();
    }
}
```

### 4.2 POSUPD00 - Position Update Migration

#### 4.2.1 COBOL Source Analysis

The position update program processes validated transactions and updates portfolio positions:

```cobol
       2200-UPDATE-POSITIONS.
           EVALUATE TRN-TYPE
               WHEN 'BU'
                   PERFORM 2210-PROCESS-BUY
               WHEN 'SL'
                   PERFORM 2220-PROCESS-SELL
               WHEN 'TR'
                   PERFORM 2230-PROCESS-TRANSFER
               WHEN 'FE'
                   PERFORM 2240-PROCESS-FEE
           END-EVALUATE
           
           PERFORM 2300-UPDATE-AUDIT-TRAIL
           .
```

#### 4.2.2 Target Implementation

**Position Service**:

```java
@Service
@Slf4j
@Transactional
public class PositionUpdateService {
    
    private final PositionRepository positionRepository;
    private final AuditService auditService;
    
    /**
     * Updates position based on transaction type.
     * Equivalent to COBOL 2200-UPDATE-POSITIONS.
     */
    public Position updatePosition(Transaction transaction) {
        return switch (transaction.getTransactionType()) {
            case BUY -> processBuy(transaction);
            case SELL -> processSell(transaction);
            case TRANSFER -> processTransfer(transaction);
            case FEE -> processFee(transaction);
        };
    }
    
    /**
     * Equivalent to COBOL 2210-PROCESS-BUY.
     */
    private Position processBuy(Transaction transaction) {
        String portfolioId = transaction.getKey().getPortfolioId();
        String investmentId = transaction.getInvestmentId();
        
        Position position = positionRepository
            .findByPortfolioIdAndInvestmentId(portfolioId, investmentId)
            .orElseGet(() -> createNewPosition(portfolioId, investmentId));
        
        // Add quantity to position
        position.setTotalUnits(
            position.getTotalUnits().add(transaction.getQuantity()));
        
        // Add amount to cost basis
        position.setTotalCost(
            position.getTotalCost().add(transaction.getAmount()));
        
        // Recalculate average cost
        if (position.getTotalUnits().compareTo(BigDecimal.ZERO) > 0) {
            position.setAverageCost(
                position.getTotalCost().divide(
                    position.getTotalUnits(), 
                    4, RoundingMode.HALF_UP));
        }
        
        position.setLastTransactionDate(LocalDateTime.now());
        position.setLastTransactionId(buildTransactionId(transaction));
        
        return positionRepository.save(position);
    }
    
    /**
     * Equivalent to COBOL 2220-PROCESS-SELL.
     */
    private Position processSell(Transaction transaction) {
        String portfolioId = transaction.getKey().getPortfolioId();
        String investmentId = transaction.getInvestmentId();
        
        Position position = positionRepository
            .findByPortfolioIdAndInvestmentId(portfolioId, investmentId)
            .orElseThrow(() -> new PositionNotFoundException(
                "Position not found for portfolio: " + portfolioId));
        
        // Check sufficient balance
        if (position.getTotalUnits().compareTo(transaction.getQuantity()) < 0) {
            throw new InsufficientBalanceException(
                String.format("Insufficient units for sale. Available: %s, Requested: %s",
                    position.getTotalUnits(), transaction.getQuantity()));
        }
        
        // Calculate gain/loss before updating
        BigDecimal costBasisSold = position.getAverageCost()
            .multiply(transaction.getQuantity());
        BigDecimal gainLoss = transaction.getAmount().subtract(costBasisSold);
        
        // Subtract quantity from position
        position.setTotalUnits(
            position.getTotalUnits().subtract(transaction.getQuantity()));
        
        // Subtract proportional cost basis
        position.setTotalCost(
            position.getTotalCost().subtract(costBasisSold));
        
        position.setRealizedGainLoss(
            position.getRealizedGainLoss().add(gainLoss));
        
        position.setLastTransactionDate(LocalDateTime.now());
        position.setLastTransactionId(buildTransactionId(transaction));
        
        return positionRepository.save(position);
    }
    
    /**
     * Equivalent to COBOL 2230-PROCESS-TRANSFER.
     */
    private Position processTransfer(Transaction transaction) {
        // Transfer requires source and destination portfolios
        // Implementation depends on transfer record structure
        throw new UnsupportedOperationException(
            "Transfer processing requires additional implementation");
    }
    
    /**
     * Equivalent to COBOL 2240-PROCESS-FEE.
     */
    private Position processFee(Transaction transaction) {
        String portfolioId = transaction.getKey().getPortfolioId();
        String investmentId = transaction.getInvestmentId();
        
        Position position = positionRepository
            .findByPortfolioIdAndInvestmentId(portfolioId, investmentId)
            .orElseThrow(() -> new PositionNotFoundException(
                "Position not found for fee: " + portfolioId));
        
        // Subtract fee from cost basis
        position.setTotalCost(
            position.getTotalCost().subtract(transaction.getAmount()));
        
        position.setLastTransactionDate(LocalDateTime.now());
        position.setLastTransactionId(buildTransactionId(transaction));
        
        return positionRepository.save(position);
    }
}
```

**Position Update Job**:

```java
@Configuration
public class PositionUpdateJobConfig {
    
    @Bean
    public Job positionUpdateJob(
            JobRepository jobRepository,
            Step positionUpdateStep,
            JobExecutionListener jobListener) {
        return new JobBuilder("positionUpdateJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(jobListener)
            .start(positionUpdateStep)
            .build();
    }
    
    @Bean
    public Step positionUpdateStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Transaction> validatedTransactionReader,
            ItemProcessor<Transaction, PositionUpdate> positionProcessor,
            ItemWriter<PositionUpdate> positionWriter,
            CheckpointListener checkpointListener) {
        return new StepBuilder("positionUpdateStep", jobRepository)
            .<Transaction, PositionUpdate>chunk(500, transactionManager)
            .reader(validatedTransactionReader)
            .processor(positionProcessor)
            .writer(positionWriter)
            .listener(checkpointListener)
            .faultTolerant()
            .skipLimit(50)
            .skip(PositionNotFoundException.class)
            .retryLimit(3)
            .retry(OptimisticLockingFailureException.class)
            .build();
    }
}
```

### 4.3 HISTLD00 - History Load Migration

#### 4.3.1 COBOL Source Analysis

The history load program transfers data from VSAM to DB2 with commit control:

```cobol
       2300-CHECK-COMMIT.
           ADD 1 TO WS-COMMIT-COUNT
           
           IF WS-COMMIT-COUNT >= WS-COMMIT-THRESHOLD
               EXEC SQL
                   COMMIT WORK
               END-EXEC
               
               MOVE 0 TO WS-COMMIT-COUNT
               
               PERFORM 2310-UPDATE-CHECKPOINT
           END-IF
           .
```

#### 4.3.2 Target Implementation

**History Load Job with Chunk Processing**:

```java
@Configuration
public class HistoryLoadJobConfig {
    
    private static final int COMMIT_INTERVAL = 1000;
    
    @Bean
    public Job historyLoadJob(
            JobRepository jobRepository,
            Step historyLoadStep,
            JobExecutionListener jobListener) {
        return new JobBuilder("historyLoadJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(jobListener)
            .start(historyLoadStep)
            .build();
    }
    
    @Bean
    public Step historyLoadStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<TransactionHistory> historyReader,
            ItemProcessor<TransactionHistory, PositionHistory> historyProcessor,
            ItemWriter<PositionHistory> historyWriter,
            SkipListener<TransactionHistory, PositionHistory> skipListener) {
        return new StepBuilder("historyLoadStep", jobRepository)
            .<TransactionHistory, PositionHistory>chunk(
                COMMIT_INTERVAL, transactionManager)
            .reader(historyReader)
            .processor(historyProcessor)
            .writer(historyWriter)
            .listener(skipListener)
            .faultTolerant()
            .skipLimit(Integer.MAX_VALUE)
            .skip(DuplicateKeyException.class) // SQLCODE -803 equivalent
            .noRollback(DuplicateKeyException.class)
            .build();
    }
    
    @Bean
    @StepScope
    public JdbcCursorItemReader<TransactionHistory> historyReader(
            DataSource dataSource,
            @Value("#{jobParameters['processDate']}") String processDate) {
        return new JdbcCursorItemReaderBuilder<TransactionHistory>()
            .name("historyReader")
            .dataSource(dataSource)
            .sql("""
                SELECT * FROM TRANSACTION_HISTORY 
                WHERE HIST_DATE = ?
                ORDER BY HIST_TIME, HIST_SEQ_NO
                """)
            .preparedStatementSetter(ps -> ps.setString(1, processDate))
            .rowMapper(new TransactionHistoryRowMapper())
            .build();
    }
    
    @Bean
    public ItemProcessor<TransactionHistory, PositionHistory> historyProcessor() {
        return history -> {
            PositionHistory posHistory = new PositionHistory();
            
            // Map fields - equivalent to COBOL 2200-LOAD-TO-DB2
            posHistory.setAccountNo(history.getAccountNo());
            posHistory.setPortfolioId(history.getPortfolioId());
            posHistory.setTransDate(history.getTransDate());
            posHistory.setTransTime(history.getTransTime());
            posHistory.setTransType(history.getTransType());
            posHistory.setSecurityId(history.getSecurityId());
            posHistory.setQuantity(history.getQuantity());
            posHistory.setPrice(history.getPrice());
            posHistory.setAmount(history.getAmount());
            posHistory.setFees(history.getFees());
            posHistory.setTotalAmount(history.getTotalAmount());
            posHistory.setCostBasis(history.getCostBasis());
            posHistory.setGainLoss(history.getGainLoss());
            posHistory.setProcessTimestamp(LocalDateTime.now());
            
            return posHistory;
        };
    }
    
    @Bean
    public JdbcBatchItemWriter<PositionHistory> historyWriter(
            DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<PositionHistory>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO POSHIST (
                    ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME,
                    TRANS_TYPE, SECURITY_ID, QUANTITY, PRICE, AMOUNT,
                    FEES, TOTAL_AMOUNT, COST_BASIS, GAIN_LOSS, 
                    PROC_TIMESTAMP
                ) VALUES (
                    :accountNo, :portfolioId, :transDate, :transTime,
                    :transType, :securityId, :quantity, :price, :amount,
                    :fees, :totalAmount, :costBasis, :gainLoss,
                    :processTimestamp
                )
                """)
            .beanMapped()
            .build();
    }
}
```

---

## 5. Batch Control Framework Migration

### 5.1 Overview

The batch control framework (BCHCTL00 and PRCSEQ00) manages job execution, dependencies, and status tracking. This will be migrated to a combination of Spring Batch metadata and Temporal.io workflow orchestration.

### 5.2 Batch Control Entity

```java
@Entity
@Table(name = "BATCH_CONTROL")
public class BatchControl {
    
    @EmbeddedId
    private BatchControlKey key;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 1)
    private BatchStatus status;
    
    @Column(name = "STEP_NAME", length = 8)
    private String stepName;
    
    @Column(name = "PROGRAM_NAME", length = 8)
    private String programName;
    
    @Column(name = "START_TIME")
    private LocalDateTime startTime;
    
    @Column(name = "END_TIME")
    private LocalDateTime endTime;
    
    @Column(name = "RETURN_CODE")
    private Integer returnCode;
    
    @Column(name = "ERROR_DESC", length = 80)
    private String errorDescription;
    
    @Column(name = "RESTART_COUNT")
    private Integer restartCount;
    
    @Column(name = "RECORDS_READ")
    private Long recordsRead;
    
    @Column(name = "RECORDS_WRITTEN")
    private Long recordsWritten;
    
    @Column(name = "ERROR_COUNT")
    private Long errorCount;
    
    @OneToMany(mappedBy = "batchControl", cascade = CascadeType.ALL)
    private List<BatchPrerequisite> prerequisites;
}

@Embeddable
public class BatchControlKey implements Serializable {
    
    @Column(name = "JOB_NAME", length = 8)
    private String jobName;
    
    @Column(name = "PROCESS_DATE", length = 8)
    private String processDate;
    
    @Column(name = "SEQUENCE_NO")
    private Integer sequenceNo;
}

public enum BatchStatus {
    READY('R'),
    ACTIVE('A'),
    WAITING('W'),
    DONE('D'),
    ERROR('E');
    
    private final char code;
    
    BatchStatus(char code) {
        this.code = code;
    }
}
```

### 5.3 Batch Control Service

```java
@Service
@Slf4j
@Transactional
public class BatchControlService {
    
    private final BatchControlRepository batchControlRepository;
    private final ProcessSequenceRepository processSequenceRepository;
    
    /**
     * Initializes batch control for a process.
     * Equivalent to COBOL BCHCTL00 FUNC-INIT.
     */
    public BatchControl initializeProcess(String jobName, 
                                          String processDate,
                                          int sequenceNo) {
        log.info("Initializing batch control for job: {}, date: {}, seq: {}",
            jobName, processDate, sequenceNo);
        
        BatchControlKey key = new BatchControlKey(jobName, processDate, sequenceNo);
        
        BatchControl control = batchControlRepository.findById(key)
            .orElseGet(() -> createNewControl(key));
        
        validateProcess(control);
        
        control.setStatus(BatchStatus.ACTIVE);
        control.setStartTime(LocalDateTime.now());
        control.setRestartCount(control.getRestartCount() + 1);
        
        return batchControlRepository.save(control);
    }
    
    /**
     * Checks if prerequisites are satisfied.
     * Equivalent to COBOL BCHCTL00 FUNC-CHEK.
     */
    public boolean checkPrerequisites(BatchControl control) {
        log.info("Checking prerequisites for job: {}", 
            control.getKey().getJobName());
        
        List<BatchPrerequisite> prerequisites = control.getPrerequisites();
        
        if (prerequisites == null || prerequisites.isEmpty()) {
            return true;
        }
        
        for (BatchPrerequisite prereq : prerequisites) {
            BatchControlKey prereqKey = new BatchControlKey(
                prereq.getPrereqJobName(),
                control.getKey().getProcessDate(),
                prereq.getPrereqSequenceNo()
            );
            
            Optional<BatchControl> prereqControl = 
                batchControlRepository.findById(prereqKey);
            
            if (prereqControl.isEmpty()) {
                log.warn("Prerequisite not found: {}", prereq.getPrereqJobName());
                return false;
            }
            
            BatchControl prereqBatch = prereqControl.get();
            
            if (prereqBatch.getStatus() != BatchStatus.DONE) {
                log.info("Prerequisite {} not complete, status: {}",
                    prereq.getPrereqJobName(), prereqBatch.getStatus());
                return false;
            }
            
            if (prereqBatch.getReturnCode() > prereq.getMaxReturnCode()) {
                log.warn("Prerequisite {} return code {} exceeds max {}",
                    prereq.getPrereqJobName(), 
                    prereqBatch.getReturnCode(),
                    prereq.getMaxReturnCode());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Updates process status.
     * Equivalent to COBOL BCHCTL00 FUNC-UPDT.
     */
    public BatchControl updateStatus(BatchControlKey key, 
                                     BatchStatus status,
                                     Integer returnCode,
                                     String errorDescription) {
        BatchControl control = batchControlRepository.findById(key)
            .orElseThrow(() -> new BatchControlNotFoundException(
                "Batch control not found: " + key));
        
        control.setStatus(status);
        control.setReturnCode(returnCode);
        control.setErrorDescription(errorDescription);
        
        if (status == BatchStatus.DONE || status == BatchStatus.ERROR) {
            control.setEndTime(LocalDateTime.now());
        }
        
        return batchControlRepository.save(control);
    }
    
    /**
     * Terminates process.
     * Equivalent to COBOL BCHCTL00 FUNC-TERM.
     */
    public BatchControl terminateProcess(BatchControlKey key,
                                         int returnCode,
                                         long recordsRead,
                                         long recordsWritten,
                                         long errorCount) {
        BatchControl control = batchControlRepository.findById(key)
            .orElseThrow(() -> new BatchControlNotFoundException(
                "Batch control not found: " + key));
        
        control.setStatus(returnCode <= 4 ? BatchStatus.DONE : BatchStatus.ERROR);
        control.setReturnCode(returnCode);
        control.setEndTime(LocalDateTime.now());
        control.setRecordsRead(recordsRead);
        control.setRecordsWritten(recordsWritten);
        control.setErrorCount(errorCount);
        
        return batchControlRepository.save(control);
    }
}
```

### 5.4 Process Sequence Service

```java
@Service
@Slf4j
@Transactional
public class ProcessSequenceService {
    
    private final ProcessSequenceRepository sequenceRepository;
    private final BatchControlService batchControlService;
    
    /**
     * Initializes process sequence for a date.
     * Equivalent to COBOL PRCSEQ00 FUNC-INIT.
     */
    public List<ProcessSequence> initializeSequence(String processDate,
                                                    String sequenceType) {
        log.info("Initializing sequence for date: {}, type: {}",
            processDate, sequenceType);
        
        List<ProcessSequence> sequences = sequenceRepository
            .findByTypeOrderBySequenceNo(sequenceType);
        
        List<BatchControl> controls = new ArrayList<>();
        
        for (int i = 0; i < sequences.size(); i++) {
            ProcessSequence seq = sequences.get(i);
            
            BatchControl control = new BatchControl();
            control.setKey(new BatchControlKey(
                seq.getProcessId(), processDate, i + 1));
            control.setStatus(BatchStatus.READY);
            control.setProgramName(seq.getProgramName());
            control.setRestartCount(0);
            
            controls.add(control);
        }
        
        batchControlRepository.saveAll(controls);
        
        return sequences;
    }
    
    /**
     * Gets next ready process.
     * Equivalent to COBOL PRCSEQ00 FUNC-NEXT.
     */
    public Optional<ProcessSequence> getNextReadyProcess(String processDate) {
        List<BatchControl> readyProcesses = batchControlRepository
            .findByKeyProcessDateAndStatus(processDate, BatchStatus.READY);
        
        for (BatchControl control : readyProcesses) {
            if (batchControlService.checkPrerequisites(control)) {
                ProcessSequence sequence = sequenceRepository
                    .findById(control.getKey().getJobName())
                    .orElse(null);
                
                if (sequence != null) {
                    return Optional.of(sequence);
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Checks sequence completion status.
     * Equivalent to COBOL PRCSEQ00 FUNC-STAT.
     */
    public SequenceStatus checkSequenceStatus(String processDate) {
        List<BatchControl> allControls = batchControlRepository
            .findByKeyProcessDate(processDate);
        
        long activeCount = allControls.stream()
            .filter(c -> c.getStatus() == BatchStatus.ACTIVE)
            .count();
        
        long errorCount = allControls.stream()
            .filter(c -> c.getStatus() == BatchStatus.ERROR)
            .count();
        
        long doneCount = allControls.stream()
            .filter(c -> c.getStatus() == BatchStatus.DONE)
            .count();
        
        long readyCount = allControls.stream()
            .filter(c -> c.getStatus() == BatchStatus.READY)
            .count();
        
        return new SequenceStatus(
            allControls.size(), activeCount, errorCount, doneCount, readyCount);
    }
}
```

### 5.5 Temporal Workflow for Orchestration

```java
@WorkflowInterface
public interface DailyBatchWorkflow {
    
    @WorkflowMethod
    BatchResult runDailyBatch(String processDate);
    
    @SignalMethod
    void cancelBatch();
    
    @QueryMethod
    BatchProgress getProgress();
}

@Slf4j
public class DailyBatchWorkflowImpl implements DailyBatchWorkflow {
    
    private final BatchActivities activities = 
        Workflow.newActivityStub(BatchActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofHours(2))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .build())
                .build());
    
    private boolean cancelled = false;
    private BatchProgress progress = new BatchProgress();
    
    @Override
    public BatchResult runDailyBatch(String processDate) {
        log.info("Starting daily batch workflow for date: {}", processDate);
        
        try {
            // Initialize sequence
            progress.setPhase("INITIALIZATION");
            activities.initializeSequence(processDate);
            
            // Run transaction validation
            if (!cancelled) {
                progress.setPhase("TRANSACTION_VALIDATION");
                progress.setCurrentJob("TRNVAL00");
                JobResult validationResult = 
                    activities.runTransactionValidation(processDate);
                progress.addJobResult(validationResult);
                
                if (validationResult.getReturnCode() > 4) {
                    return BatchResult.failed("Transaction validation failed");
                }
            }
            
            // Run position updates
            if (!cancelled) {
                progress.setPhase("POSITION_UPDATE");
                progress.setCurrentJob("POSUPD00");
                JobResult updateResult = 
                    activities.runPositionUpdate(processDate);
                progress.addJobResult(updateResult);
                
                if (updateResult.getReturnCode() > 4) {
                    return BatchResult.failed("Position update failed");
                }
            }
            
            // Run history load
            if (!cancelled) {
                progress.setPhase("HISTORY_LOAD");
                progress.setCurrentJob("HISTLD00");
                JobResult historyResult = 
                    activities.runHistoryLoad(processDate);
                progress.addJobResult(historyResult);
            }
            
            progress.setPhase("COMPLETE");
            return BatchResult.success(progress.getJobResults());
            
        } catch (Exception e) {
            log.error("Batch workflow failed", e);
            return BatchResult.failed(e.getMessage());
        }
    }
    
    @Override
    public void cancelBatch() {
        this.cancelled = true;
    }
    
    @Override
    public BatchProgress getProgress() {
        return progress;
    }
}

@ActivityInterface
public interface BatchActivities {
    
    @ActivityMethod
    void initializeSequence(String processDate);
    
    @ActivityMethod
    JobResult runTransactionValidation(String processDate);
    
    @ActivityMethod
    JobResult runPositionUpdate(String processDate);
    
    @ActivityMethod
    JobResult runHistoryLoad(String processDate);
}
```

---

## 6. Checkpoint/Restart Modernization

### 6.1 Overview

The COBOL checkpoint/restart mechanism (CKPRST.cpy) must be preserved to ensure batch jobs can resume from the last successful point after a failure. Spring Batch provides built-in support for this through its job repository.

### 6.2 Checkpoint Data Structure

```java
@Entity
@Table(name = "BATCH_CHECKPOINT")
public class BatchCheckpoint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "JOB_EXECUTION_ID")
    private Long jobExecutionId;
    
    @Column(name = "STEP_EXECUTION_ID")
    private Long stepExecutionId;
    
    @Column(name = "PROGRAM_ID", length = 8)
    private String programId;
    
    @Column(name = "RUN_DATE", length = 8)
    private String runDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 1)
    private CheckpointStatus status;
    
    @Column(name = "RECORDS_READ")
    private Long recordsRead;
    
    @Column(name = "RECORDS_PROCESSED")
    private Long recordsProcessed;
    
    @Column(name = "RECORDS_ERROR")
    private Long recordsError;
    
    @Column(name = "RESTART_COUNT")
    private Integer restartCount;
    
    @Column(name = "LAST_KEY", length = 50)
    private String lastKey;
    
    @Column(name = "LAST_CHECKPOINT_TIME")
    private LocalDateTime lastCheckpointTime;
    
    @Column(name = "PHASE", length = 2)
    private String phase;
    
    @Column(name = "COMMIT_FREQUENCY")
    private Integer commitFrequency;
    
    @Column(name = "MAX_ERRORS")
    private Integer maxErrors;
    
    @Column(name = "MAX_RESTARTS")
    private Integer maxRestarts;
}

public enum CheckpointStatus {
    INITIAL('I'),
    ACTIVE('A'),
    COMPLETE('C'),
    FAILED('F'),
    RESTARTED('R');
    
    private final char code;
}
```

### 6.3 Checkpoint Listener

```java
@Component
@Slf4j
public class CheckpointListener implements StepExecutionListener, ChunkListener {
    
    private final BatchCheckpointRepository checkpointRepository;
    private final AtomicLong recordsRead = new AtomicLong(0);
    private final AtomicLong recordsProcessed = new AtomicLong(0);
    private final AtomicLong recordsError = new AtomicLong(0);
    private String lastProcessedKey;
    
    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Starting step: {}", stepExecution.getStepName());
        
        // Check for restart scenario
        if (stepExecution.getJobExecution().getExecutionContext()
                .containsKey("restart")) {
            loadCheckpoint(stepExecution);
        } else {
            initializeCheckpoint(stepExecution);
        }
    }
    
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Completing step: {}, read: {}, written: {}, errors: {}",
            stepExecution.getStepName(),
            stepExecution.getReadCount(),
            stepExecution.getWriteCount(),
            stepExecution.getSkipCount());
        
        finalizeCheckpoint(stepExecution);
        
        return stepExecution.getExitStatus();
    }
    
    @Override
    public void afterChunk(ChunkContext context) {
        StepExecution stepExecution = context.getStepContext().getStepExecution();
        
        // Update checkpoint after each chunk (commit point)
        updateCheckpoint(stepExecution);
        
        log.debug("Checkpoint updated after chunk. Records processed: {}",
            recordsProcessed.get());
    }
    
    private void initializeCheckpoint(StepExecution stepExecution) {
        BatchCheckpoint checkpoint = new BatchCheckpoint();
        checkpoint.setJobExecutionId(stepExecution.getJobExecutionId());
        checkpoint.setStepExecutionId(stepExecution.getId());
        checkpoint.setProgramId(stepExecution.getStepName());
        checkpoint.setRunDate(LocalDate.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd")));
        checkpoint.setStatus(CheckpointStatus.ACTIVE);
        checkpoint.setRecordsRead(0L);
        checkpoint.setRecordsProcessed(0L);
        checkpoint.setRecordsError(0L);
        checkpoint.setRestartCount(0);
        checkpoint.setPhase("00"); // INIT phase
        checkpoint.setCommitFrequency(1000);
        checkpoint.setMaxErrors(100);
        checkpoint.setMaxRestarts(3);
        checkpoint.setLastCheckpointTime(LocalDateTime.now());
        
        checkpointRepository.save(checkpoint);
        
        stepExecution.getExecutionContext()
            .put("checkpointId", checkpoint.getId());
    }
    
    private void loadCheckpoint(StepExecution stepExecution) {
        Long checkpointId = stepExecution.getJobExecution()
            .getExecutionContext().getLong("checkpointId");
        
        BatchCheckpoint checkpoint = checkpointRepository.findById(checkpointId)
            .orElseThrow(() -> new CheckpointNotFoundException(
                "Checkpoint not found: " + checkpointId));
        
        // Restore state
        recordsRead.set(checkpoint.getRecordsRead());
        recordsProcessed.set(checkpoint.getRecordsProcessed());
        recordsError.set(checkpoint.getRecordsError());
        lastProcessedKey = checkpoint.getLastKey();
        
        checkpoint.setStatus(CheckpointStatus.RESTARTED);
        checkpoint.setRestartCount(checkpoint.getRestartCount() + 1);
        
        if (checkpoint.getRestartCount() > checkpoint.getMaxRestarts()) {
            throw new MaxRestartsExceededException(
                "Maximum restart count exceeded: " + checkpoint.getMaxRestarts());
        }
        
        checkpointRepository.save(checkpoint);
        
        log.info("Loaded checkpoint. Resuming from key: {}, records: {}",
            lastProcessedKey, recordsProcessed.get());
    }
    
    private void updateCheckpoint(StepExecution stepExecution) {
        Long checkpointId = stepExecution.getExecutionContext()
            .getLong("checkpointId");
        
        BatchCheckpoint checkpoint = checkpointRepository.findById(checkpointId)
            .orElseThrow();
        
        checkpoint.setRecordsRead(recordsRead.get());
        checkpoint.setRecordsProcessed(recordsProcessed.get());
        checkpoint.setRecordsError(recordsError.get());
        checkpoint.setLastKey(lastProcessedKey);
        checkpoint.setLastCheckpointTime(LocalDateTime.now());
        checkpoint.setPhase("20"); // PROC phase
        
        checkpointRepository.save(checkpoint);
    }
    
    private void finalizeCheckpoint(StepExecution stepExecution) {
        Long checkpointId = stepExecution.getExecutionContext()
            .getLong("checkpointId");
        
        BatchCheckpoint checkpoint = checkpointRepository.findById(checkpointId)
            .orElseThrow();
        
        checkpoint.setRecordsRead(stepExecution.getReadCount());
        checkpoint.setRecordsProcessed(stepExecution.getWriteCount());
        checkpoint.setRecordsError((long) stepExecution.getSkipCount());
        checkpoint.setLastCheckpointTime(LocalDateTime.now());
        checkpoint.setPhase("40"); // TERM phase
        
        if (stepExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            checkpoint.setStatus(CheckpointStatus.COMPLETE);
        } else {
            checkpoint.setStatus(CheckpointStatus.FAILED);
        }
        
        checkpointRepository.save(checkpoint);
    }
    
    public void setLastProcessedKey(String key) {
        this.lastProcessedKey = key;
    }
    
    public void incrementRecordsRead() {
        recordsRead.incrementAndGet();
    }
    
    public void incrementRecordsProcessed() {
        recordsProcessed.incrementAndGet();
    }
    
    public void incrementRecordsError() {
        recordsError.incrementAndGet();
    }
}
```

### 6.4 Restartable Item Reader

```java
@Component
@StepScope
@Slf4j
public class RestartableTransactionReader implements 
        ItemReader<Transaction>, ItemStream {
    
    private final JdbcCursorItemReader<Transaction> delegate;
    private final CheckpointListener checkpointListener;
    private String lastProcessedKey;
    private boolean initialized = false;
    
    public RestartableTransactionReader(
            DataSource dataSource,
            CheckpointListener checkpointListener,
            @Value("#{jobParameters['processDate']}") String processDate) {
        this.checkpointListener = checkpointListener;
        
        this.delegate = new JdbcCursorItemReaderBuilder<Transaction>()
            .name("restartableTransactionReader")
            .dataSource(dataSource)
            .sql("""
                SELECT * FROM TRANSACTION 
                WHERE TRANS_DATE = ? 
                AND STATUS = 'P'
                ORDER BY TRANS_TIME, SEQUENCE_NO
                """)
            .preparedStatementSetter(ps -> ps.setString(1, processDate))
            .rowMapper(new TransactionRowMapper())
            .build();
    }
    
    @Override
    public void open(ExecutionContext executionContext) {
        delegate.open(executionContext);
        
        if (executionContext.containsKey("lastProcessedKey")) {
            lastProcessedKey = executionContext.getString("lastProcessedKey");
            skipToLastProcessedKey();
        }
        
        initialized = true;
    }
    
    @Override
    public Transaction read() throws Exception {
        Transaction transaction = delegate.read();
        
        if (transaction != null) {
            lastProcessedKey = buildKey(transaction);
            checkpointListener.setLastProcessedKey(lastProcessedKey);
            checkpointListener.incrementRecordsRead();
        }
        
        return transaction;
    }
    
    @Override
    public void update(ExecutionContext executionContext) {
        delegate.update(executionContext);
        executionContext.putString("lastProcessedKey", lastProcessedKey);
    }
    
    @Override
    public void close() {
        delegate.close();
    }
    
    private void skipToLastProcessedKey() {
        if (lastProcessedKey == null) {
            return;
        }
        
        log.info("Skipping to last processed key: {}", lastProcessedKey);
        
        try {
            Transaction transaction;
            while ((transaction = delegate.read()) != null) {
                String currentKey = buildKey(transaction);
                if (currentKey.equals(lastProcessedKey)) {
                    log.info("Found restart position at key: {}", currentKey);
                    break;
                }
            }
        } catch (Exception e) {
            throw new RestartException(
                "Failed to skip to restart position", e);
        }
    }
    
    private String buildKey(Transaction transaction) {
        return String.format("%s|%s|%s|%s",
            transaction.getKey().getTransDate(),
            transaction.getKey().getTransTime(),
            transaction.getKey().getPortfolioId(),
            transaction.getKey().getSequenceNo());
    }
}
```

---

## 7. Error Handling Modernization

### 7.1 Overview

The COBOL ERRPROC error handling must be converted to modern exception handling with structured logging, alerting, and recovery capabilities.

### 7.2 Exception Hierarchy

```java
/**
 * Base exception for all batch processing errors.
 * Equivalent to COBOL ERR-MESSAGE structure.
 */
public abstract class BatchProcessingException extends RuntimeException {
    
    private final String errorCode;
    private final ErrorCategory category;
    private final ErrorSeverity severity;
    private final String programId;
    private final LocalDateTime timestamp;
    private final Map<String, Object> context;
    
    protected BatchProcessingException(
            String message,
            String errorCode,
            ErrorCategory category,
            ErrorSeverity severity,
            String programId) {
        super(message);
        this.errorCode = errorCode;
        this.category = category;
        this.severity = severity;
        this.programId = programId;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
    }
    
    public BatchProcessingException withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
}

/**
 * Equivalent to COBOL ERR-CAT-VALID.
 */
public class ValidationException extends BatchProcessingException {
    
    private final List<ValidationError> errors;
    
    public ValidationException(List<ValidationError> errors) {
        super(
            "Validation failed with " + errors.size() + " errors",
            "E001",
            ErrorCategory.VALIDATION,
            ErrorSeverity.ERROR,
            "TRNVAL00"
        );
        this.errors = errors;
    }
}

/**
 * Equivalent to COBOL ERR-CAT-VSAM.
 */
public class DataAccessException extends BatchProcessingException {
    
    private final String fileStatus;
    
    public DataAccessException(String message, String fileStatus) {
        super(
            message,
            "E002",
            ErrorCategory.DATA_ACCESS,
            mapSeverity(fileStatus),
            null
        );
        this.fileStatus = fileStatus;
    }
    
    private static ErrorSeverity mapSeverity(String fileStatus) {
        return switch (fileStatus) {
            case "22" -> ErrorSeverity.WARNING;  // Duplicate key
            case "23" -> ErrorSeverity.ERROR;    // Record not found
            case "10" -> ErrorSeverity.INFO;     // End of file
            default -> ErrorSeverity.SEVERE;
        };
    }
}

/**
 * Equivalent to COBOL ERR-CAT-PROC.
 */
public class ProcessingException extends BatchProcessingException {
    
    public ProcessingException(String message, String programId) {
        super(
            message,
            "E003",
            ErrorCategory.PROCESSING,
            ErrorSeverity.ERROR,
            programId
        );
    }
}

public enum ErrorCategory {
    VALIDATION("VL"),
    DATA_ACCESS("VS"),
    PROCESSING("PR"),
    SYSTEM("SY");
    
    private final String code;
}

public enum ErrorSeverity {
    INFO(0),
    WARNING(4),
    ERROR(8),
    SEVERE(12),
    TERMINAL(16);
    
    private final int returnCode;
}
```

### 7.3 Error Handler Service

```java
@Service
@Slf4j
public class ErrorHandlerService {
    
    private final ErrorLogRepository errorLogRepository;
    private final AlertService alertService;
    private final MetricsService metricsService;
    
    /**
     * Handles batch processing errors.
     * Equivalent to COBOL ERRPROC.
     */
    public ErrorHandlingResult handleError(BatchProcessingException exception) {
        // Log the error
        ErrorLog errorLog = logError(exception);
        
        // Update metrics
        metricsService.incrementErrorCount(
            exception.getCategory(),
            exception.getSeverity());
        
        // Determine action based on severity
        ErrorAction action = determineAction(exception);
        
        // Send alerts for severe errors
        if (exception.getSeverity().getReturnCode() >= 12) {
            alertService.sendAlert(exception);
        }
        
        return new ErrorHandlingResult(errorLog.getId(), action);
    }
    
    private ErrorLog logError(BatchProcessingException exception) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setTimestamp(exception.getTimestamp());
        errorLog.setProgramId(exception.getProgramId());
        errorLog.setErrorCode(exception.getErrorCode());
        errorLog.setCategory(exception.getCategory().getCode());
        errorLog.setSeverity(exception.getSeverity().getReturnCode());
        errorLog.setMessage(exception.getMessage());
        errorLog.setDetails(serializeContext(exception.getContext()));
        errorLog.setStackTrace(getStackTrace(exception));
        
        log.error("Batch error: [{}] {} - {}",
            exception.getErrorCode(),
            exception.getCategory(),
            exception.getMessage(),
            exception);
        
        return errorLogRepository.save(errorLog);
    }
    
    private ErrorAction determineAction(BatchProcessingException exception) {
        return switch (exception.getSeverity()) {
            case INFO, WARNING -> ErrorAction.CONTINUE;
            case ERROR -> ErrorAction.SKIP;
            case SEVERE -> ErrorAction.RETRY;
            case TERMINAL -> ErrorAction.ABORT;
        };
    }
}
```

### 7.4 Skip Policy and Retry Policy

```java
@Configuration
public class ErrorHandlingConfig {
    
    @Bean
    public SkipPolicy batchSkipPolicy() {
        return new SkipPolicy() {
            @Override
            public boolean shouldSkip(Throwable t, long skipCount) {
                if (t instanceof ValidationException) {
                    return skipCount < 100; // Skip up to 100 validation errors
                }
                if (t instanceof DataAccessException dae) {
                    // Skip duplicate key errors (SQLCODE -803)
                    return "22".equals(dae.getFileStatus());
                }
                return false;
            }
        };
    }
    
    @Bean
    public RetryPolicy batchRetryPolicy() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = 
            new HashMap<>();
        retryableExceptions.put(TransientDataAccessException.class, true);
        retryableExceptions.put(OptimisticLockingFailureException.class, true);
        retryableExceptions.put(DeadlockLoserDataAccessException.class, true);
        
        SimpleRetryPolicy policy = new SimpleRetryPolicy(3, retryableExceptions);
        return policy;
    }
    
    @Bean
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(1000);
        policy.setMultiplier(2.0);
        policy.setMaxInterval(30000);
        return policy;
    }
}
```

---

## 8. Week-by-Week Implementation Timeline

### Phase 2 Timeline Overview (20 Weeks)

```
Week  1-2:  Foundation and Framework Setup
Week  3-4:  TRNVAL00 Migration
Week  5-7:  POSUPD00 Migration
Week  8-10: HISTLD00 Migration
Week 11-13: BCHCTL00 and PRCSEQ00 Migration
Week 14-16: RCVPRC00 Migration
Week 17-18: ERRPROC Modernization
Week 19-20: Integration Testing and Documentation
```

### Detailed Weekly Breakdown

#### Weeks 1-2: Foundation and Framework Setup

**Week 1 Objectives:**
- Set up Spring Batch project structure
- Configure database connections and job repository
- Implement base configuration classes
- Create entity classes for batch control tables

**Week 1 Deliverables:**
- Project skeleton with Maven/Gradle configuration
- BatchConfiguration.java with job repository setup
- DataSourceConfiguration.java with connection pooling
- Base entity classes (BatchControl, ProcessSequence, BatchCheckpoint)

**Week 2 Objectives:**
- Set up Temporal.io workflow infrastructure
- Implement base workflow and activity interfaces
- Create repository interfaces
- Set up logging and monitoring infrastructure

**Week 2 Deliverables:**
- TemporalConfiguration.java
- DailyBatchWorkflow interface and stub implementation
- Repository interfaces for all entities
- Logging configuration with correlation IDs
- Prometheus metrics endpoints

#### Weeks 3-4: TRNVAL00 Migration

**Week 3 Objectives:**
- Implement Transaction entity and repository
- Create TransactionValidationService
- Implement validation rules engine
- Create unit tests for validation logic

**Week 3 Deliverables:**
- Transaction.java entity
- TransactionRepository.java
- TransactionValidationService.java
- ValidationRulesEngine.java
- 80%+ unit test coverage for validation

**Week 4 Objectives:**
- Implement Spring Batch job for transaction validation
- Create ItemReader, ItemProcessor, ItemWriter
- Implement skip and retry policies
- Integration testing

**Week 4 Deliverables:**
- TransactionValidationJobConfig.java
- TransactionItemReader.java
- TransactionItemProcessor.java
- TransactionItemWriter.java
- Integration tests with test database

#### Weeks 5-7: POSUPD00 Migration

**Week 5 Objectives:**
- Implement Position entity and repository
- Create PositionUpdateService with buy/sell logic
- Implement gain/loss calculations
- Unit tests for position calculations

**Week 5 Deliverables:**
- Position.java entity
- PositionRepository.java
- PositionUpdateService.java (buy, sell methods)
- Unit tests for calculations

**Week 6 Objectives:**
- Implement transfer and fee processing
- Create audit trail integration
- Implement optimistic locking for concurrent updates
- Unit tests for all transaction types

**Week 6 Deliverables:**
- Transfer processing implementation
- Fee processing implementation
- AuditService integration
- Optimistic locking configuration

**Week 7 Objectives:**
- Implement Spring Batch job for position updates
- Create checkpoint listener for position updates
- Integration testing with validation job
- Performance testing

**Week 7 Deliverables:**
- PositionUpdateJobConfig.java
- PositionCheckpointListener.java
- End-to-end integration tests
- Performance benchmarks

#### Weeks 8-10: HISTLD00 Migration

**Week 8 Objectives:**
- Implement PositionHistory entity and repository
- Create history mapping logic
- Implement duplicate detection
- Unit tests for mapping

**Week 8 Deliverables:**
- PositionHistory.java entity
- PositionHistoryRepository.java
- HistoryMappingService.java
- Unit tests

**Week 9 Objectives:**
- Implement Spring Batch job for history load
- Configure chunk-oriented processing with commit intervals
- Implement skip policy for duplicates
- Integration testing

**Week 9 Deliverables:**
- HistoryLoadJobConfig.java
- HistoryItemReader.java
- HistoryItemProcessor.java
- HistoryItemWriter.java

**Week 10 Objectives:**
- Performance optimization for bulk inserts
- Implement parallel processing where applicable
- Load testing with production-like volumes
- Documentation

**Week 10 Deliverables:**
- Optimized batch insert configuration
- Parallel step configuration (if applicable)
- Load test results
- Technical documentation

#### Weeks 11-13: BCHCTL00 and PRCSEQ00 Migration

**Week 11 Objectives:**
- Implement BatchControlService
- Create process initialization logic
- Implement prerequisite checking
- Unit tests

**Week 11 Deliverables:**
- BatchControlService.java
- initializeProcess() implementation
- checkPrerequisites() implementation
- Unit tests

**Week 12 Objectives:**
- Implement ProcessSequenceService
- Create sequence initialization logic
- Implement dependency resolution
- Unit tests

**Week 12 Deliverables:**
- ProcessSequenceService.java
- initializeSequence() implementation
- getNextReadyProcess() implementation
- Unit tests

**Week 13 Objectives:**
- Implement Temporal workflow for daily batch
- Create activity implementations
- Integration testing of full workflow
- Documentation

**Week 13 Deliverables:**
- DailyBatchWorkflowImpl.java
- BatchActivitiesImpl.java
- Workflow integration tests
- Workflow documentation

#### Weeks 14-16: RCVPRC00 Migration

**Week 14 Objectives:**
- Implement RecoveryService
- Create process recovery logic
- Implement restart, bypass, terminate actions
- Unit tests

**Week 14 Deliverables:**
- RecoveryService.java
- recoverProcess() implementation
- Recovery action implementations
- Unit tests

**Week 15 Objectives:**
- Implement sequence and full recovery modes
- Create RecoveryWorkflow
- Integration with batch control
- Integration testing

**Week 15 Deliverables:**
- recoverSequence() implementation
- recoverAll() implementation
- RecoveryWorkflowImpl.java
- Integration tests

**Week 16 Objectives:**
- Implement recovery UI/API endpoints
- Create monitoring dashboards
- Documentation and runbooks
- User acceptance testing

**Week 16 Deliverables:**
- RecoveryController.java (REST API)
- Grafana dashboards
- Recovery runbook
- UAT sign-off

#### Weeks 17-18: ERRPROC Modernization

**Week 17 Objectives:**
- Implement exception hierarchy
- Create ErrorHandlerService
- Implement error logging to database
- Unit tests

**Week 17 Deliverables:**
- Exception classes (ValidationException, etc.)
- ErrorHandlerService.java
- ErrorLog entity and repository
- Unit tests

**Week 18 Objectives:**
- Implement alerting integration
- Create error reporting dashboards
- Implement error notification service
- Integration testing

**Week 18 Deliverables:**
- AlertService.java
- Error dashboards
- NotificationService.java
- Integration tests

#### Weeks 19-20: Integration Testing and Documentation

**Week 19 Objectives:**
- End-to-end integration testing
- Performance testing
- Security testing
- Bug fixes

**Week 19 Deliverables:**
- Complete integration test suite
- Performance test results
- Security audit results
- Bug fix releases

**Week 20 Objectives:**
- Final documentation
- Training materials
- Deployment preparation
- Go-live readiness review

**Week 20 Deliverables:**
- Complete technical documentation
- Operations runbook
- Training materials
- Go-live checklist

---

## 9. Risk Mitigation Strategies

### 9.1 Technical Risks

| Risk | Impact | Probability | Mitigation Strategy |
|------|--------|-------------|---------------------|
| Data integrity issues during migration | High | Medium | Implement comprehensive validation, parallel running with COBOL, reconciliation reports |
| Performance degradation | High | Medium | Early performance testing, profiling, database optimization |
| Checkpoint/restart failures | High | Low | Extensive testing of restart scenarios, automated recovery testing |
| Dependency resolution errors | Medium | Medium | Comprehensive unit tests, integration tests with all dependency scenarios |
| Database deadlocks | Medium | Medium | Implement retry policies, optimize transaction scope, use row-level locking |

### 9.2 Operational Risks

| Risk | Impact | Probability | Mitigation Strategy |
|------|--------|-------------|---------------------|
| Extended batch window | High | Medium | Parallel processing, performance optimization, early warning monitoring |
| Recovery procedure failures | High | Low | Documented runbooks, automated recovery testing, regular drills |
| Monitoring gaps | Medium | Medium | Comprehensive metrics, alerting, dashboards before go-live |
| Knowledge transfer gaps | Medium | Medium | Detailed documentation, training sessions, pair programming |

### 9.3 Mitigation Implementation Details

#### 9.3.1 Parallel Running Strategy

During the transition period, run both COBOL and Java batch jobs in parallel:

```java
@Service
public class ParallelValidationService {
    
    public ReconciliationResult reconcile(String processDate) {
        // Get COBOL results
        List<TransactionResult> cobolResults = 
            cobolResultRepository.findByProcessDate(processDate);
        
        // Get Java results
        List<TransactionResult> javaResults = 
            javaResultRepository.findByProcessDate(processDate);
        
        // Compare results
        ReconciliationResult result = new ReconciliationResult();
        
        for (TransactionResult cobol : cobolResults) {
            TransactionResult java = findMatching(javaResults, cobol);
            
            if (java == null) {
                result.addMissing(cobol);
            } else if (!cobol.equals(java)) {
                result.addMismatch(cobol, java);
            } else {
                result.addMatched(cobol);
            }
        }
        
        return result;
    }
}
```

#### 9.3.2 Performance Monitoring

```java
@Component
@Slf4j
public class BatchPerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onJobExecution(JobExecutionEvent event) {
        JobExecution execution = event.getJobExecution();
        
        // Record job duration
        Timer.builder("batch.job.duration")
            .tag("job", execution.getJobInstance().getJobName())
            .tag("status", execution.getStatus().toString())
            .register(meterRegistry)
            .record(Duration.between(
                execution.getStartTime(), 
                execution.getEndTime()));
        
        // Record throughput
        long recordsProcessed = execution.getStepExecutions().stream()
            .mapToLong(StepExecution::getWriteCount)
            .sum();
        
        Gauge.builder("batch.job.throughput", () -> 
            recordsProcessed / getDurationSeconds(execution))
            .tag("job", execution.getJobInstance().getJobName())
            .register(meterRegistry);
    }
}
```

#### 9.3.3 Automated Recovery Testing

```java
@SpringBootTest
public class RecoveryIntegrationTest {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private RecoveryService recoveryService;
    
    @Test
    void testRestartAfterFailure() throws Exception {
        // Start job
        JobExecution execution = jobLauncher.run(
            transactionValidationJob,
            new JobParametersBuilder()
                .addString("processDate", "20260115")
                .toJobParameters());
        
        // Simulate failure at 50% completion
        simulateFailureAt(execution, 0.5);
        
        // Verify job is in FAILED state
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        
        // Recover and restart
        recoveryService.recoverProcess(
            "TRNVAL00", "20260115", RecoveryAction.RESTART);
        
        // Restart job
        JobExecution restartExecution = jobLauncher.run(
            transactionValidationJob,
            new JobParametersBuilder()
                .addString("processDate", "20260115")
                .addLong("restart", 1L)
                .toJobParameters());
        
        // Verify completion
        assertThat(restartExecution.getStatus())
            .isEqualTo(BatchStatus.COMPLETED);
        
        // Verify no duplicate processing
        long totalProcessed = execution.getStepExecutions().stream()
            .mapToLong(StepExecution::getWriteCount)
            .sum() +
            restartExecution.getStepExecutions().stream()
            .mapToLong(StepExecution::getWriteCount)
            .sum();
        
        assertThat(totalProcessed).isEqualTo(expectedTotalRecords);
    }
}
```

---

## 10. Testing Strategy

### 10.1 Testing Levels

| Level | Scope | Tools | Coverage Target |
|-------|-------|-------|-----------------|
| Unit | Individual services and components | JUnit 5, Mockito | 80%+ |
| Integration | Service interactions, database | Spring Test, Testcontainers | 70%+ |
| End-to-End | Complete batch workflows | Cucumber, Spring Batch Test | All critical paths |
| Performance | Throughput, latency | JMeter, Gatling | Production volumes |
| Regression | COBOL parity | Custom reconciliation | 100% match |

### 10.2 Test Data Strategy

```java
@Component
public class TestDataGenerator {
    
    /**
     * Generates test transactions matching COBOL test data specs.
     */
    public List<Transaction> generateTransactions(int count) {
        List<Transaction> transactions = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Transaction t = new Transaction();
            t.setKey(generateKey(i));
            t.setTransactionType(randomTransactionType());
            t.setQuantity(randomQuantity());
            t.setPrice(randomPrice());
            t.setAmount(t.getQuantity().multiply(t.getPrice()));
            t.setStatus(TransactionStatus.PENDING);
            transactions.add(t);
        }
        
        return transactions;
    }
    
    /**
     * Generates error scenarios for testing.
     */
    public List<Transaction> generateErrorScenarios() {
        return List.of(
            createInvalidPortfolioTransaction(),
            createInvalidTransactionType(),
            createZeroQuantityTransaction(),
            createNegativePriceTransaction(),
            createInsufficientBalanceTransaction()
        );
    }
}
```

### 10.3 Regression Testing Against COBOL

```java
@SpringBootTest
public class CobolParityTest {
    
    @Autowired
    private TransactionValidationService validationService;
    
    @ParameterizedTest
    @CsvFileSource(resources = "/cobol-test-cases.csv")
    void testValidationParityWithCobol(
            String portfolioId,
            String transType,
            String quantity,
            String price,
            String expectedResult,
            String expectedErrorCode) {
        
        Transaction transaction = buildTransaction(
            portfolioId, transType, quantity, price);
        
        ValidationResult result = validationService.validate(transaction);
        
        if ("VALID".equals(expectedResult)) {
            assertThat(result.isValid()).isTrue();
        } else {
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                .extracting(ValidationError::getCode)
                .contains(expectedErrorCode);
        }
    }
}
```

---

## 11. Appendices

### Appendix A: COBOL to Java Type Mapping

| COBOL Type | Java Type | Notes |
|------------|-----------|-------|
| PIC X(n) | String | Fixed length, trim on read |
| PIC 9(n) | Integer/Long | Depends on size |
| PIC S9(n) COMP | int/long | Binary, signed |
| PIC S9(n)V9(m) COMP-3 | BigDecimal | Packed decimal |
| PIC 9(n)V9(m) | BigDecimal | Implied decimal |

### Appendix B: Return Code Mapping

| COBOL RC | Java Equivalent | Spring Batch Status |
|----------|-----------------|---------------------|
| 0 | ExitStatus.COMPLETED | COMPLETED |
| 4 | ExitStatus.COMPLETED with warnings | COMPLETED |
| 8 | ExitStatus.FAILED | FAILED |
| 12 | ExitStatus.FAILED (severe) | FAILED |
| 16 | ExitStatus.FAILED (critical) | FAILED |

### Appendix C: File Status to Exception Mapping

| VSAM Status | Exception | Recovery Action |
|-------------|-----------|-----------------|
| 00 | None | Continue |
| 10 | EndOfFileException | Normal completion |
| 22 | DuplicateKeyException | Skip |
| 23 | RecordNotFoundException | Error/Skip |
| 35 | FileNotFoundException | Abort |

### Appendix D: Glossary

| Term | Definition |
|------|------------|
| Chunk | A group of items processed together in Spring Batch |
| Checkpoint | A saved state allowing restart from a known point |
| ItemReader | Spring Batch component that reads input data |
| ItemProcessor | Spring Batch component that transforms data |
| ItemWriter | Spring Batch component that writes output data |
| Job | A complete batch process in Spring Batch |
| Step | A phase within a job |
| Workflow | A Temporal.io orchestration of multiple activities |
| Activity | A Temporal.io unit of work |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-15 | Migration Team | Initial version |

---

## Approval

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Technical Lead | | | |
| Project Manager | | | |
| Business Owner | | | |
