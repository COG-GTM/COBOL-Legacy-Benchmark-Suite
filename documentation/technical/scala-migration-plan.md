# Staged Migration Plan: COBOL Batch Processing to Scala

Version: 1.0  
Last Updated: 2025-01-25  
Author: Migration Planning Team

## Executive Summary

This document outlines a comprehensive staged migration plan for reimplementing the COBOL batch processing layer of the Investment Portfolio Management System in Scala. The migration preserves the existing sequential processing pipeline (validation, position updates, history loading) while modernizing the technology stack and maintaining operational continuity through parallel operation of both systems during the transition period.

## 1. Current System Analysis

### 1.1 Batch Processing Architecture Overview

The existing COBOL batch processing layer operates as a sequential pipeline with three distinct component categories:

**Control Programs** manage process execution, sequencing, and recovery:

| Program | Function | Key Responsibilities |
|---------|----------|---------------------|
| BCHCTL00 | Batch Control Processor | Controls process execution, maintains status, supports checkpoint/restart |
| PRCSEQ00 | Process Sequence Manager | Defines process sequence, manages dependencies, creates control records |
| RCVPRC00 | Process Recovery Handler | Handles recovery operations (process, sequence, or all), determines restart/bypass/terminate actions |

**Processing Programs** execute the core business logic in sequence:

| Program | Function | Input | Output |
|---------|----------|-------|--------|
| TRNVAL00 | Transaction Validation | External transaction files | Validated transactions |
| POSUPD00 | Position Update | Validated transactions | Position Master VSAM, Transaction History VSAM |
| HISTLD00 | History Load | Transaction History VSAM | DB2 POSHIST table |

**Shared Utilities** provide common services:

| Program | Function | Consumers |
|---------|----------|-----------|
| ERRPROC | Error Processing | All batch programs |
| DB2CONN | Database Connectivity | All DB2-accessing programs |

### 1.2 Data Flow Analysis

The current data flow follows a strict sequential pattern:

```
External Transaction Files
         |
         v
    [TRNVAL00] - Validates transactions, performs error checking
         |
         v
    [POSUPD00] - Updates Position Master VSAM
         |         Writes to Transaction History VSAM
         v
    [HISTLD00] - Loads from VSAM to DB2 POSHIST table
         |
         v
    DB2 Tables (POSHIST, ERRLOG)
```

### 1.3 Dual-Storage Strategy

The system implements a dual-storage architecture optimized for different access patterns:

**VSAM Files (High-Velocity Operational Updates)**:
- Position Master VSAM: Current portfolio positions and balances (indexed by portfolio ID, date, investment ID)
- Transaction History VSAM: Recent transaction records (indexed by portfolio ID, date, time, sequence)

**DB2 Tables (Analytical Queries and Long-Term Storage)**:
- POSHIST: Historical position snapshots with full transaction details
- ERRLOG: Error logs and exceptions for audit and troubleshooting

### 1.4 Checkpoint/Recovery Mechanism

The current system uses a two-level checkpoint strategy:

**Job-Level Control (BCHCTL.cpy)**:
- Tracks job status (Ready, Active, Waiting, Done, Error)
- Manages dependencies between jobs
- Records restart counts and timestamps

**Program-Level Checkpointing (CKPRST.cpy)**:
- Tracks processing phase (Init, Read, Process, Update, Terminate)
- Records last processed key for restart positioning
- Maintains file positions for all input files
- Configurable commit frequency (default: 1000 records)

### 1.5 Key Data Structures

**Transaction Record (TRNREC.cpy)**:
- Key: Date + Time + Portfolio ID + Sequence Number
- Data: Investment ID, Type (Buy/Sell/Transfer/Fee), Quantity, Price, Amount, Currency, Status

**Position Record (POSREC.cpy)**:
- Key: Portfolio ID + Date + Investment ID
- Data: Quantity, Cost Basis, Market Value, Currency, Status

**History Record (HISTREC.cpy)**:
- Key: Portfolio ID + Date + Time + Sequence
- Data: Record Type, Action Code, Before/After Images, Reason Code

## 2. Migration Strategy

### 2.1 Guiding Principles

The migration follows these core principles:

1. **Incremental Migration**: Migrate components in stages to minimize risk and allow validation at each step
2. **Functional Equivalence**: Ensure Scala implementations produce identical outputs for identical inputs
3. **Parallel Operation**: Run COBOL and Scala systems in parallel during transition with result comparison
4. **Preserve Semantics**: Maintain the sequential processing pipeline and checkpoint/recovery semantics
5. **Modern Equivalents**: Use appropriate Scala/JVM technologies that match COBOL capabilities

