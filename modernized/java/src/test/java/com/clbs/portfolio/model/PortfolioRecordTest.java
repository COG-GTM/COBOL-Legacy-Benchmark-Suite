package com.clbs.portfolio.model;

import com.clbs.portfolio.harness.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PORTFLIO.cpy, plus the two synthetic holdings fields PORTTRAN expects but no copybook defines. */
class PortfolioRecordTest {

    @Test
    @DisplayName("PORT-ID is eight bytes, so a documented nine-character id loses its last digit")
    void recordKeyIsEightBytes() {
        PortfolioRecord portfolio = new PortfolioRecord();
        portfolio.setPortId(TestData.DOCUMENTED_GROWTH_PORTFOLIO_ID);

        assertEquals("PORT0000", portfolio.getPortId());
        assertEquals(18, portfolio.getPortKey().length());
    }

    @Test
    @DisplayName("financial fields hold two decimals")
    void financialScales() {
        PortfolioRecord portfolio = TestData.growthPortfolio();
        assertEquals(new BigDecimal("12345678.99"), portfolio.getPortTotalValue());
        assertEquals(2, portfolio.getPortCashBalance().scale());

        portfolio.setPortTotalValue(new BigDecimal("1.239"));
        assertEquals(new BigDecimal("1.23"), portfolio.getPortTotalValue());
    }

    @Test
    @DisplayName("the level-88 statuses are A, C and S; the documented I is not one of them")
    void statuses() {
        PortfolioRecord portfolio = TestData.growthPortfolio();
        assertTrue(portfolio.isPortActive());
        assertEquals(PortfolioStatus.ACTIVE, portfolio.getPortfolioStatus());

        portfolio.setPortStatus("I");
        assertNull(portfolio.getPortfolioStatus());
        assertEquals("I", portfolio.getPortStatus());
    }

    @Test
    @DisplayName("client type maps the three level-88 values")
    void clientTypes() {
        PortfolioRecord portfolio = TestData.growthPortfolio();
        assertEquals(ClientType.INDIVIDUAL, portfolio.getClientType());

        portfolio.setPortClientType(ClientType.TRUST);
        assertEquals("T", portfolio.getPortClientType());
    }

    @Test
    @DisplayName("PIC 9(8) dates drop a sign and any overflow")
    void displayDates() {
        PortfolioRecord portfolio = new PortfolioRecord();
        portfolio.setPortCreateDate(20240320);
        portfolio.setPortLastMaint(-20240321);

        assertEquals(20240320, portfolio.getPortCreateDate());
        assertEquals(20240321, portfolio.getPortLastMaint());
    }

    @Test
    @DisplayName("the synthetic holdings fields carry POSREC scales")
    void syntheticHoldingsFields() {
        PortfolioRecord portfolio = TestData.growthPortfolio();
        assertEquals(4, portfolio.getPortTotalUnits().scale());
        assertEquals(2, portfolio.getPortTotalCost().scale());

        portfolio.setPortTotalUnits(new BigDecimal("10.99999"));
        assertEquals(new BigDecimal("10.9999"), portfolio.getPortTotalUnits());
    }

    @Test
    @DisplayName("the record image covers only fields PORTFLIO.cpy declares")
    void recordImageExcludesSyntheticFields() {
        PortfolioRecord portfolio = TestData.growthPortfolio();
        String image = portfolio.toRecordImage();

        assertTrue(image.startsWith("PORT0001" + "0000000001" + CobolText.picX("GROWTH PORTFOLIO", 30)));
        assertTrue(image.contains(CobolDecimal.image(portfolio.getPortTotalValue(), 13, 2)));

        portfolio.setPortTotalUnits("9999.0000");
        assertEquals(image, portfolio.toRecordImage());
    }

    @Test
    @DisplayName("copying detaches a record from the shared file area")
    void copyIsIndependent() {
        PortfolioRecord original = TestData.growthPortfolio();
        PortfolioRecord copy = new PortfolioRecord(original);
        original.setPortTotalUnits("0");

        assertEquals(new BigDecimal("1000.0000"), copy.getPortTotalUnits());
    }
}
