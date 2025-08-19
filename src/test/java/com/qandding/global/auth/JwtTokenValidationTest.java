package com.qandding.global.auth;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.auth.JwtTokenValidator;
import com.qandding.global.auth.JwtTokenProvider;
import com.qandding.global.auth.TokenService;
import com.qandding.global.common.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JWT 토큰 검증 테스트 클래스
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenValidationTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private JwtTokenValidator jwtTokenValidator;

    private CustomUserPrincipal validUserPrincipal;
    private CustomUserPrincipal nullUserPrincipal;

    @BeforeEach
    void setUp() {
        validUserPrincipal = new CustomUserPrincipal(
            1L, "test@mju.ac.kr", "테스트사용자", 
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        nullUserPrincipal = null;
    }

    @Test
    void validateUserPrincipal_ValidUser_ShouldNotThrowException() {
        // Given
        // When & Then
        assertDoesNotThrow(() -> jwtTokenValidator.validateUserPrincipal(validUserPrincipal));
    }

    @Test
    void validateUserPrincipal_NullUser_ShouldThrowBusinessException() {
        // Given
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> jwtTokenValidator.validateUserPrincipal(nullUserPrincipal));
        
        assertEquals("UNAUTHORIZED", exception.getErrorCode().name());
    }

    @Test
    void validateToken_ValidToken_ShouldReturnUserId() {
        // Given
        String validToken = "valid.jwt.token";
        Long expectedUserId = 1L;
        
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(validToken)).thenReturn(true);
        when(tokenService.isTokenValid(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(expectedUserId);

        // When
        Long actualUserId = jwtTokenValidator.validateToken(validToken);

        // Then
        assertEquals(expectedUserId, actualUserId);
        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).isAccessToken(validToken);
        verify(tokenService).isTokenValid(validToken);
        verify(jwtTokenProvider).getUserIdFromToken(validToken);
    }

    @Test
    void validateToken_EmptyToken_ShouldThrowBusinessException() {
        // Given
        String emptyToken = "";

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> jwtTokenValidator.validateToken(emptyToken));
        
        assertEquals("UNAUTHORIZED", exception.getErrorCode().name());
    }

    @Test
    void validateToken_InvalidToken_ShouldThrowBusinessException() {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> jwtTokenValidator.validateToken(invalidToken));
        
        assertEquals("UNAUTHORIZED", exception.getErrorCode().name());
        verify(jwtTokenProvider).validateToken(invalidToken);
    }

    @Test
    void validateToken_NotAccessToken_ShouldThrowBusinessException() {
        // Given
        String refreshToken = "refresh.jwt.token";
        
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(refreshToken)).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> jwtTokenValidator.validateToken(refreshToken));
        
        assertEquals("UNAUTHORIZED", exception.getErrorCode().name());
        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).isAccessToken(refreshToken);
    }

    @Test
    void validateToken_InvalidInDB_ShouldThrowBusinessException() {
        // Given
        String invalidToken = "invalid.db.token";
        
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(invalidToken)).thenReturn(true);
        when(tokenService.isTokenValid(invalidToken)).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> jwtTokenValidator.validateToken(invalidToken));
        
        assertEquals("UNAUTHORIZED", exception.getErrorCode().name());
        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(jwtTokenProvider).isAccessToken(invalidToken);
        verify(tokenService).isTokenValid(invalidToken);
    }

    @Test
    void validateUserAccess_SameUserId_ShouldNotThrowException() {
        // Given
        Long requestedUserId = 1L;

        // When & Then
        assertDoesNotThrow(() -> jwtTokenValidator.validateUserAccess(validUserPrincipal, requestedUserId));
    }

    @Test
    void validateUserAccess_DifferentUserId_ShouldThrowBusinessException() {
        // Given
        Long requestedUserId = 2L;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> jwtTokenValidator.validateUserAccess(validUserPrincipal, requestedUserId));
        
        assertEquals("FORBIDDEN_ACTION", exception.getErrorCode().name());
    }
}