### 2.2 Technology Stack for Scala Implementation

| COBOL Component | Scala Equivalent | Rationale |
|-----------------|------------------|-----------|
| VSAM Files | Apache Ignite or RocksDB | High-velocity key-value operations with persistence |
| DB2 Access | Slick or Doobie with JDBC | Type-safe database access with connection pooling |
| Checkpoint Files | Apache Kafka + State Store | Durable checkpointing with exactly-once semantics |
| Batch Control | Akka Streams or ZIO Streams | Backpressure-aware stream processing |
| Error Handling | Cats Effect / ZIO Error Channel | Typed error handling with recovery |
| Sequential Processing | For-comprehensions / Monadic composition | Preserve sequential semantics |

### 2.3 Dual-Storage Strategy in Scala

**High-Velocity Operational Store (VSAM Replacement)**:

Option A: Apache Ignite
- In-memory data grid with persistence
- Supports indexed access patterns matching VSAM
- Built-in partitioning and replication
- SQL and key-value APIs

Option B: RocksDB with Custom Wrapper
- Embedded key-value store
- LSM-tree architecture for write-heavy workloads
- Supports range scans (equivalent to VSAM sequential access)
- Lower operational overhead than distributed systems

**Analytical Store (DB2 Replacement)**:

Option A: PostgreSQL with Slick
- Mature RDBMS with excellent Scala integration
- Supports complex analytical queries
- JDBC compatibility for existing tooling

Option B: Keep DB2 with Doobie
- Minimize changes to downstream systems
- Leverage existing DB2 infrastructure
- Gradual migration path for database layer

### 2.4 Checkpoint/Recovery in Scala

The Scala implementation will use a checkpoint service that mirrors COBOL semantics:

```scala
// Checkpoint state matching CKPRST.cpy structure
case class CheckpointState(
  programId: String,
  runDate: LocalDate,
  runTime: LocalTime,
  status: ProcessStatus,
  counters: ProcessCounters,
  position: ProcessPosition,
  resources: List[FileResource],
  controlInfo: ControlInfo
)

case class ProcessCounters(
  recordsRead: Long,
  recordsProcessed: Long,
  recordsError: Long,
  restartCount: Int
)

case class ProcessPosition(
  lastKey: String,
  lastTime: Instant,
  phase: ProcessPhase
)

sealed trait ProcessPhase
object ProcessPhase {
  case object Init extends ProcessPhase
  case object Read extends ProcessPhase
  case object Process extends ProcessPhase
  case object Update extends ProcessPhase
  case object Terminate extends ProcessPhase
}
```

Checkpoint persistence options:
1. **Kafka Streams State Store**: For distributed deployments with exactly-once guarantees
2. **Local RocksDB**: For single-node deployments matching current VSAM checkpoint behavior
3. **PostgreSQL**: For centralized checkpoint management with query capabilities

## 3. Migration Stages

### Stage 1: Foundation Layer (Weeks 1-4)

**Objective**: Establish shared infrastructure and utilities that all batch programs depend on.

#### 3.1.1 Error Handling Service (ERRPROC Equivalent)

Implement a centralized error handling service matching ERRPROC semantics:

```scala
// Error categories matching COBOL ERRHAND.cpy
sealed trait ErrorCategory
object ErrorCategory {
  case object System extends ErrorCategory
  case object Application extends ErrorCategory
  case object Data extends ErrorCategory
}

// Error severity levels
sealed trait Severity
object Severity {
  case object Info extends Severity
  case object Warning extends Severity
  case object Error extends Severity
  case object Severe extends Severity
}

// Error service interface
trait ErrorService[F[_]] {
  def logError(
    programId: String,
    category: ErrorCategory,
    code: String,
    severity: Severity,
    message: String,
    details: Option[String]
  ): F[Unit]
  
  def getErrors(
    programId: Option[String],
    fromTime: Option[Instant],
    toTime: Option[Instant]
  ): F[List[ErrorRecord]]
}
```

**Deliverables**:
- ErrorService trait and implementation
- Error logging to both file and database (matching ERRPROC dual output)
- Error display formatting matching COBOL output format
- Unit tests with COBOL output comparison

#### 3.1.2 Database Connectivity Service (DB2CONN Equivalent)

Implement database connection management with retry logic:

