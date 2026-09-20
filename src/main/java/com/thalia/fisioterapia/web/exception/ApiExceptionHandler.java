package com.thalia.fisioterapia.web.exception;

import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.application.exception.ConflictException;
import com.thalia.fisioterapia.application.exception.PlanoForaValidadeException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Sem este handler, o AccessDeniedException do @PreAuthorize cairia no handler genérico (500).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        String original = ex.getMessage();
        boolean mensagemPadraoDoSpring = original == null
                || original.equalsIgnoreCase("Access Denied")
                || original.equalsIgnoreCase("Acesso negado");
        return buildResponse(HttpStatus.FORBIDDEN,
                mensagemPadraoDoSpring ? "Você não tem permissão para realizar esta ação." : original);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido");
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

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Usuário inativo. Entre em contato com o administrador.");
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
                        message,
                        message
                )
        );
    }
}
