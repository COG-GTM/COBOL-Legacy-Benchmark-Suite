package com.cognition.portfolio.transaction.web;

import com.cognition.portfolio.traceability.CobolOrigin;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionKey;
import com.cognition.portfolio.transaction.domain.TransactionStatus;
import com.cognition.portfolio.transaction.service.PortfolioTransactionService;
import com.cognition.portfolio.transaction.service.TransactionAmountCalculator;
import com.cognition.portfolio.transaction.service.TransactionProcessingResult;
import com.cognition.portfolio.transaction.web.dto.CreateTransactionRequest;
import com.cognition.portfolio.transaction.web.dto.ErrorResponse;
import com.cognition.portfolio.transaction.web.dto.ProcessTransactionRequest;
import com.cognition.portfolio.transaction.web.dto.ProcessTransactionResponse;
import com.cognition.portfolio.transaction.web.dto.StatusTransitionRequest;
import com.cognition.portfolio.transaction.web.dto.TransactionResponse;
import com.cognition.portfolio.transaction.web.dto.UpdateTransactionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST facade over the migrated transaction entity. Each operation replaces a COBOL file operation
 * so the contract can be published to the API gateway while the batch programs are retired.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(
    name = "Portfolio Transactions",
    description = "Migrated from COBOL copybook TRNREC and programs PORTTRAN / PORTVALD / PRCSEQ00")
@CobolOrigin(program = "PORTTRAN", paragraph = "0000-MAIN")
public class PortfolioTransactionController {

  private final PortfolioTransactionService service;
  private final TransactionAmountCalculator amountCalculator;

  public PortfolioTransactionController(
      PortfolioTransactionService service, TransactionAmountCalculator amountCalculator) {
    this.service = service;
    this.amountCalculator = amountCalculator;
  }