```scala
// Connection configuration matching DB2CONN parameters
case class DatabaseConfig(
  dbName: String,
  planName: String,
  maxRetries: Int = 3,
  retryDelayMs: Long = 1000
)

// Connection service interface
trait DatabaseService[F[_]] {
  def connect(config: DatabaseConfig): F[Either[ConnectionError, Connection]]
  def disconnect(): F[Unit]
  def checkStatus(): F[ConnectionStatus]
  def withTransaction[A](f: Connection => F[A]): F[A]
}

// Connection status matching COBOL states
sealed trait ConnectionStatus
object ConnectionStatus {
  case object Connected extends ConnectionStatus
  case object Disconnected extends ConnectionStatus
  case class Error(sqlCode: Int, message: String) extends ConnectionStatus
}
```

**Deliverables**:
- DatabaseService trait and implementation
- Connection pooling with HikariCP
- Retry logic matching DB2CONN (3 retries with delay)
- Transaction management with commit/rollback
- Integration tests against test database

#### 3.1.3 Checkpoint/Restart Service

Implement checkpoint management matching CKPRST semantics:

```scala
trait CheckpointService[F[_]] {
  // Initialize checkpoint for new run or restart
  def initialize(programId: String, runDate: LocalDate): F[CheckpointState]
  
  // Take checkpoint at current position
  def takeCheckpoint(state: CheckpointState): F[Unit]
  
  // Commit checkpoint (make durable)
  def commitCheckpoint(state: CheckpointState): F[Unit]
  
  // Restart from last checkpoint
  def restartFromCheckpoint(programId: String, runDate: LocalDate): F[Option[CheckpointState]]
  
  // Check if restart is needed
  def needsRestart(programId: String, runDate: LocalDate): F[Boolean]
}
```

**Deliverables**:
- CheckpointService trait and implementation
- Persistent storage backend (RocksDB or PostgreSQL)
- Checkpoint file format compatible with parallel operation comparison
- Recovery logic matching RCVPRC00 behavior

#### 3.1.4 Operational Data Store (VSAM Replacement)

Implement high-velocity data store for positions and transactions:

```scala
// Key-value store interface matching VSAM access patterns
trait OperationalStore[F[_], K, V] {
  // Direct key access (VSAM READ)
  def get(key: K): F[Option[V]]
  
  // Sequential access (VSAM READ NEXT)
  def scan(fromKey: K, toKey: K): fs2.Stream[F, (K, V)]
  
  // Write operations (VSAM WRITE/REWRITE)
  def put(key: K, value: V): F[Unit]
  def update(key: K, f: V => V): F[Option[V]]
  
  // Delete (VSAM DELETE)
  def delete(key: K): F[Boolean]
  
  // Batch operations for efficiency
  def putBatch(entries: List[(K, V)]): F[Unit]
}

// Position store with VSAM-like indexing
trait PositionStore[F[_]] extends OperationalStore[F, PositionKey, Position] {
  def getByPortfolio(portfolioId: String): fs2.Stream[F, Position]
  def getByPortfolioAndDate(portfolioId: String, date: LocalDate): fs2.Stream[F, Position]
}

// Transaction history store
trait TransactionHistoryStore[F[_]] extends OperationalStore[F, TransactionKey, TransactionHistory] {
  def getByPortfolioDateRange(
    portfolioId: String,
    fromDate: LocalDate,
    toDate: LocalDate
  ): fs2.Stream[F, TransactionHistory]
}
```

**Deliverables**:
- OperationalStore trait and RocksDB/Ignite implementation
- PositionStore and TransactionHistoryStore implementations
- Index management matching VSAM key structures
- Performance benchmarks comparing to VSAM baseline

### Stage 2: Batch Control Framework (Weeks 5-8)

**Objective**: Implement the batch control and sequencing infrastructure.

#### 3.2.1 Batch Control Service (BCHCTL00 Equivalent)

```scala
// Batch control record matching BCHCTL.cpy
case class BatchControlRecord(
  key: BatchControlKey,
  status: BatchStatus,
  processControl: ProcessControl,
  dependencies: Dependencies,
  returnInfo: ReturnInfo,
  statistics: BatchStatistics
)

case class BatchControlKey(
  jobName: String,
  processDate: LocalDate,
  sequenceNo: Int
)

sealed trait BatchStatus
object BatchStatus {
  case object Ready extends BatchStatus
  case object Active extends BatchStatus
  case object Waiting extends BatchStatus
  case object Done extends BatchStatus
  case object Error extends BatchStatus
}

// Batch control service interface
trait BatchControlService[F[_]] {
  def initialize(request: ControlRequest): F[Either[BatchError, Unit]]
  def checkPrerequisites(jobName: String, processDate: LocalDate): F[PrerequisiteResult]
  def updateStatus(jobName: String, processDate: LocalDate, status: BatchStatus): F[Unit]
  def terminate(jobName: String, processDate: LocalDate, returnCode: Int): F[Unit]
}
```

