package com.portfolio;
import com.portfolio.service.PortfolioValidationService; import org.junit.jupiter.api.Test; import java.math.BigDecimal; import static org.junit.jupiter.api.Assertions.*;
class PortfolioValidationServiceTest {
 private final PortfolioValidationService service=new PortfolioValidationService();
 @Test void ids(){assertTrue(service.validatePortfolioId("PORT0001").valid());assertFalse(service.validatePortfolioId("PORTABCD").valid());assertFalse(service.validatePortfolioId("ABCD0001").valid());assertFalse(service.validatePortfolioId("PORT001").valid());}
 @Test void accounts(){assertTrue(service.validateAccountNo("1234567890").valid());assertFalse(service.validateAccountNo("0000000000").valid());assertFalse(service.validateAccountNo("12345").valid());assertFalse(service.validateAccountNo("12345678AB").valid());}
 @Test void investmentTypes(){for(String t:new String[]{"STK","BND","MMF","ETF"})assertTrue(service.validateInvestmentType(t).valid());assertFalse(service.validateInvestmentType("XYZ").valid());}
 @Test void amounts(){assertTrue(service.validateAmount(new BigDecimal("9999999999999.99")).valid());assertFalse(service.validateAmount(new BigDecimal("10000000000000.00")).valid());assertTrue(service.validateAmount(new BigDecimal("-9999999999999.99")).valid());assertFalse(service.validateAmount(new BigDecimal("-10000000000000.00")).valid());}
}
