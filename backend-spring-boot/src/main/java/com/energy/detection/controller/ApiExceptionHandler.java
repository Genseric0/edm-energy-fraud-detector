package com.energy.detection.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;

/** Respostas estáveis sem SQL, stack traces ou detalhes internos. */
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> notFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(404).body(Map.of("message", "Recurso não encontrado"));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> badRequest(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "Dados do pedido inválidos"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body(Map.of("message", "Operação em conflito com os dados existentes"));
    }
}
