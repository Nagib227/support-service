package ru.ithub.support.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private final Clock clock;

  public ApiExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApi(ApiException exception, HttpServletRequest request) {
    log.warn("Ошибка API {}: {}", exception.getStatus().value(), exception.getMessage());
    return respond(exception.getStatus(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ApiError.FieldViolation> violations =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
            .toList();
    return respond(HttpStatus.BAD_REQUEST, "Ошибка валидации", request, violations);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    HttpMessageNotReadableException.class,
    PropertyReferenceException.class
  })
  public ResponseEntity<ApiError> handleBadRequest(
      Exception exception, HttpServletRequest request) {
    log.warn("Некорректный запрос: {}", exception.getMessage());
    return respond(
        HttpStatus.BAD_REQUEST,
        "Некорректный запрос. Проверьте формат полей и допустимые значения.",
        request,
        List.of());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      NoResourceFoundException exception, HttpServletRequest request) {
    return respond(HttpStatus.NOT_FOUND, "Ресурс не найден", request, List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleIntegrity(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    log.warn("Конфликт данных", exception);
    return respond(HttpStatus.CONFLICT, "Конфликт данных", request, List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    log.error("Необработанная ошибка", exception);
    return respond(
        HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", request, List.of());
  }

  private ResponseEntity<ApiError> respond(
      HttpStatus status,
      String message,
      HttpServletRequest request,
      List<ApiError.FieldViolation> violations) {
    ApiError body =
        new ApiError(
            clock.instant(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            violations);
    return ResponseEntity.status(status).body(body);
  }
}
