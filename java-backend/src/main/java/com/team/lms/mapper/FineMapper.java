package com.team.lms.mapper;

import com.team.lms.entity.Fine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FineMapper {
    Fine selectById(Long id);
    List<Fine> selectAll();
    List<Fine> selectUnpaid();
    Fine selectByBorrowRecordId(Long borrowRecordId);
    int insert(Fine fine);
    int update(Fine fine);
    BigDecimal sumAmountByStatusAndDateRange(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
