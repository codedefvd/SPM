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
        createFineBorrowRecordConstraint();
        cleanupTestReviews();
    }

    private void createFineBorrowRecordConstraint() {
        try {
            jdbcTemplate.execute("alter table fines add constraint uq_fine_borrow_record unique (borrow_record_id)");
            log.info("Created unique constraint uq_fine_borrow_record on fines");
        } catch (Exception e) {
            log.info("Constraint uq_fine_borrow_record already exists or cannot be created: {}", e.getMessage());
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
