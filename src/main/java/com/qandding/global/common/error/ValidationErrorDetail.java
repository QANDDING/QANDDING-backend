package com.qandding.global.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@AllArgsConstructor
@Schema(name = "ValidationErrorDetail", description = "검증 에러 상세")
public class ValidationErrorDetail {
	@Schema(description = "필드명", example = "title")
	private final String field;
	@Schema(description = "입력값")
	private final Object value;
	@Schema(description = "사유", example = "must not be blank")
	private final String reason;
}
