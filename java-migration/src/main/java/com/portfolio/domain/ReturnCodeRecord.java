package com.portfolio.domain;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="RTNCODES")
public class ReturnCodeRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long rtnId; @Column(name="RC_TIMESTAMP") private LocalDateTime rcTimestamp; @Column(name="PROGRAM_ID",columnDefinition="char(8)") private String programId; private int returnCode; private int highestCode; @Column(name="STATUS_CODE",columnDefinition="char(1)") private String statusCode; @Column(name="MESSAGE_TEXT",length=80) private String messageText;
 public ReturnCodeRecord(){} public Long getRtnId(){return rtnId;} public void setRtnId(Long v){rtnId=v;} public LocalDateTime getRcTimestamp(){return rcTimestamp;} public void setRcTimestamp(LocalDateTime v){rcTimestamp=v;} public String getProgramId(){return programId;} public void setProgramId(String v){programId=v;} public int getReturnCode(){return returnCode;} public void setReturnCode(int v){returnCode=v;} public int getHighestCode(){return highestCode;} public void setHighestCode(int v){highestCode=v;} public String getStatusCode(){return statusCode;} public void setStatusCode(String v){statusCode=v;} public String getMessageText(){return messageText;} public void setMessageText(String v){messageText=v;}
}
