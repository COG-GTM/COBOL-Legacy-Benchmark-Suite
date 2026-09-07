package com.portfolio.domain;
import jakarta.persistence.*; import java.io.Serializable; import java.util.Objects;
@Embeddable public class HistoryKey implements Serializable {
    @Column(name="PORTFOLIO_ID",columnDefinition="char(8)") private String portfolioId; @Column(name="HIST_DATE",columnDefinition="char(8)") private String histDate; @Column(name="HIST_TIME",columnDefinition="char(6)") private String histTime; @Column(name="SEQ_NO",columnDefinition="char(4)") private String seqNo;
    public HistoryKey(){} public HistoryKey(String p,String d,String t,String s){portfolioId=p;histDate=d;histTime=t;seqNo=s;}
    public String getPortfolioId(){return portfolioId;} public void setPortfolioId(String v){portfolioId=v;} public String getHistDate(){return histDate;} public void setHistDate(String v){histDate=v;} public String getHistTime(){return histTime;} public void setHistTime(String v){histTime=v;} public String getSeqNo(){return seqNo;} public void setSeqNo(String v){seqNo=v;}
    public boolean equals(Object o){if(this==o)return true;if(!(o instanceof HistoryKey x))return false;return Objects.equals(portfolioId,x.portfolioId)&&Objects.equals(histDate,x.histDate)&&Objects.equals(histTime,x.histTime)&&Objects.equals(seqNo,x.seqNo);} public int hashCode(){return Objects.hash(portfolioId,histDate,histTime,seqNo);}
}
