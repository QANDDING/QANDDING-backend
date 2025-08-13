package com.qandding.common.error;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {
	private final String code;
	private final String message;
	private final OffsetDateTime timestamp;
	private final List<ValidationErrorDetail> errors;

	public static ApiErrorResponse of(ErrorCode errorCode, String message) {
		return new ApiErrorResponse(errorCode.name(), message, OffsetDateTime.now(), List.of());
	}

	public static ApiErrorResponse of(ErrorCode errorCode, String message, List<ValidationErrorDetail> errors) {
		return new ApiErrorResponse(errorCode.name(), message, OffsetDateTime.now(), errors);
	}
}
