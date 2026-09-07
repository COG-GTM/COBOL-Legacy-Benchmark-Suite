package com.portfolio.service;

import com.portfolio.domain.AuditAction;
import com.portfolio.domain.AuditLog;
import com.portfolio.domain.AuditStatus;
import com.portfolio.domain.AuditType;
import com.portfolio.domain.ClientType;
import com.portfolio.domain.InvestmentPosition;
import com.portfolio.domain.Portfolio;
import com.portfolio.domain.PortfolioStatus;
import com.portfolio.dto.PortfolioDto;
import com.portfolio.repository.AuditLogRepository;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {
  public enum UpdateType {
    S,
    V,
    N
  }

  private final PortfolioRepository portfolioRepository;
  private final InvestmentPositionRepository positionRepository;
  private final AuditLogRepository auditLogRepository;
  private final PortfolioValidationService validationService;

  public PortfolioService(
      PortfolioRepository portfolioRepository,
      InvestmentPositionRepository positionRepository,
      AuditLogRepository auditLogRepository,
      PortfolioValidationService validationService) {
    this.portfolioRepository = portfolioRepository;
    this.positionRepository = positionRepository;
    this.auditLogRepository = auditLogRepository;
    this.validationService = validationService;
  }

  @Transactional
  public Portfolio create(PortfolioDto portfolioDto) {
    validationService.requirePortfolioId(portfolioDto.getPortfolioId());
    validationService.requireAccountNo(portfolioDto.getAccountNo());
    if (portfolioDto.getClientName() == null || portfolioDto.getClientName().isBlank())
      throw new BusinessException("E001", "Client name is required");
    if (portfolioRepository.existsById(portfolioDto.getPortfolioId()))
      throw new BusinessException("E003", "Portfolio ID already exists");
    Portfolio portfolio = new Portfolio();
    portfolio.setPortfolioId(portfolioDto.getPortfolioId());
    portfolio.setAccountNo(portfolioDto.getAccountNo());
    portfolio.setClientName(portfolioDto.getClientName());
    portfolio.setClientType(clientTypeFromCode(portfolioDto.getClientType()));
    portfolio.setCreateDate(
        portfolioDto.getCreateDate() == null ? LocalDate.now() : portfolioDto.getCreateDate());
    portfolio.setLastMaintDate(LocalDate.now());
    portfolio.setStatus(statusFromCode(portfolioDto.getStatus()));
    portfolio.setTotalValue(zeroIfNull(portfolioDto.getTotalValue()));
    portfolio.setCashBalance(zeroIfNull(portfolioDto.getCashBalance()));
    portfolio.setAccountType(portfolioDto.getAccountType());
    portfolio.setBranchId(portfolioDto.getBranchId());
    portfolio.setCurrencyCode(
        portfolioDto.getCurrencyCode() == null ? "USD" : portfolioDto.getCurrencyCode());
    portfolio.setRiskLevel(portfolioDto.getRiskLevel());
    return portfolioRepository.save(portfolio);
  }

  private BigDecimal zeroIfNull(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }

  private ClientType clientTypeFromCode(String code) {
    try {
      return ClientType.fromCode(code);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException("E008", "Invalid Client Type");
    }
  }

  private PortfolioStatus statusFromCode(String code) {
    if (code == null) {
      return PortfolioStatus.ACTIVE;
    }
    try {
      return PortfolioStatus.fromCode(code);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException("E008", "Invalid Status");
    }
  }

  public Portfolio read(String portfolioId) {
    return portfolioRepository
        .findById(portfolioId)
        .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
  }

  @Transactional
  public void delete(String portfolioId) {
    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for deletion"));
    portfolioRepository.delete(portfolio);
    writeAudit(AuditAction.DELETE, portfolio, "PORTDEL");
  }

  @Transactional
  public Portfolio applyUpdate(String portfolioId, UpdateType updateType, String newValue) {
    Portfolio portfolio =
        portfolioRepository
            .findById(portfolioId)
            .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
    switch (updateType) {
      case S -> portfolio.setStatus(statusFromCode(newValue));
      case V -> portfolio.setTotalValue(parseAmount(newValue));
      case N -> portfolio.setClientName(newValue);
    }
    portfolio.setLastMaintDate(LocalDate.now());
    return portfolioRepository.save(portfolio);
  }

  private BigDecimal parseAmount(String newValue) {
    try {
      return new BigDecimal(newValue).setScale(2, RoundingMode.HALF_UP);
    } catch (NumberFormatException exception) {
      throw new BusinessException("E008", "Invalid amount");
    }
  }

  public List<Portfolio> findAll() {
    return portfolioRepository.findAll();
  }

  public Optional<Portfolio> findByAccountNo(String accountNo) {
    return portfolioRepository.findByAccountNo(accountNo);
  }

  public InvestmentPosition savePosition(InvestmentPosition position) {
    return positionRepository.save(position);
  }

  public List<InvestmentPosition> positionsForAccount(String accountNo) {
    return positionRepository.findByAccountNo(accountNo);
  }

  public List<InvestmentPosition> positionsForPortfolio(String portfolioId) {
    return positionRepository.findByIdPortfolioIdOrderByIdInvestmentId(portfolioId);
  }

  private void writeAudit(AuditAction action, Portfolio portfolio, String program) {
    AuditLog auditLog = new AuditLog();
    auditLog.setAudTimestamp(java.time.LocalDateTime.now());
    auditLog.setProgram(program);
    auditLog.setAudType(AuditType.TRAN);
    auditLog.setAction(action);
    auditLog.setStatus(AuditStatus.SUCCESS);
    auditLog.setPortfolioId(portfolio.getPortfolioId());
    auditLog.setAccountNo(portfolio.getAccountNo());
    auditLogRepository.save(auditLog);
  }
}
