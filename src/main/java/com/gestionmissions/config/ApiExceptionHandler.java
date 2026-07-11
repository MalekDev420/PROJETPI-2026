package com.gestionmissions.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String,String>> handle(Exception e){ return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()==null?"Erreur serveur":e.getMessage())); }
}
