package com.team.lms.mapper;

import com.team.lms.entity.Fine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FineMapper {
    Fine selectById(Long id);
    List<Fine> selectAll();
    List<Fine> selectByFilters(@Param("status") String status, @Param("keyword") String keyword);
    List<Fine> selectUnpaid();
    Fine selectByBorrowRecordId(Long borrowRecordId);
    int insert(Fine fine);
    int update(Fine fine);
}
