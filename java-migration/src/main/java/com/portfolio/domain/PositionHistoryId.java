package com.portfolio.domain;
import jakarta.persistence.*; import java.io.Serializable; import java.time.*; import java.util.Objects;
@Embeddable public class PositionHistoryId implements Serializable {
    @Column(name="ACCOUNT_NO",columnDefinition="char(10)") private String accountNo; @Column(name="PORTFOLIO_ID",columnDefinition="char(10)") private String portfolioId; private LocalDate transDate; private LocalTime transTime;
    public PositionHistoryId(){} public PositionHistoryId(String a,String p,LocalDate d,LocalTime t){accountNo=a;portfolioId=p;transDate=d;transTime=t;}
    public String getAccountNo(){return accountNo;} public void setAccountNo(String v){accountNo=v;} public String getPortfolioId(){return portfolioId;} public void setPortfolioId(String v){portfolioId=v;} public LocalDate getTransDate(){return transDate;} public void setTransDate(LocalDate v){transDate=v;} public LocalTime getTransTime(){return transTime;} public void setTransTime(LocalTime v){transTime=v;}
    public boolean equals(Object o){if(this==o)return true;if(!(o instanceof PositionHistoryId x))return false;return Objects.equals(accountNo,x.accountNo)&&Objects.equals(portfolioId,x.portfolioId)&&Objects.equals(transDate,x.transDate)&&Objects.equals(transTime,x.transTime);} public int hashCode(){return Objects.hash(accountNo,portfolioId,transDate,transTime);}
}