  /** Keyed read. */
  @GetMapping("/{transactionKey}")
  @Operation(
      summary = "Read a transaction by its VSAM key",
      description =
          "Keyed READ on TRANHIST. The path variable is the 28 character TRN-KEY: "
              + "TRN-DATE(8) + TRN-TIME(6) + TRN-PORTFOLIO-ID(8) + TRN-SEQUENCE-NO(6).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Transaction found"),
    @ApiResponse(responseCode = "400", description = "Malformed TRN-KEY",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "INVALID KEY - no such transaction",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @CobolOrigin(program = "PORTTRAN", paragraph = "2110-CHECK-PORTFOLIO", rules = {"BR-21"})
  public TransactionResponse read(
      @Parameter(description = "28 character TRN-KEY", example = "20240320093015PORT0001000001")
          @PathVariable String transactionKey) {
    return TransactionResponse.from(service.findByKey(TransactionKey.parse(transactionKey)));
  }

  /** Sequential / paged read in VSAM key order. */
  @GetMapping
  @Operation(
      summary = "Browse transactions in VSAM key sequence",
      description =
          "Sequential read of TRANHIST as performed by PORTTRAN 2000-PROCESS-TRANSACTIONS, "
              + "ordered by TRN-DATE, TRN-TIME, TRN-PORTFOLIO-ID, TRN-SEQUENCE-NO (BR-21).")
  @ApiResponse(responseCode = "200", description = "Page of transactions")
  @CobolOrigin(program = "PORTTRAN", paragraph = "2000-PROCESS-TRANSACTIONS", rules = {"BR-21"})
  public Page<TransactionResponse> browse(
      @Parameter(description = "Filter on TRN-PORTFOLIO-ID", example = "PORT0001")
          @RequestParam(required = false) String portfolioId,
      @Parameter(description = "Filter on TRN-STATUS (P, D, F, R)", example = "P")
          @RequestParam(required = false) String status,
      @Parameter(description = "Zero based page index") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
    TransactionStatus statusFilter =
        status == null || status.isBlank()
            ? null
            : TransactionStatus.fromCode(status)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Transaction Status: " + status));
    return service
        .browse(portfolioId, statusFilter, PageRequest.of(page, size))
        .map(TransactionResponse::from);
  }

  /** Insert. */
  @PostMapping
  @Operation(
      summary = "Write a new transaction",
      description =
          "WRITE TRANSACTION-RECORD. Validated by PORTTRAN 2100-VALIDATE-TRANSACTION (BR-01 to "
              + "BR-07). TRN-SEQUENCE-NO is assigned per BR-20 when omitted, and TRN-AMOUNT is "
              + "derived as quantity x price truncated to two decimals per BR-22 when omitted.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Transaction written"),
    @ApiResponse(responseCode = "400", description = "Validation failure carrying the COBOL ERR-TEXT",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "409", description = "Duplicate TRN-KEY (VSAM status 22)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @CobolOrigin(program = "PORTTRAN", paragraph = "2100-VALIDATE-TRANSACTION", rules = {"BR-07", "BR-20", "BR-22"})
  public ResponseEntity<TransactionResponse> insert(@Valid @RequestBody CreateTransactionRequest request) {
    BigDecimal amount =
        request.amount() == null
            ? amountCalculator.computeAmount(request.quantity(), request.price())
            : request.amount();
    boolean assignSequence = request.sequenceNo() == null || request.sequenceNo().isBlank();
    PortfolioTransaction transaction = request.toEntity(request.sequenceNo(), amount);
    PortfolioTransaction saved =
        assignSequence ? service.insertNextInSequence(transaction) : service.insert(transaction);
    return ResponseEntity.created(URI.create("/api/v1/transactions/" + saved.getTrnKey().toKeyString()))
        .body(TransactionResponse.from(saved));
  }

  /** Update / rewrite. */
  @PutMapping("/{transactionKey}")
  @Operation(
      summary = "Rewrite an existing transaction",
      description = "REWRITE TRANSACTION-RECORD. TRN-KEY is immutable; only TRN-DATA and TRN-AUDIT change.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Transaction rewritten"),
    @ApiResponse(responseCode = "400", description = "Validation failure carrying the COBOL ERR-TEXT",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "INVALID KEY - no such transaction",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @CobolOrigin(program = "PORTMSTR", paragraph = "4000-UPDATE-PORTFOLIO", rules = {"BR-07", "BR-22"})
  public TransactionResponse rewrite(
      @Parameter(description = "28 character TRN-KEY", example = "20240320093015PORT0001000001")
          @PathVariable String transactionKey,
      @Valid @RequestBody UpdateTransactionRequest request) {
    BigDecimal amount =
        request.amount() == null
            ? amountCalculator.computeAmount(request.quantity(), request.price())
            : request.amount();
    PortfolioTransaction saved =
        service.rewrite(TransactionKey.parse(transactionKey), request.toCarrier(amount));
    return TransactionResponse.from(saved);
  }

  /** Status transition. */
  @PostMapping("/{transactionKey}/status")
  @Operation(
      summary = "Transition TRN-STATUS",
      description =
          "Applies a TRN-STATUS transition. Allowed transitions (BR-23, derived): P -> D, P -> F, "
              + "D -> R; F and R are terminal.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Status changed"),
    @ApiResponse(responseCode = "400", description = "Transition not allowed",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "INVALID KEY - no such transaction",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @CobolOrigin(program = "TRNREC", paragraph = "TRN-STATUS 88-levels", rules = {"BR-23"}, derived = true)
  public TransactionResponse transitionStatus(
      @Parameter(description = "28 character TRN-KEY", example = "20240320093015PORT0001000001")
          @PathVariable String transactionKey,
      @Valid @RequestBody StatusTransitionRequest request) {
    TransactionStatus target =
        TransactionStatus.fromCode(request.status())
            .orElseThrow(() -> new IllegalArgumentException("Invalid Transaction Status: " + request.status()));
    return TransactionResponse.from(
        service.transitionStatus(TransactionKey.parse(transactionKey), target, request.processUser()));
  }

  /** Position update, i.e. the online equivalent of the batch processing step. */
  @PostMapping("/{transactionKey}/process")
  @Operation(
      summary = "Process a transaction against the portfolio",
      description =
          "Runs PORTTRAN 2100-VALIDATE-TRANSACTION then 2200-UPDATE-POSITIONS and returns the "
              + "deltas for PORT-TOTAL-UNITS and PORT-TOTAL-COST. TR transactions always fail with "
              + "'Transfer processing not implemented' (BR-11), matching the legacy behaviour. "
              + "Only a pending (P) transaction can be processed (BR-23), and availableUnits is "
              + "required for SL because the legacy program reads it from PORTFILE.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Processing outcome, successful or failed"),
    @ApiResponse(responseCode = "400",
        description = "Record is no longer pending, or availableUnits is missing for an SL record",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "INVALID KEY - no such transaction",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @CobolOrigin(program = "PORTTRAN", paragraph = "2200-UPDATE-POSITIONS",
      rules = {"BR-09", "BR-10", "BR-11", "BR-12"})
  public ProcessTransactionResponse process(
      @Parameter(description = "28 character TRN-KEY", example = "20240320093015PORT0001000001")
          @PathVariable String transactionKey,
      @RequestBody(required = false) ProcessTransactionRequest request) {
    BigDecimal availableUnits = request == null ? null : request.availableUnits();
    TransactionProcessingResult result =
        service.process(TransactionKey.parse(transactionKey), availableUnits);
    return ProcessTransactionResponse.from(result);
  }
}
