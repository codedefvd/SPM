package com.team.lms.librarian.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LibrarianStatsDetailVo {
    private LibrarianStatsVo basicStats;
    private PeriodSummaryVo periodSummary;
    private List<PopularBookVo> popularBooks;
    private List<CategoryBorrowVo> popularCategories;
    private List<BorrowTrendVo> borrowTrend;

    @Data
    @Builder
    public static class PeriodSummaryVo {
        private String label;
        private Long borrowCount;
        private Long returnCount;
        private Long overdueCount;
        private Long activeReaderCount;
        private Double returnRate;
        private Double overdueRate;
    }

    @Data
    @Builder
    public static class PopularBookVo {
        private Long bookId;
        private String title;
        private String author;
        private Long borrowCount;
        private String categoryName;
    }

    @Data
    @Builder
    public static class CategoryBorrowVo {
        private String categoryName;
        private Long borrowCount;
    }

    @Data
    @Builder
    public static class BorrowTrendVo {
        private String period; // 格式: "2024-01" 或 "第1周"
        private Long borrowCount;
        private Long returnCount;
    }
}
