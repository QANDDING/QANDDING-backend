package com.qandding.global.storage;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "파일 저장소 관련 API")
public class StorageController {

    private final S3PresignService s3PresignService;

    @GetMapping("/presign")
    public ResponseEntity<String> presign(@RequestParam("key") String objectKey) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(s3PresignService.presignPutUrl(objectKey));
    }

    @GetMapping("/presign-get")
    public ResponseEntity<String> presignGet(@RequestParam("url") String url) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(s3PresignService.presignGetUrlByUrl(url));
    }
}