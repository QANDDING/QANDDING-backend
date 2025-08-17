package com.qandding.storage;

import com.qandding.global.storage.S3PresignService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestS3PresignServiceConfig {
    @Bean
    public S3PresignService s3PresignService() {
        return Mockito.mock(S3PresignService.class);
    }
}
