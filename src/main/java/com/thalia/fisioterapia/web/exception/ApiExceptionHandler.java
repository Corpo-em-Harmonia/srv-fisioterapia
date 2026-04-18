package com.thalia.fisioterapia.web.exception;

import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.application.exception.PlanoForaValidadeException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AgendaConflictException.class)
    public ResponseEntity<AgendaConflictResponse> handleAgendaConflict(AgendaConflictException ex) {
        var conflitos = ex.getConflitos().stream()
                .map(c -> new AgendaConflictResponse.ConflitoItem(
                        c.sessaoId(),
                        c.dataHora().toString(),
                        c.pacienteNome()
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new AgendaConflictResponse(
                        "CONFLITO_AGENDA",
                        ex.getMessage(),
                        conflitos
                )
        );
    }

    @ExceptionHandler(PlanoForaValidadeException.class)
    public ResponseEntity<PlanoForaValidadeResponse> handlePlanoForaValidade(PlanoForaValidadeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                new PlanoForaValidadeResponse(
                        "PLANO_FORA_DA_VALIDADE",
                        ex.getMessage(),
                        new PlanoForaValidadeResponse.Detalhes(
                                ex.getDuracaoDias(),
                                ex.getValidadeGuiaDias(),
                                ex.getFrequenciaMinimaSugerida()
                        )
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Dados de entrada inválidos");
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Erro inesperado na API", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message
                )
        );
    }
}
