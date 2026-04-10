package com.team.lms.mapper;

import com.team.lms.entity.BorrowRecord;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BorrowRecordMapper {
    List<BorrowRecord> selectAll();
    BorrowRecord selectById(Long id);
    void insert(BorrowRecord record);
    void update(BorrowRecord record);
    List<BorrowRecord> selectReturnPendingRecords();

    List<PopularBookStat> selectPopularBooks(@Param("limit") int limit);
    List<BorrowTrendStat> selectBorrowTrend(@Param("groupBy") String groupBy);

    class PopularBookStat {
        private Long bookId;
        private String title;
        private String author;
        private Long borrowCount;
        private String categoryName;

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public Long getBorrowCount() { return borrowCount; }
        public void setBorrowCount(Long borrowCount) { this.borrowCount = borrowCount; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    }

    class BorrowTrendStat {
        private String period;
        private Long borrowCount;
        private Long returnCount;

        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public Long getBorrowCount() { return borrowCount; }
        public void setBorrowCount(Long borrowCount) { this.borrowCount = borrowCount; }
        public Long getReturnCount() { return returnCount; }
        public void setReturnCount(Long returnCount) { this.returnCount = returnCount; }
    }
}
