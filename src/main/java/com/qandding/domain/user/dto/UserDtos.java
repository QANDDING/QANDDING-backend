package com.qandding.domain.user.dto;

import com.qandding.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDtos {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private Long id;
        private String email;
        private String nickname;
        private String grade;
        private String major;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(User user) {
            return new Response(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getGrade(),
                user.getMajor(),
                user.getCreatedAt(),
                user.getUpdatedAt()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateProfileRequest {
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다")
        private String nickname;
        
        @Size(max = 20, message = "학년은 20자 이하여야 합니다")
        private String grade;
        
        @Size(max = 50, message = "전공은 50자 이하여야 합니다")
        private String major;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompleteProfileRequest {
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다")
        private String nickname;
        
        @NotBlank(message = "학년은 필수입니다")
        @Size(max = 20, message = "학년은 20자 이하여야 합니다")
        private String grade;
        
        @NotBlank(message = "전공은 필수입니다")
        @Size(max = 50, message = "전공은 50자 이하여야 합니다")
        private String major;
    }
}
