package com.qandding.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "API 공통 응답")
public class CommonResponse<T> {

    @Schema(description = "응답 메시지", example = "요청에 성공했습니다.")
    private String message;

    @Schema(description = "응답 코드 (HTTP Status Code)", example = "200")
    private int code;

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 데이터")
    private T data;

    // Modified success methods to use ResponseCode
    public static <T> CommonResponse<T> success(ResponseCode responseCode, T data) {
        return new CommonResponse<>(responseCode.getMessage(), responseCode.getCode(), true, data);
    }

    public static <T> CommonResponse<T> success(ResponseCode responseCode) {
        return new CommonResponse<>(responseCode.getMessage(), responseCode.getCode(), true, null);
    }

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(ResponseCode.SUCCESS.getMessage(), ResponseCode.SUCCESS.getCode(), true, data);
    }

    public static <T> CommonResponse<T> success() {
        return new CommonResponse<>(ResponseCode.SUCCESS.getMessage(), ResponseCode.SUCCESS.getCode(), true, null);
    }

    // Original error methods (unchanged)
    public static <T> CommonResponse<T> error(String message, int code, T data) {
        return new CommonResponse<>(message, code, false, data);
    }

    public static <T> CommonResponse<T> error(String message, int code) {
        return new CommonResponse<>(message, code, false, null);
    }
}
