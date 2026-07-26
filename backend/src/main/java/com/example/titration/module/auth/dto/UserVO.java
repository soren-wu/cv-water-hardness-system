package com.example.titration.module.auth.dto;

import com.example.titration.module.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private String status;
    private Long classId;
    private String email;
    private String phone;
    private String avatarUrl;
    private LocalDateTime lastLoginAt;

    public static UserVO fromEntity(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .status(user.getStatus())
                .classId(user.getClassId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
