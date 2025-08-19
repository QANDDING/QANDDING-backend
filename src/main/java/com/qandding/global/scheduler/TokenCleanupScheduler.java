package com.qandding.global.scheduler;

import com.qandding.domain.user.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final UserTokenRepository userTokenRepository;

    // 매일 새벽 2시에 만료된 토큰 정리
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = userTokenRepository.deleteExpiredTokens(now);
            
            if (deletedCount > 0) {
                log.info("만료된 토큰 {}개 정리 완료", deletedCount);
            }
        } catch (Exception e) {
            log.error("만료된 토큰 정리 중 오류 발생: {}", e.getMessage());
        }
    }
}
