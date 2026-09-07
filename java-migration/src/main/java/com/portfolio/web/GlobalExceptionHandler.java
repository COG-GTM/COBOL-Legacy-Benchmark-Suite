package com.portfolio.web;
import com.portfolio.service.*; import com.portfolio.config.ErrorLoggingService; import com.portfolio.domain.*; import org.springframework.http.*; import org.springframework.security.access.AccessDeniedException; import org.springframework.stereotype.Controller; import org.springframework.web.bind.annotation.*; import java.util.Map;
@ControllerAdvice public class GlobalExceptionHandler {
 private final ErrorLoggingService errors; public GlobalExceptionHandler(ErrorLoggingService e){errors=e;}
 @ExceptionHandler(BusinessException.class) public Object business(BusinessException e){errors.log("WEB",ErrorType.APPLICATION,ErrorSeverity.WARNING,e.getCode(),e.getMessage(),"");return response(e.getCode(),e.getMessage());}
 @ExceptionHandler(ResourceNotFoundException.class) public Object notFound(ResourceNotFoundException e){return response("E002",e.getMessage());}
 @ExceptionHandler(AccessDeniedException.class) public Object denied(AccessDeniedException e){return response("E006",e.getMessage());}
 @ExceptionHandler(Exception.class) public Object generic(Exception e){errors.log("WEB",ErrorType.SYSTEM,ErrorSeverity.SEVERE,"E007",e.getMessage(),"");return response("E007","Internal server error");}
 private ResponseEntity<Map<String,String>> response(String code,String message){return ResponseEntity.status(code.equals("E002")?HttpStatus.NOT_FOUND:code.equals("E006")?HttpStatus.FORBIDDEN:code.equals("E007")?HttpStatus.INTERNAL_SERVER_ERROR:HttpStatus.BAD_REQUEST).body(Map.of("code",code,"message",message));}
}
