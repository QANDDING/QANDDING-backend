package com.qandding.global.common.error;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@AllArgsConstructor
@Schema(name = "ApiErrorResponse", description = "공통 에러 응답")
public class ApiErrorResponse {
	@Schema(description = "에러 코드", example = "UNAUTHORIZED")
	private final String code;
	@Schema(description = "메시지", example = "인증이 필요합니다.")
	private final String message;
	@Schema(description = "발생 시각(UTC)", example = "2025-08-19T12:34:56Z")
	private final OffsetDateTime timestamp;
	@ArraySchema(arraySchema = @Schema(description = "검증 에러 상세 목록"))
	private final List<ValidationErrorDetail> errors;

	public static ApiErrorResponse of(ErrorCode errorCode, String message) {
		return new ApiErrorResponse(errorCode.name(), message, OffsetDateTime.now(), java.util.List.of());
	}

	public static ApiErrorResponse of(ErrorCode errorCode, String message, List<ValidationErrorDetail> errors) {
		return new ApiErrorResponse(errorCode.name(), message, OffsetDateTime.now(), errors);
	}
}
