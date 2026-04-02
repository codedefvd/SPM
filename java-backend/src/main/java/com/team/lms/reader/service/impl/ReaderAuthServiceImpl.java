package com.team.lms.reader.service.impl;

import com.team.lms.common.enums.RoleType;
import com.team.lms.entity.User;
import com.team.lms.exception.BusinessException;
import com.team.lms.mapper.UserMapper;
import com.team.lms.reader.dto.ReaderLoginRequest;
import com.team.lms.reader.dto.ReaderRegisterRequest;
import com.team.lms.reader.service.ReaderAuthService;
import com.team.lms.reader.vo.ReaderAuthVo;
import org.springframework.stereotype.Service;

@Service
public class ReaderAuthServiceImpl implements ReaderAuthService {

    private final UserMapper userMapper;

    public ReaderAuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public ReaderAuthVo register(ReaderRegisterRequest request) {
        User existing = userMapper.selectByUsername(request.getUsername());
        if (existing != null) {
            throw new BusinessException(400, "username already exists");
        }

        User existingStudentNo = userMapper.selectByStudentNo(request.getStudentNo());
        if (existingStudentNo != null) {
            throw new BusinessException(400, "studentNo already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setFullName(request.getFullName());
        user.setStudentNo(request.getStudentNo());
        user.setPhone(request.getPhone());
        user.setRole(RoleType.READER);
        user.setEnabled(true);
        userMapper.insert(user);

        return ReaderAuthVo.builder()
                .username(user.getUsername())
                .token("mock-token-for-" + user.getUsername())
                .build();
    }

    @Override
    public ReaderAuthVo login(ReaderLoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(404, "user not found");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(403, "user is disabled");
        }
        if (!request.getPassword().equals(user.getPassword())) {
            throw new BusinessException(400, "username or password is invalid");
        }
        return ReaderAuthVo.builder()
                .username(user.getUsername())
                .token("mock-token-for-" + user.getUsername())
                .build();
    }
}