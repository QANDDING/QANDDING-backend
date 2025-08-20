package com.qandding.global.storage;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3PresignService {
	@Value("${app.s3.bucket}")
	private String bucket;
	@Value("${app.s3.region}")
	private String region;
	@Value("${app.s3.url-expire-seconds}")
	private long expireSeconds;
    @Value("${app.s3.upload-prefix}")
    private String uploadPrefix;
    @Value("${app.s3.access-key}")
    private String accessKeyId;
    @Value("${app.s3.secret-key}")
    private String secretAccessKey;

    public String presignPutUrl(String objectKey) {
        log.debug("presignPutUrl() 호출됨 - objectKey: {}", objectKey);
		Region r = Region.of(region);
            try (S3Presigner presigner = S3Presigner.builder()
                    .region(r)
                    .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    ))
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
            String presignedUrl = presigned.url().toString();
            log.debug("presignPutUrl() 반환 - presignedUrl: {}", presignedUrl);
			return presignedUrl;
        }
    }

    public String presignGetUrl(String objectKey) {
        log.debug("presignGetUrl() 호출됨 - objectKey: {}", objectKey);
        Region r = Region.of(region);
        try (S3Presigner presigner = S3Presigner.builder()
                .region(r)
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .serviceConfiguration(S3Configuration.builder().build())
                .build()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            PresignedGetObjectRequest presigned = presigner.presignGetObject(b -> b
                    .signatureDuration(Duration.ofSeconds(expireSeconds))
                    .getObjectRequest(getObjectRequest)
            );
            String presignedUrl = presigned.url().toString();
            log.debug("presignGetUrl() 반환 - presignedUrl: {}", presignedUrl);
            return presignedUrl;
        }
    }

    public String presignGetUrlByUrl(String url) {
        log.debug("presignGetUrlByUrl() 호출됨 - url: {}", url);
        String key = extractObjectKey(url);
        log.debug("추출된 objectKey: {}", key);
        return presignGetUrl(key);
    }

    private String extractObjectKey(String url) {
        if (url == null) {
            return "";
        }
        try {
            URL urlObject = new URL(url);
            String path = urlObject.getPath();
            // Remove the leading slash
            if (path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        } catch (MalformedURLException e) {
            // If it's not a valid URL, assume it's already a key.
            log.warn("Invalid URL format, assuming it's an object key: {}", url, e);
            return url;
        }
    }
}
