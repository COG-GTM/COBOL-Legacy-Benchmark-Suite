package com.portfolio.service;

import com.portfolio.dto.BatchProcessingResult;
import com.portfolio.dto.PortfolioResponse;
import com.portfolio.dto.PortfolioUpdateRequest;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.enums.AuditStatus;
import com.portfolio.model.enums.ClientType;
import com.portfolio.model.enums.PortfolioStatus;
import com.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioById(String portfolioId) {
        Portfolio portfolio = portfolioRepository.findByPortfolioId(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId, true));
        return mapToResponse(portfolio);
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioByAccountNo(String accountNo) {
        Portfolio portfolio = portfolioRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new PortfolioNotFoundException(accountNo, false));
        return mapToResponse(portfolio);
    }

    @Transactional(readOnly = true)
    public List<PortfolioResponse> getAllPortfolios() {
        return portfolioRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PortfolioResponse> getPortfoliosByStatus(PortfolioStatus status) {
        return portfolioRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PortfolioResponse> searchPortfoliosByName(String clientName) {
        return portfolioRepository.findByClientNameContainingIgnoreCase(clientName).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public PortfolioResponse createPortfolio(String portfolioId, String accountNo, String clientName, 
                                             ClientType clientType, String userId) {
        log.info("Creating portfolio: {}", portfolioId);

        Portfolio portfolio = Portfolio.builder()
                .portfolioId(portfolioId)
                .accountNo(accountNo)
                .clientName(clientName)
                .clientType(clientType)
                .createDate(LocalDate.now())
                .status(PortfolioStatus.ACTIVE)
                .totalValue(BigDecimal.ZERO)
                .cashBalance(BigDecimal.ZERO)
                .totalUnits(BigDecimal.ZERO)
                .totalCost(BigDecimal.ZERO)
                .lastUser(userId)
                .build();

        portfolio = portfolioRepository.save(portfolio);

        auditService.createPortfolioUpdateAudit(portfolio, null, userId, 
                AuditStatus.SUCCESS, "Portfolio created");

        return mapToResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse updatePortfolioStatus(String portfolioId, PortfolioStatus status, String userId) {
        log.info("Updating status for portfolio: {} to {}", portfolioId, status);

        Portfolio portfolio = portfolioRepository.findByPortfolioId(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId, true));

        String beforeImage = buildPortfolioImage(portfolio);

        portfolio.setStatus(status);
        portfolio.setLastUser(userId);
        portfolio.setLastMaintDate(LocalDate.now());

        portfolio = portfolioRepository.save(portfolio);

        auditService.createPortfolioUpdateAudit(portfolio, beforeImage, userId, 
                AuditStatus.SUCCESS, "Status updated to: " + status);

        log.info("Status updated successfully for portfolio: {}", portfolioId);
        return mapToResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse updatePortfolioName(String portfolioId, String clientName, String userId) {
        log.info("Updating client name for portfolio: {}", portfolioId);

        Portfolio portfolio = portfolioRepository.findByPortfolioId(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId, true));

        String beforeImage = buildPortfolioImage(portfolio);

        portfolio.setClientName(clientName);
        portfolio.setLastUser(userId);
        portfolio.setLastMaintDate(LocalDate.now());

        portfolio = portfolioRepository.save(portfolio);

        auditService.createPortfolioUpdateAudit(portfolio, beforeImage, userId, 
                AuditStatus.SUCCESS, "Client name updated to: " + clientName);

        log.info("Client name updated successfully for portfolio: {}", portfolioId);
        return mapToResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse updatePortfolioValue(String portfolioId, BigDecimal totalValue, String userId) {
        log.info("Updating total value for portfolio: {}", portfolioId);

        Portfolio portfolio = portfolioRepository.findByPortfolioId(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId, true));

        String beforeImage = buildPortfolioImage(portfolio);

        portfolio.setTotalValue(totalValue);
        portfolio.setLastUser(userId);
        portfolio.setLastMaintDate(LocalDate.now());

        portfolio = portfolioRepository.save(portfolio);

        auditService.createPortfolioUpdateAudit(portfolio, beforeImage, userId, 
                AuditStatus.SUCCESS, "Total value updated to: " + totalValue);

        log.info("Total value updated successfully for portfolio: {}", portfolioId);
        return mapToResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse updatePortfolio(String portfolioId, PortfolioUpdateRequest request) {
        log.info("Applying updates for portfolio: {}", portfolioId);

        Portfolio portfolio = portfolioRepository.findByPortfolioId(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId, true));

        String beforeImage = buildPortfolioImage(portfolio);
        StringBuilder updateMessage = new StringBuilder("Updates applied: ");

        if (request.getStatus() != null) {
            portfolio.setStatus(request.getStatus());
            updateMessage.append("status=").append(request.getStatus()).append("; ");
        }

        if (request.getClientName() != null) {
            portfolio.setClientName(request.getClientName());
            updateMessage.append("name=").append(request.getClientName()).append("; ");
        }

        if (request.getTotalValue() != null) {
            portfolio.setTotalValue(request.getTotalValue());
            updateMessage.append("value=").append(request.getTotalValue()).append("; ");
        }

        portfolio.setLastUser(request.getUserId());
        portfolio.setLastMaintDate(LocalDate.now());

        portfolio = portfolioRepository.save(portfolio);

        auditService.createPortfolioUpdateAudit(portfolio, beforeImage, request.getUserId(), 
                AuditStatus.SUCCESS, updateMessage.toString());

        log.info("Updates applied successfully for portfolio: {}", portfolioId);
        return mapToResponse(portfolio);
    }

    @Transactional
    public BatchProcessingResult processBatchUpdates(List<PortfolioUpdateBatchItem> updates) {
        log.info("Processing batch of {} portfolio updates", updates.size());

        BatchProcessingResult result = BatchProcessingResult.builder()
                .recordsRead(0)
                .recordsProcessed(0)
                .errorCount(0)
                .status("IN_PROGRESS")
                .build();

        for (PortfolioUpdateBatchItem item : updates) {
            result.incrementRead();

            try {
                Portfolio portfolio = portfolioRepository.findByPortfolioId(item.portfolioId())
                        .orElseThrow(() -> new PortfolioNotFoundException(item.portfolioId(), true));

                String beforeImage = buildPortfolioImage(portfolio);

                switch (item.action()) {
                    case 'S' -> portfolio.setStatus(PortfolioStatus.valueOf(item.newValue()));
                    case 'N' -> portfolio.setClientName(item.newValue());
                    case 'V' -> portfolio.setTotalValue(new BigDecimal(item.newValue()));
                    default -> throw new IllegalArgumentException("Unknown action: " + item.action());
                }

                portfolio.setLastUser(item.userId());
                portfolio.setLastMaintDate(LocalDate.now());
                portfolioRepository.save(portfolio);

                auditService.createPortfolioUpdateAudit(portfolio, beforeImage, item.userId(), 
                        AuditStatus.SUCCESS, "Batch update: action=" + item.action());

                result.incrementProcessed();

            } catch (Exception e) {
                log.error("Error updating portfolio {}: {}", item.portfolioId(), e.getMessage());
                result.addError(String.format("Portfolio %s: %s", item.portfolioId(), e.getMessage()));
            }
        }

        result.setStatus("COMPLETED");
        result.setMessage(String.format("Batch updates completed. Read: %d, Processed: %d, Errors: %d",
                result.getRecordsRead(), result.getRecordsProcessed(), result.getErrorCount()));

        log.info("Updates processed: {}", result.getRecordsProcessed());
        log.info("Errors occurred:  {}", result.getErrorCount());

        return result;
    }

    private String buildPortfolioImage(Portfolio portfolio) {
        return String.format("ID=%s,Status=%s,Name=%s,Value=%s,Units=%s,Cost=%s",
                portfolio.getPortfolioId(),
                portfolio.getStatus(),
                portfolio.getClientName(),
                portfolio.getTotalValue(),
                portfolio.getTotalUnits(),
                portfolio.getTotalCost());
    }

    private PortfolioResponse mapToResponse(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .portfolioId(portfolio.getPortfolioId())
                .accountNo(portfolio.getAccountNo())
                .clientName(portfolio.getClientName())
                .clientType(portfolio.getClientType())
                .createDate(portfolio.getCreateDate())
                .lastMaintDate(portfolio.getLastMaintDate())
                .status(portfolio.getStatus())
                .totalValue(portfolio.getTotalValue())
                .cashBalance(portfolio.getCashBalance())
                .totalUnits(portfolio.getTotalUnits())
                .totalCost(portfolio.getTotalCost())
                .lastUser(portfolio.getLastUser())
                .lastTransDate(portfolio.getLastTransDate())
                .build();
    }

    public record PortfolioUpdateBatchItem(
            String portfolioId,
            String accountNo,
            char action,
            String newValue,
            String userId
    ) {}
}