**Deliverables**:
- BatchControlService implementation
- Batch control record persistence
- Status transition validation
- Integration with checkpoint service

#### 3.2.2 Process Sequence Manager (PRCSEQ00 Equivalent)

```scala
// Process sequence definition
case class ProcessSequence(
  processId: String,
  sequenceType: String,
  dependencies: List[ProcessDependency],
  restartable: Boolean,
  maxRestarts: Int
)

case class ProcessDependency(
  dependsOn: String,
  isHard: Boolean,  // Hard dependency must complete successfully
  maxReturnCode: Int
)

trait ProcessSequenceService[F[_]] {
  def initializeSequence(processDate: LocalDate, sequenceType: String): F[Unit]
  def getNextProcess(processDate: LocalDate): F[Option[String]]
  def checkDependencies(processId: String, processDate: LocalDate): F[DependencyResult]
  def updateProcessStatus(processId: String, processDate: LocalDate, status: BatchStatus): F[Unit]
  def checkCompletion(processDate: LocalDate): F[SequenceStatus]
  def terminateSequence(processDate: LocalDate): F[SequenceResult]
}
```

**Deliverables**:
- ProcessSequenceService implementation
- Dependency checking logic matching PRCSEQ00
- Process table management
- Sequence completion tracking

#### 3.2.3 Recovery Handler (RCVPRC00 Equivalent)

```scala
sealed trait RecoveryMode
object RecoveryMode {
  case object Process extends RecoveryMode   // Recover single process
  case object Sequence extends RecoveryMode  // Recover all processes in sequence
  case object All extends RecoveryMode       // Recover all processes
}

sealed trait RecoveryAction
object RecoveryAction {
  case object Restart extends RecoveryAction
  case object Bypass extends RecoveryAction
  case object Terminate extends RecoveryAction
}

trait RecoveryService[F[_]] {
  def initializeRecovery(
    processDate: LocalDate,
    processId: Option[String],
    mode: RecoveryMode
  ): F[Either[RecoveryError, Unit]]
  
  def determineAction(processId: String): F[RecoveryAction]
  
  def executeRecovery(
    processId: String,
    action: RecoveryAction
  ): F[Either[RecoveryError, RecoveryResult]]
  
  def recoverSequence(processDate: LocalDate): F[List[RecoveryResult]]
  
  def recoverAll(): F[List[RecoveryResult]]
}
```

**Deliverables**:
- RecoveryService implementation
- Recovery action determination logic
- Process restart/bypass/terminate operations
- Recovery audit logging

### Stage 3: Transaction Validation (Weeks 9-12)

**Objective**: Implement TRNVAL00 equivalent with identical validation rules.

#### 3.3.1 Transaction Validation Service

```scala
// Transaction record matching TRNREC.cpy
case class Transaction(
  key: TransactionKey,
  data: TransactionData,
  audit: AuditInfo
)

case class TransactionKey(
  date: LocalDate,
  time: LocalTime,
  portfolioId: String,
  sequenceNo: String
)

case class TransactionData(
  investmentId: String,
  transactionType: TransactionType,
  quantity: BigDecimal,
  price: BigDecimal,
  amount: BigDecimal,
  currency: String,
  status: TransactionStatus
)

sealed trait TransactionType
object TransactionType {
  case object Buy extends TransactionType
  case object Sell extends TransactionType
  case object Transfer extends TransactionType
  case object Fee extends TransactionType
}

// Validation result
sealed trait ValidationResult
object ValidationResult {
  case class Valid(transaction: Transaction) extends ValidationResult
  case class Invalid(transaction: Transaction, errors: List[ValidationError]) extends ValidationResult
}

// Validation service
trait TransactionValidationService[F[_]] {
  def validateTransaction(transaction: Transaction): F[ValidationResult]
  def validateBatch(transactions: List[Transaction]): F[List[ValidationResult]]
}
```

#### 3.3.2 Validation Rules Engine

Implement all validation rules from TRNVAL00:

```scala
trait ValidationRule {
  def validate(transaction: Transaction): Either[ValidationError, Unit]
}

// Example validation rules
object ValidationRules {
  // Date validation
  val dateNotFuture: ValidationRule = transaction =>
    if (transaction.key.date.isAfter(LocalDate.now()))
      Left(ValidationError("TRN001", "Transaction date cannot be in the future"))
    else Right(())
  
  // Amount validation
  val amountMatchesQuantityPrice: ValidationRule = transaction => {
    val expected = transaction.data.quantity * transaction.data.price
    if ((transaction.data.amount - expected).abs > 0.01)
      Left(ValidationError("TRN002", "Amount does not match quantity * price"))
    else Right(())
  }
  
  // Portfolio validation
  val portfolioExists: ValidationRule = transaction =>
    // Check portfolio exists in master file
    ???
  
  // Investment validation
  val investmentExists: ValidationRule = transaction =>
    // Check investment exists in reference data
    ???
}
```

