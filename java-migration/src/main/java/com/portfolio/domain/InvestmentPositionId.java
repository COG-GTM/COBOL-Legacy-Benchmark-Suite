package com.portfolio.domain;
import jakarta.persistence.*; import java.io.Serializable; import java.time.LocalDate; import java.util.Objects;
@Embeddable public class InvestmentPositionId implements Serializable {
    @Column(name="PORTFOLIO_ID", columnDefinition="char(8)") private String portfolioId;
    @Column(name="INVESTMENT_ID", columnDefinition="char(10)") private String investmentId;
    @Column(name="POSITION_DATE") private LocalDate positionDate;
    public InvestmentPositionId() {} public InvestmentPositionId(String p,String i,LocalDate d){portfolioId=p;investmentId=i;positionDate=d;}
    public String getPortfolioId(){return portfolioId;} public void setPortfolioId(String v){portfolioId=v;} public String getInvestmentId(){return investmentId;} public void setInvestmentId(String v){investmentId=v;} public LocalDate getPositionDate(){return positionDate;} public void setPositionDate(LocalDate v){positionDate=v;}
    public boolean equals(Object o){if(this==o)return true;if(!(o instanceof InvestmentPositionId x))return false;return Objects.equals(portfolioId,x.portfolioId)&&Objects.equals(investmentId,x.investmentId)&&Objects.equals(positionDate,x.positionDate);}
    public int hashCode(){return Objects.hash(portfolioId,investmentId,positionDate);}
}
