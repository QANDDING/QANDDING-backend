package com.qandding.global.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaAdjuster {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureLongTextForQuestionContent() {
        try {
            String dataType = jdbcTemplate.query(
                    "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = SCHEMA() AND TABLE_NAME = 'question_post' AND COLUMN_NAME = 'content'",
                    rs -> rs.next() ? rs.getString(1) : null
            );
            if (dataType == null) {
                log.warn("question_post.content column not found; skipping type check");
                return;
            }
            if (!"longtext".equalsIgnoreCase(dataType)) {
                log.info("Altering question_post.content column type from '{}' to LONGTEXT", dataType);
                jdbcTemplate.execute("ALTER TABLE question_post MODIFY content LONGTEXT NOT NULL");
                log.info("Altered question_post.content to LONGTEXT successfully");
            } else {
                log.debug("question_post.content is already LONGTEXT");
            }
        } catch (Exception e) {
            log.warn("Failed to verify/alter question_post.content to LONGTEXT. Proceeding without change.", e);
        }
    }

    @PostConstruct
    public void ensureUniqueNicknameIndex() {
        try {
            Integer exists = jdbcTemplate.query(
                    "SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = SCHEMA() AND TABLE_NAME = 'users' AND INDEX_NAME = 'uk_users_nickname'",
                    rs -> rs.next() ? rs.getInt(1) : 0
            );
            if (exists == null || exists == 0) {
                log.info("Adding unique index uk_users_nickname on users(nickname)");
                jdbcTemplate.execute("ALTER TABLE users ADD UNIQUE INDEX uk_users_nickname (nickname)");
            }
        } catch (Exception e) {
            log.warn("Failed to ensure unique index on users.nickname. Proceeding without change.", e);
        }
    }
}