**Deliverables**:
- TransactionValidationService implementation
- All validation rules matching TRNVAL00
- Validation error reporting matching COBOL format
- Batch validation with checkpoint support
- Comparison tests against COBOL output

### Stage 4: Position Update Processing (Weeks 13-16)

**Objective**: Implement POSUPD00 equivalent for updating positions and recording history.

#### 3.4.1 Position Update Service

```scala
// Position record matching POSREC.cpy
case class Position(
  key: PositionKey,
  data: PositionData,
  audit: AuditInfo
)

case class PositionKey(
  portfolioId: String,
  date: LocalDate,
  investmentId: String
)

case class PositionData(
  quantity: BigDecimal,
  costBasis: BigDecimal,
  marketValue: BigDecimal,
  currency: String,
  status: PositionStatus
)

trait PositionUpdateService[F[_]] {
  def processTransaction(transaction: Transaction): F[Either[UpdateError, Position]]
  
  def updatePosition(
    portfolioId: String,
    investmentId: String,
    transaction: Transaction
  ): F[Either[UpdateError, Position]]
  
  def recordTransactionHistory(
    transaction: Transaction,
    beforePosition: Option[Position],
    afterPosition: Position
  ): F[Unit]
}
```

#### 3.4.2 Position Calculation Logic

```scala
object PositionCalculator {
  def applyTransaction(
    currentPosition: Option[Position],
    transaction: Transaction
  ): Either[CalculationError, Position] = {
    transaction.data.transactionType match {
      case TransactionType.Buy =>
        applyBuy(currentPosition, transaction)
      case TransactionType.Sell =>
        applySell(currentPosition, transaction)
      case TransactionType.Transfer =>
        applyTransfer(currentPosition, transaction)
      case TransactionType.Fee =>
        applyFee(currentPosition, transaction)
    }
  }
  
  private def applyBuy(
    current: Option[Position],
    transaction: Transaction
  ): Either[CalculationError, Position] = {
    val newQuantity = current.map(_.data.quantity).getOrElse(BigDecimal(0)) + 
                      transaction.data.quantity
    val newCostBasis = current.map(_.data.costBasis).getOrElse(BigDecimal(0)) + 
                       transaction.data.amount
    // ... full calculation logic
    ???
  }
  
  // Similar methods for sell, transfer, fee
}
```

**Deliverables**:
- PositionUpdateService implementation
- Position calculation logic matching POSUPD00
- Transaction history recording to operational store
- Checkpoint integration for restart capability
- Comparison tests against COBOL output

### Stage 5: History Loading (Weeks 17-20)

**Objective**: Implement HISTLD00 equivalent for loading data from operational store to analytical database.

#### 3.5.1 History Load Service

```scala
trait HistoryLoadService[F[_]] {
  def loadHistory(
    fromDate: LocalDate,
    toDate: LocalDate,
    commitThreshold: Int = 1000
  ): F[LoadResult]
  
  def loadSingleRecord(history: TransactionHistory): F[Either[LoadError, Unit]]
}

case class LoadResult(
  recordsRead: Long,
  recordsWritten: Long,
  recordsSkipped: Long,
  errors: List[LoadError]
)

// Implementation with checkpoint support
class HistoryLoadServiceImpl[F[_]: Async](
  historyStore: TransactionHistoryStore[F],
  database: DatabaseService[F],
  checkpointService: CheckpointService[F],
  errorService: ErrorService[F]
) extends HistoryLoadService[F] {
  
  def loadHistory(
    fromDate: LocalDate,
    toDate: LocalDate,
    commitThreshold: Int
  ): F[LoadResult] = {
    for {
      checkpoint <- checkpointService.initialize("HISTLD00", fromDate)
      result <- historyStore
        .scan(fromKey(fromDate), toKey(toDate))
        .through(skipToCheckpoint(checkpoint))
        .evalMap(loadSingleRecord)
        .through(commitEvery(commitThreshold))
        .compile
        .fold(LoadResult.empty)(_ |+| _)
      _ <- checkpointService.commitCheckpoint(checkpoint.copy(
        status = ProcessStatus.Complete
      ))
    } yield result
  }
}
```

#### 3.5.2 DB2 Table Mapping

