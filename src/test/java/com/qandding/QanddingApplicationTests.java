package com.qandding;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(com.qandding.storage.TestS3PresignServiceConfig.class)
class QanddingApplicationTests {
	@Test
	void contextLoads() {
	}
}
