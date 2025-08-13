package com.qandding.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidationErrorDetail {
	private final String field;
	private final Object value;
	private final String reason;
}
