package com.team.lms.librarian.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LibrarianStatsDetailVo {
    private LibrarianStatsVo basicStats;
    private List<PopularBookVo> popularBooks;
    private List<BorrowTrendVo> borrowTrend;

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
    public static class BorrowTrendVo {
        private String period; // 格式: "2024-01" 或 "第1周"
        private Long borrowCount;
        private Long returnCount;
    }
}