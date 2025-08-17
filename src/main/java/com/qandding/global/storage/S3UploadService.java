package com.qandding.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {
    
    @Value("${app.s3.bucket}")
    private String bucket;
    
    @Value("${app.s3.region}")
    private String region;
    
    @Value("${app.s3.upload-prefix}")
    private String uploadPrefix;
    
    @Value("${app.s3.access-key}")
    private String accessKeyId;
    
    @Value("${app.s3.secret-key}")
    private String secretAccessKey;
    
    /**
     * 이미지를 S3에 업로드하고 URL을 반환
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        
        // AWS 자격 증명 확인
        if (accessKeyId == null || accessKeyId.isEmpty() || accessKeyId.equals("your_aws_access_key_here") ||
            secretAccessKey == null || secretAccessKey.isEmpty() || secretAccessKey.equals("your_aws_secret_key_here")) {
            log.error("AWS 자격 증명이 설정되지 않았습니다. application.yml에서 app.s3.access-key와 app.s3.secret-key를 설정해주세요.");
            throw new RuntimeException("AWS 자격 증명이 설정되지 않았습니다. application.yml을 확인해주세요.");
        }
        
        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!isValidImageExtension(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다: " + extension);
        }
        
        // 고유한 파일명 생성
        String fileName = generateUniqueFileName(extension);
        String objectKey = uploadPrefix + fileName;
        
        log.info("S3에 이미지 업로드 시작: bucket={}, key={}, size={}", bucket, objectKey, file.getSize());
        
        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build()) {
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            
            PutObjectResponse response = s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            String imageUrl = generateImageUrl(objectKey);
            log.info("S3 이미지 업로드 완료: key={}, url={}", objectKey, imageUrl);
            
            return imageUrl;
            
        } catch (Exception e) {
            log.error("S3 이미지 업로드 실패: key={}", objectKey, e);
            throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 유효한 이미지 확장자 검증
     */
    private boolean isValidImageExtension(String extension) {
        return extension.matches("(jpg|jpeg|png|gif|webp)");
    }
    
    /**
     * 고유한 파일명 생성
     */
    private String generateUniqueFileName(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }
    
    /**
     * 이미지 URL 생성
     */
    private String generateImageUrl(String objectKey) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + objectKey;
    }
}
