package com.qandding.global.storage;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3PresignService {
	@Value("${app.s3.bucket}")
	private String bucket;
	@Value("${app.s3.region}")
	private String region;
	@Value("${app.s3.url-expire-seconds}")
	private long expireSeconds;
	@Value("${app.s3.upload-prefix}")
	private String uploadPrefix;

	public String presignPutUrl(String objectKey) {
		Region r = Region.of(region);
		try (S3Presigner presigner = S3Presigner.builder()
				.region(r)
				.credentialsProvider(DefaultCredentialsProvider.create())
				.serviceConfiguration(S3Configuration.builder().build())
				.build()) {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(uploadPrefix + objectKey)
				.build();
			PresignedPutObjectRequest presigned = presigner.presignPutObject(b -> b
				.signatureDuration(Duration.ofSeconds(expireSeconds))
				.putObjectRequest(putObjectRequest)
			);
			return presigned.url().toString();
		}
	}
}
