package com.team.lms.common.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        dropUniqueReviewConstraint();
        migrateNewFeatures();
        cleanupTestReviews();
    }

    private void migrateNewFeatures() {
        try {
            jdbcTemplate.execute("alter table books add column storage_location varchar(255)");
            log.info("Added storage_location to books");
        } catch (Exception e) {
            log.info("Column books.storage_location already exists: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("alter table book_copies add column storage_location varchar(255)");
            log.info("Added storage_location to book_copies");
        } catch (Exception e) {
            log.info("Column book_copies.storage_location already exists: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("alter table borrow_records add column renewal_count int not null default 0");
            log.info("Added renewal_count to borrow_records");
        } catch (Exception e) {
            log.info("Column borrow_records.renewal_count already exists: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute(
                "create table if not exists review_likes (" +
                "id bigint primary key auto_increment, review_id bigint not null, reader_id bigint not null, " +
                "created_at datetime not null, constraint uq_review_like unique (review_id, reader_id))"
            );
            log.info("Ensured review_likes table exists");
        } catch (Exception e) {
            log.info("review_likes migration skipped: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute(
                "create table if not exists review_replies (" +
                "id bigint primary key auto_increment, review_id bigint not null, reader_id bigint not null, " +
                "reply_content varchar(1000) not null, created_at datetime not null, updated_at datetime not null, " +
                "deleted boolean not null default false)"
            );
            log.info("Ensured review_replies table exists");
        } catch (Exception e) {
            log.info("review_replies migration skipped: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute(
                "create table if not exists feedback_conversations (" +
                "id bigint primary key auto_increment, reader_id bigint not null, subject varchar(255) not null, " +
                "status varchar(32) not null, created_at datetime not null, updated_at datetime not null, " +
                "deleted boolean not null default false)"
            );
            log.info("Ensured feedback_conversations table exists");
        } catch (Exception e) {
            log.info("feedback_conversations migration skipped: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute(
                "create table if not exists feedback_messages (" +
                "id bigint primary key auto_increment, conversation_id bigint not null, sender_id bigint not null, " +
                "content varchar(2000) not null, read_at datetime, created_at datetime not null, " +
                "updated_at datetime not null, deleted boolean not null default false)"
            );
            log.info("Ensured feedback_messages table exists");
        } catch (Exception e) {
            log.info("feedback_messages migration skipped: {}", e.getMessage());
        }
        try {
            jdbcTemplate.update(
                "insert into system_configs(config_key, config_value, description, created_at, updated_at, deleted) " +
                "select 'MAX_RENEWALS', '2', 'Maximum renewals per borrow record', now(), now(), false " +
                "where not exists (select 1 from system_configs where config_key = 'MAX_RENEWALS')"
            );
        } catch (Exception e) {
            log.info("MAX_RENEWALS config migration skipped: {}", e.getMessage());
        }
        appendPermission("READER", "FEEDBACK");
        appendPermission("LIBRARIAN", "FEEDBACK_MANAGE");
    }

    private void appendPermission(String role, String permission) {
        try {
            String current = jdbcTemplate.queryForObject(
                "select permission_scope from role_permissions where role = ? and deleted = false limit 1",
                String.class,
                role
            );
            if (current != null && !current.contains(permission)) {
                jdbcTemplate.update(
                    "update role_permissions set permission_scope = ?, updated_at = now() where role = ? and deleted = false",
                    current + "," + permission,
                    role
                );
                log.info("Appended {} permission to role {}", permission, role);
            }
        } catch (Exception e) {
            log.info("Permission append for {} skipped: {}", role, e.getMessage());
        }
    }

    private void cleanupTestReviews() {
        try {
            int deleted = jdbcTemplate.update(
                "delete from book_reviews where review_content in ('good','poor','1') " +
                "or review_content like '%second review after fix%' " +
                "or review_content like '%multiple reviews%' " +
                "or char_length(review_content) < 5"
            );
            if (deleted > 0) {
                log.info("Cleaned up {} test reviews", deleted);
            }
        } catch (Exception e) {
            log.info("Review cleanup skipped: {}", e.getMessage());
        }
    }

    private void dropUniqueReviewConstraint() {
        try {
            jdbcTemplate.execute("create index idx_book_review_reader on book_reviews(reader_id)");
            log.info("Created index idx_book_review_reader");
        } catch (Exception e) {
            log.info("Index idx_book_review_reader already exists: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("create index idx_book_review_book on book_reviews(book_id)");
            log.info("Created index idx_book_review_book");
        } catch (Exception e) {
            log.info("Index idx_book_review_book already exists: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("alter table book_reviews drop index uq_book_review");
            log.info("Dropped unique constraint uq_book_review from book_reviews");
        } catch (Exception e) {
            log.info("Constraint uq_book_review already removed or not present: {}", e.getMessage());
        }
    }
}
