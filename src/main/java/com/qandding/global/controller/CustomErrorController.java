package com.qandding.global.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute("javax.servlet.error.status_code");
        Object message = request.getAttribute("javax.servlet.error.message");
        Object exception = request.getAttribute("javax.servlet.error.exception");
        
        Map<String, Object> errorResponse = new HashMap<>();
        
        if (status != null) {
            int statusCode = Integer.valueOf(status.toString());
            errorResponse.put("status", statusCode);
            errorResponse.put("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
            
            if (statusCode == 500) {
                errorResponse.put("message", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                errorResponse.put("details", "CORS 설정 또는 Bean 생성 과정에서 오류가 발생했습니다.");
            } else if (statusCode == 404) {
                errorResponse.put("message", "요청하신 페이지를 찾을 수 없습니다.");
            } else {
                errorResponse.put("message", message != null ? message.toString() : "알 수 없는 오류가 발생했습니다.");
            }
        } else {
            errorResponse.put("status", 500);
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "서버 오류가 발생했습니다.");
        }
        
        if (exception != null) {
            errorResponse.put("exception", exception.getClass().getSimpleName());
        }
        
        return ResponseEntity.status((Integer) errorResponse.get("status"))
                .body(errorResponse);
    }
}
