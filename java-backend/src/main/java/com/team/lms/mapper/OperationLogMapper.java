package com.team.lms.mapper;

import com.team.lms.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper {
    void insert(OperationLog log);
    List<OperationLog> selectByCondition(
            @Param("operatorName") String operatorName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("actionName") String actionName,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );
    Integer countByCondition(
            @Param("operatorName") String operatorName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("actionName") String actionName
    );
    List<OperationLog> selectAll();
}