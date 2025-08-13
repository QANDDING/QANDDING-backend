package com.qandding.common.error;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException e) {
		ErrorCode code = e.getErrorCode();
		return ResponseEntity.status(code.getHttpStatus()).body(ApiErrorResponse.of(code, e.getMessage()));
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<ApiErrorResponse> handleValidation(Exception e) {
		List<FieldError> fieldErrors = e instanceof MethodArgumentNotValidException manve
			? manve.getBindingResult().getFieldErrors()
			: ((BindException) e).getBindingResult().getFieldErrors();
		List<ValidationErrorDetail> details = fieldErrors.stream()
			.map(fe -> new ValidationErrorDetail(fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()))
			.collect(Collectors.toList());
		ErrorCode code = ErrorCode.VALIDATION_FAILED;
		return ResponseEntity.status(code.getHttpStatus()).body(ApiErrorResponse.of(code, code.getDefaultMessage(), details));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException e) {
		ErrorCode code = ErrorCode.BAD_REQUEST;
		return ResponseEntity.status(code.getHttpStatus()).body(ApiErrorResponse.of(code, e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception e) {
		ErrorCode code = ErrorCode.INTERNAL_ERROR;
		return ResponseEntity.status(code.getHttpStatus()).body(ApiErrorResponse.of(code, code.getDefaultMessage()));
	}
}
