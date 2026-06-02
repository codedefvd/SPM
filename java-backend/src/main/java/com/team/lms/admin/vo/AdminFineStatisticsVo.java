package com.team.lms.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminFineStatisticsVo {
    /** 未缴纳罚款总金额 */
    private BigDecimal unpaidFineAmount;
    /** 已缴纳罚款总金额（全部历史） */
    private BigDecimal paidFineTotal;
    /** 当日已缴纳罚款金额 */
    private BigDecimal paidFineToday;
    /** 昨日已缴纳罚款金额 */
    private BigDecimal paidFineYesterday;
    /** 上周已缴纳罚款金额 */
    private BigDecimal paidFineLastWeek;
    /** 上月已缴纳罚款金额 */
    private BigDecimal paidFineLastMonth;
}