```scala
// POSHIST table mapping matching DBTBLS.cpy
case class PosHistRecord(
  accountNo: String,
  portfolioId: String,
  transDate: LocalDate,
  transTime: LocalTime,
  transType: String,
  securityId: String,
  quantity: BigDecimal,
  price: BigDecimal,
  amount: BigDecimal,
  fees: BigDecimal,
  totalAmount: BigDecimal,
  costBasis: BigDecimal,
  gainLoss: BigDecimal,
  processDate: LocalDate,
  processTime: LocalTime,
  programId: String,
  userId: String,
  auditTimestamp: Instant
)

// Slick table definition
class PosHistTable(tag: Tag) extends Table[PosHistRecord](tag, "POSHIST") {
  def accountNo = column[String]("ACCOUNT_NO", O.Length(8))
  def portfolioId = column[String]("PORTFOLIO_ID", O.Length(10))
  def transDate = column[LocalDate]("TRANS_DATE")
  def transTime = column[LocalTime]("TRANS_TIME")
  // ... all columns
  
  def * = (accountNo, portfolioId, transDate, transTime, ...).mapTo[PosHistRecord]
  
  def pk = primaryKey("POSHIST_PK", (accountNo, portfolioId, transDate, transTime))
}
```

**Deliverables**:
- HistoryLoadService implementation
- DB2/PostgreSQL table mappings
- Duplicate handling (matching SQLCODE -803 behavior)
- Commit frequency matching COBOL (every 1000 records)
- Checkpoint integration
- Statistics reporting matching HISTLD00 output format

### Stage 6: Integration and Parallel Operation (Weeks 21-24)

**Objective**: Integrate all components and establish parallel operation with COBOL system.

#### 3.6.1 Batch Pipeline Orchestration

```scala
// Main batch pipeline matching COBOL job flow
class BatchPipeline[F[_]: Async](
  batchControl: BatchControlService[F],
  sequenceService: ProcessSequenceService[F],
  validationService: TransactionValidationService[F],
  positionService: PositionUpdateService[F],
  historyService: HistoryLoadService[F],
  recoveryService: RecoveryService[F]
) {
  
  def runDailyBatch(processDate: LocalDate): F[BatchResult] = {
    for {
      // Initialize sequence (like JCL job submission)
      _ <- sequenceService.initializeSequence(processDate, "DAILY")
      
      // Run TRNVAL00 equivalent
      _ <- batchControl.updateStatus("TRNVAL00", processDate, BatchStatus.Active)
      validationResult <- runValidation(processDate)
      _ <- batchControl.terminate("TRNVAL00", processDate, validationResult.returnCode)
      
      // Run POSUPD00 equivalent if validation succeeded
      _ <- Async[F].whenA(validationResult.returnCode <= 4) {
        for {
          _ <- batchControl.updateStatus("POSUPD00", processDate, BatchStatus.Active)
          updateResult <- runPositionUpdates(processDate)
          _ <- batchControl.terminate("POSUPD00", processDate, updateResult.returnCode)
        } yield ()
      }
      
      // Run HISTLD00 equivalent if updates succeeded
      _ <- Async[F].whenA(validationResult.returnCode <= 4) {
        for {
          _ <- batchControl.updateStatus("HISTLD00", processDate, BatchStatus.Active)
          loadResult <- runHistoryLoad(processDate)
          _ <- batchControl.terminate("HISTLD00", processDate, loadResult.returnCode)
        } yield ()
      }
      
      // Finalize sequence
      result <- sequenceService.terminateSequence(processDate)
    } yield BatchResult(processDate, result)
  }
}
```

#### 3.6.2 Parallel Operation Framework

During migration, both COBOL and Scala systems run in parallel with result comparison:

```scala
// Parallel execution coordinator
trait ParallelExecutionService[F[_]] {
  def runParallel(
    processDate: LocalDate,
    cobolRunner: CobolBatchRunner,
    scalaBatch: BatchPipeline[F]
  ): F[ComparisonResult]
}

case class ComparisonResult(
  processDate: LocalDate,
  positionDifferences: List[PositionDifference],
  historyDifferences: List[HistoryDifference],
  performanceComparison: PerformanceMetrics,
  recommendation: MigrationReadiness
)

// Comparison logic
class ResultComparator[F[_]: Async] {
  def comparePositions(
    cobolPositions: List[Position],
    scalaPositions: List[Position]
  ): F[List[PositionDifference]] = {
    // Compare each position field
    // Account for decimal precision differences
    // Report any mismatches
    ???
  }
  
  def compareHistory(
    cobolHistory: List[PosHistRecord],
    scalaHistory: List[PosHistRecord]
  ): F[List[HistoryDifference]] = {
    // Compare each history record
    // Account for timestamp differences
    // Report any mismatches
    ???
  }
}
```

