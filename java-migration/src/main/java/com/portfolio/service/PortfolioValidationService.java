package com.portfolio.service;
import com.portfolio.common.ValidationConstants; import org.springframework.stereotype.Service; import java.math.BigDecimal; import java.util.Set;
@Service public class PortfolioValidationService {
 public record ValidationResult(int code,String message){public boolean valid(){return code==0;}}
 private static final Set<String> TYPES=Set.of("STK","BND","MMF","ETF");
 public ValidationResult validatePortfolioId(String id){return id!=null&&id.matches("PORT\\d{4}")?new ValidationResult(0,""):new ValidationResult(1,ValidationConstants.INVALID_ID_MESSAGE);}
 public ValidationResult validateAccountNo(String no){return no!=null&&no.matches("\\d{10}")&&!no.matches("0{10}")?new ValidationResult(0,""):new ValidationResult(2,ValidationConstants.INVALID_ACCOUNT_MESSAGE);}
 public ValidationResult validateInvestmentType(String type){return type!=null&&TYPES.contains(type)?new ValidationResult(0,""):new ValidationResult(3,ValidationConstants.INVALID_TYPE_MESSAGE);}
 public ValidationResult validateAmount(BigDecimal amount){return amount!=null&&amount.compareTo(ValidationConstants.MIN_AMOUNT)>=0&&amount.compareTo(ValidationConstants.MAX_AMOUNT)<=0?new ValidationResult(0,""):new ValidationResult(4,ValidationConstants.INVALID_AMOUNT_MESSAGE);}
 public void requirePortfolioId(String v){require(validatePortfolioId(v));} public void requireAccountNo(String v){require(validateAccountNo(v));} public void requireInvestmentType(String v){require(validateInvestmentType(v));} public void requireAmount(BigDecimal v){require(validateAmount(v));}
 private void require(ValidationResult r){if(!r.valid())throw new BusinessException("E008",r.message());}
 public String investmentTypeOf(String investmentId){return investmentId!=null&&investmentId.length()>=3?investmentId.substring(0,3):"";}
}
