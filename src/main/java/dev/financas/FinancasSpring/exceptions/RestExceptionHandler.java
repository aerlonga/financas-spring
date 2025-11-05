package dev.financas.FinancasSpring.exceptions;

import dev.financas.FinancasSpring.rest.dto.ApiErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class RestExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiErrorDTO> handleResourceNotFoundException(ResourceNotFoundException ex,
                        HttpServletRequest request) {
                ApiErrorDTO error = ApiErrorDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .build();
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiErrorDTO> handleBusinessException(BusinessException ex, HttpServletRequest request) {
                ApiErrorDTO error = ApiErrorDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .build();
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorDTO> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                                .collect(Collectors.toList());

                List<String> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                                .map(globalError -> globalError.getObjectName() + ": "
                                                + globalError.getDefaultMessage())
                                .collect(Collectors.toList());

                List<String> allErrors = new ArrayList<>(globalErrors);
                allErrors.addAll(fieldErrors);

                ApiErrorDTO error = ApiErrorDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Validation Error")
                                .message("Um ou mais campos estão inválidos.")
                                .validationErrors(allErrors)
                                .build();
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiErrorDTO> handleDataIntegrityViolationException(DataIntegrityViolationException ex,
                        HttpServletRequest request) {
                String message = "Erro de integridade de dados. Pode ser um valor duplicado (como e-mail ou CPF) ou uma referência a uma entidade inexistente.";
                if (ex.getMostSpecificCause() != null) {
                        message = ex.getMostSpecificCause().getMessage();
                }
                ApiErrorDTO error = ApiErrorDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error("Data Integrity Violation")
                                .message(message)
                                .build();
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorDTO> handleGenericException(Exception ex, HttpServletRequest request) {
                logger.error("Erro inesperado ao processar requisição: {} {}", request.getMethod(),
                                request.getRequestURI(), ex);
                String message = (ex instanceof IllegalStateException)
                                ? ex.getMessage()
                                : "Ocorreu um erro inesperado no servidor.";
                ApiErrorDTO error = ApiErrorDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error("Internal Server Error")
                                .message(message)
                                .build();
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler({ BadCredentialsException.class, UsernameNotFoundException.class })
        public ResponseEntity<ApiErrorDTO> handleAuthExceptions(RuntimeException ex, HttpServletRequest request) {
                ApiErrorDTO error = ApiErrorDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message("Acesso incorreto")
                                .build();
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }
}