**Parallel Operation Modes**:

1. **Shadow Mode** (Weeks 21-22): Scala system processes same input, results compared but not used
2. **Verification Mode** (Weeks 23-24): Scala results verified against COBOL, discrepancies investigated
3. **Primary Mode** (Post-migration): Scala becomes primary, COBOL available for fallback

**Deliverables**:
- Complete batch pipeline orchestration
- Parallel execution framework
- Result comparison tools
- Performance benchmarking
- Migration readiness dashboard

## 4. Parallel Operation Strategy

### 4.1 Architecture for Parallel Operation

```
                    +------------------+
                    | Input Files      |
                    | (Transactions)   |
                    +--------+---------+
                             |
              +--------------+--------------+
              |                             |
              v                             v
    +------------------+          +------------------+
    | COBOL System     |          | Scala System     |
    | (Production)     |          | (Shadow)         |
    +--------+---------+          +--------+---------+
              |                             |
              v                             v
    +------------------+          +------------------+
    | VSAM Files       |          | RocksDB/Ignite   |
    | DB2 Tables       |          | PostgreSQL/DB2   |
    +--------+---------+          +--------+---------+
              |                             |
              +--------------+--------------+
                             |
                             v
                    +------------------+
                    | Comparison       |
                    | Service          |
                    +------------------+
                             |
                             v
                    +------------------+
                    | Discrepancy      |
                    | Reports          |
                    +------------------+
```

### 4.2 Data Synchronization

During parallel operation, maintain data consistency:

1. **Input Synchronization**: Both systems process identical input files
2. **Checkpoint Alignment**: Scala checkpoints map to COBOL checkpoint positions
3. **Output Comparison**: Automated comparison of all outputs after each run

### 4.3 Cutover Criteria

Migration to Scala-primary requires meeting these criteria:

| Criterion | Threshold | Measurement |
|-----------|-----------|-------------|
| Functional Equivalence | 100% | All outputs match COBOL within tolerance |
| Performance | <= 110% of COBOL | Processing time comparison |
| Recovery Success | 100% | All recovery scenarios pass |
| Error Handling | 100% | All error conditions handled identically |
| Parallel Runs | 10 consecutive | Successful parallel runs without discrepancy |

### 4.4 Rollback Plan

If issues arise after cutover:

1. **Immediate Rollback**: Switch back to COBOL system (< 1 hour)
2. **Data Recovery**: Restore from last COBOL checkpoint
3. **Investigation**: Analyze Scala system issues
4. **Remediation**: Fix issues and return to parallel operation

## 5. Risk Mitigation

### 5.1 Technical Risks

| Risk | Mitigation |
|------|------------|
| Decimal precision differences | Use BigDecimal with explicit scale matching COBOL COMP-3 |
| Date/time handling | Use java.time with explicit timezone handling |
| Character encoding | Ensure EBCDIC to UTF-8 conversion is consistent |
| Transaction ordering | Preserve COBOL sequential processing semantics |
| Checkpoint compatibility | Design checkpoint format for cross-system restart |

### 5.2 Operational Risks

| Risk | Mitigation |
|------|------------|
| Extended parallel operation cost | Optimize Scala resource usage, plan for 3-month parallel |
| Staff training | Provide Scala training for operations team |
| Monitoring gaps | Implement comprehensive logging matching COBOL output |
| Incident response | Document runbooks for both systems |

### 5.3 Data Risks

| Risk | Mitigation |
|------|------------|
| Data corruption | Implement checksums and validation at each stage |
| Lost transactions | Ensure exactly-once processing semantics |
| Inconsistent state | Use distributed transactions where needed |
| Audit trail gaps | Maintain complete audit logging in both systems |

## 6. Testing Strategy

### 6.1 Test Categories

**Unit Tests**: Test individual components in isolation
- Validation rules
- Position calculations
- Checkpoint operations
- Error handling

**Integration Tests**: Test component interactions
- Database operations
- File I/O
- Service communication

**Functional Tests**: Test business logic
- End-to-end transaction processing
- Recovery scenarios
- Edge cases

**Comparison Tests**: Compare against COBOL
- Output file comparison
- Database state comparison
- Performance comparison

### 6.2 Test Data

Use TSTGEN00 to generate test data for both systems:

