package com.qandding.global.storage;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "파일 저장소 관련 API")
public class StorageController {

    private final S3PresignService s3PresignService;

    @GetMapping("/presign")
    @Operation(summary = "S3 업로드용 Presigned URL 생성", description = "파일 업로드를 위한 S3 Presigned URL을 생성합니다.")
    public ResponseEntity<String> presign(@Parameter(description = "S3 객체 키") @RequestParam("key") String objectKey,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        // JWT 토큰 검증 (Spring Security가 자동으로 처리)
        if (customPrincipal == null) {
            log.error("인증되지 않은 사용자의 S3 Presigned URL 생성 요청");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        log.info("S3 업로드용 Presigned URL 생성 요청 - userId: {}, objectKey: {}", customPrincipal.getUserId(), objectKey);
        
        try {
            String presignedUrl = s3PresignService.presignPutUrl(objectKey);
            log.info("S3 업로드용 Presigned URL 생성 완료 - userId: {}, objectKey: {}", customPrincipal.getUserId(), objectKey);
            return ResponseEntity.ok(presignedUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("S3 Presigned URL 생성 중 오류 발생 - userId: {}, objectKey: {}", customPrincipal.getUserId(), objectKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "S3 Presigned URL 생성 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/presign-get")
    @Operation(summary = "S3 다운로드용 Presigned URL 생성", description = "파일 다운로드를 위한 S3 Presigned URL을 생성합니다.")
    public ResponseEntity<String> presignGet(@Parameter(description = "S3 URL") @RequestParam("url") String url,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        // JWT 토큰 검증 (Spring Security가 자동으로 처리)
        if (customPrincipal == null) {
            log.error("인증되지 않은 사용자의 S3 다운로드용 Presigned URL 생성 요청");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        log.info("S3 다운로드용 Presigned URL 생성 요청 - userId: {}, url: {}", customPrincipal.getUserId(), url);
        
        try {
            String presignedUrl = s3PresignService.presignGetUrlByUrl(url);
            log.info("S3 다운로드용 Presigned URL 생성 완료 - userId: {}, url: {}", customPrincipal.getUserId(), url);
            return ResponseEntity.ok(presignedUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("S3 다운로드용 Presigned URL 생성 중 오류 발생 - userId: {}, url: {}", customPrincipal.getUserId(), url, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "S3 다운로드용 Presigned URL 생성 중 오류가 발생했습니다.");
        }
    }
}