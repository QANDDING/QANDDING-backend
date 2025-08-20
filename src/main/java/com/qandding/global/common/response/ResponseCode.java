package com.qandding.global.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    // Common Success
    SUCCESS(200, "요청에 성공했습니다."),
    CREATED(201, "성공적으로 생성되었습니다."),
    NO_CONTENT(204, "성공적으로 처리되었으나, 응답할 콘텐츠가 없습니다.");

    private final int code;
    private final String message;
}