1. **Normal Cases**: Standard transactions across all types
2. **Edge Cases**: Boundary values, special characters
3. **Error Cases**: Invalid data, missing fields
4. **Volume Cases**: High-volume processing for performance testing
5. **Recovery Cases**: Interrupted processing for checkpoint testing

### 6.3 Acceptance Criteria

| Test Category | Pass Criteria |
|---------------|---------------|
| Unit Tests | 100% pass, 80% code coverage |
| Integration Tests | 100% pass |
| Functional Tests | 100% pass |
| Comparison Tests | 100% output match |
| Performance Tests | Within 110% of COBOL baseline |
| Recovery Tests | 100% successful recovery |

## 7. Timeline Summary

| Stage | Duration | Key Deliverables |
|-------|----------|------------------|
| Stage 1: Foundation | Weeks 1-4 | Error handling, DB connectivity, Checkpoint service, Operational store |
| Stage 2: Batch Control | Weeks 5-8 | Batch control, Sequence manager, Recovery handler |
| Stage 3: Validation | Weeks 9-12 | Transaction validation service, Validation rules |
| Stage 4: Position Update | Weeks 13-16 | Position update service, History recording |
| Stage 5: History Load | Weeks 17-20 | History load service, DB2 integration |
| Stage 6: Integration | Weeks 21-24 | Pipeline orchestration, Parallel operation |
| Parallel Operation | Weeks 25-36 | Shadow mode, Verification mode, Cutover |

**Total Duration**: 36 weeks (9 months)

## 8. Success Metrics

### 8.1 Migration Success

- All COBOL functionality replicated in Scala
- Zero data loss during migration
- Successful cutover with no rollback required
- Operations team fully trained on new system

### 8.2 Operational Success

- Processing time within 110% of COBOL baseline
- 99.9% availability maintained during migration
- All recovery scenarios functional
- Complete audit trail maintained

### 8.3 Business Success

- No business disruption during migration
- Reduced maintenance costs post-migration
- Improved developer productivity
- Foundation for future enhancements

## Appendix A: COBOL to Scala Type Mappings

| COBOL Type | Scala Type | Notes |
|------------|------------|-------|
| PIC X(n) | String | Fixed-length, pad/truncate as needed |
| PIC 9(n) | Int/Long | Based on size |
| PIC S9(n) COMP | Int/Long | Signed binary |
| PIC S9(n)V9(m) COMP-3 | BigDecimal | Packed decimal, preserve scale |
| PIC X(8) (date) | LocalDate | Parse YYYYMMDD format |
| PIC X(6) (time) | LocalTime | Parse HHMMSS format |
| PIC X(26) (timestamp) | Instant | Full timestamp with timezone |

## Appendix B: Error Code Mapping

| COBOL Error | Scala Exception | Handling |
|-------------|-----------------|----------|
| File Status '00' | Success | Continue processing |
| File Status '10' | EndOfFileException | Normal EOF handling |
| File Status '23' | RecordNotFoundException | Log and continue or fail based on context |
| SQLCODE 0 | Success | Continue processing |
| SQLCODE -803 | DuplicateKeyException | Skip record (matching COBOL behavior) |
| SQLCODE -911 | DeadlockException | Retry with backoff |

## Appendix C: Checkpoint File Format

```json
{
  "programId": "HISTLD00",
  "runDate": "2025-01-25",
  "runTime": "18:30:00",
  "status": "ACTIVE",
  "counters": {
    "recordsRead": 150000,
    "recordsProcessed": 149500,
    "recordsError": 500,
    "restartCount": 0
  },
  "position": {
    "lastKey": "PORT0001|20250125|183000|0001",
    "lastTime": "2025-01-25T18:45:00Z",
    "phase": "PROCESS"
  },
  "resources": [
    {
      "fileName": "TRANHIST",
      "filePosition": "PORT0001|20250125|183000|0001",
      "fileStatus": "00"
    }
  ],
  "controlInfo": {
    "commitFrequency": 1000,
    "maxErrors": 100,
    "maxRestarts": 3,
    "restartMode": "NORMAL"
  }
}
```

## Appendix D: Glossary

| Term | Definition |
|------|------------|
| BCHCTL | Batch Control - manages job execution and status |
| Checkpoint | Saved processing state for restart capability |
| COMP-3 | Packed decimal format used in COBOL |
| HISTLD | History Load - transfers data from VSAM to DB2 |
| POSUPD | Position Update - updates portfolio positions |
| PRCSEQ | Process Sequence - defines job dependencies |
| RCVPRC | Recovery Process - handles restart and recovery |
| TRNVAL | Transaction Validation - validates input transactions |
| VSAM | Virtual Storage Access Method - IBM indexed file system |
