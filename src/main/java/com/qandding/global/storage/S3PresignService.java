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
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

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
    @Value("${app.s3.access-key}")
    private String accessKeyId;
    @Value("${app.s3.secret-key}")
    private String secretAccessKey;

    public String presignPutUrl(String objectKey) {
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
			return presigned.url().toString();
        }
    }

    public String presignGetUrl(String objectKey) {
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
            return presigned.url().toString();
        }
    }

    public String presignGetUrlByUrl(String url) {
        String key = extractObjectKey(url);
        return presignGetUrl(key);
    }

    private String extractObjectKey(String url) {
        String prefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        if (url == null) return "";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        // If a full URL in another style or already a key, return as-is best effort
        int idx = url.indexOf("amazonaws.com/");
        if (idx > 0) {
            return url.substring(idx + "amazonaws.com/".length());
        }
        return url;
    }
}
