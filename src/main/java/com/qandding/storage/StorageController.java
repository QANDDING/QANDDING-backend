package com.qandding.storage;

import com.qandding.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
	private final S3PresignService s3PresignService;

	public StorageController(S3PresignService s3PresignService) {
		this.s3PresignService = s3PresignService;
	}

	@GetMapping("/presign")
	public ResponseEntity<String> presign(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                     @RequestParam("key") String objectKey) {
		return ResponseEntity.ok(s3PresignService.presignPutUrl(objectKey));
	}
}
