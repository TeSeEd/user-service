package com.aston.userservice.mapper;

import com.aston.userservice.dto.UserResponseDto;
import com.aston.userservice.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDto toResponseDto(User user) {
        return
                new UserResponseDto(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getAge(),
                        user.getCreatedAt()

                );
    }
}
