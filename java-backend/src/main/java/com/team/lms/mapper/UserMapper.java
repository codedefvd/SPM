package com.team.lms.mapper;

import com.team.lms.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    User selectById(Long id);
    User selectByUsername(String username);
    User selectByStudentNo(String studentNo);
    List<User> selectAllActive();
    int insert(User user);
    int update(User user);
}
