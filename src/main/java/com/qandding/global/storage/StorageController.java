package com.qandding.global.storage;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
	public ResponseEntity<String> presign(@RequestParam("key") String objectKey) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (principal instanceof CustomUserPrincipal) {
			return ResponseEntity.ok(s3PresignService.presignPutUrl(objectKey));
		} else {
			return ResponseEntity.status(401).build();
		}
	}
}
