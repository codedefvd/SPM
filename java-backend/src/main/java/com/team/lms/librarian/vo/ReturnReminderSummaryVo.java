package com.team.lms.librarian.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReturnReminderSummaryVo {
    private Integer reminderWindowDays;
    private Integer totalReminderCount;
    private Integer dueSoonCount;
    private Integer dueTodayCount;
    private Integer overdueCount;
}
