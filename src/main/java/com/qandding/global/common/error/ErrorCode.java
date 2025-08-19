package com.qandding.global.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	INVALID_SCHOOL_EMAIL(HttpStatus.BAD_REQUEST, "학교 이메일(@mju.ac.kr)만 사용할 수 있습니다."),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
	ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다."),
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
	SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "과목을 찾을 수 없습니다."),
	PROFESSOR_NOT_FOUND(HttpStatus.NOT_FOUND, "교수님을 찾을 수 없습니다."),
	FORBIDDEN_ACTION(HttpStatus.FORBIDDEN, "권한이 없습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청 한도를 초과했습니다."),
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "내용이 비어있습니다." ),
    ANSWER_NOT_SELECTABLE(HttpStatus.BAD_REQUEST, "채택할 수 없는 답변입니다.");

	private final HttpStatus httpStatus;
	private final String defaultMessage;

	ErrorCode(HttpStatus httpStatus, String defaultMessage) {
		this.httpStatus = httpStatus;
		this.defaultMessage = defaultMessage;
	}

	public HttpStatus getHttpStatus() { return httpStatus; }
	public String getDefaultMessage() { return defaultMessage; }
}
