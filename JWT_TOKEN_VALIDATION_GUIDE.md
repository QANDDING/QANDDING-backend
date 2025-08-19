# JWT 토큰 검증 시스템 가이드

## 개요

이 문서는 Qandding 백엔드의 JWT 토큰 검증 시스템에 대한 상세한 설명을 제공합니다.

## 시스템 구조

### 1. 핵심 컴포넌트

#### JwtTokenProvider

- JWT 토큰 생성, 검증, 파싱 담당
- Access Token과 Refresh Token 구분
- 토큰 만료 시간 관리

#### JwtAuthenticationFilter

- HTTP 요청에서 JWT 토큰 추출
- Authorization 헤더와 쿠키 모두 지원
- Spring Security 컨텍스트에 인증 정보 설정

#### TokenService

- 토큰 쌍 생성 및 관리
- DB 기반 토큰 유효성 검증
- 토큰 무효화 및 재발급

#### JwtTokenValidator

- 중앙화된 토큰 검증 로직
- 사용자 권한 검증
- 에러 처리 표준화

### 2. 보안 계층

```
HTTP Request
    ↓
JwtAuthenticationFilter (토큰 추출)
    ↓
Spring Security (인증 처리)
    ↓
JwtTokenValidationInterceptor (추가 검증)
    ↓
Controller (비즈니스 로직)
```

## 토큰 처리 방식

### 1. 토큰 추출 우선순위

1. **Authorization 헤더** (우선순위 높음)

   ```
   Authorization: Bearer <access_token>
   ```

2. **쿠키** (하위 호환성)
   ```
   Cookie: access_token=<access_token>
   ```

### 2. 토큰 타입 구분

- **Access Token**: 15분 만료, API 요청용
- **Refresh Token**: 7일 만료, 토큰 갱신용

### 3. 토큰 검증 단계

1. **형식 검증**: JWT 구조 및 서명 확인
2. **타입 검증**: Access Token 여부 확인
3. **DB 검증**: 토큰 존재 및 만료 여부 확인
4. **사용자 검증**: 사용자 정보 일치성 확인

## API 보안 설정

### 1. 보호된 경로

```java
// JwtTokenValidationInterceptor에서 보호
"/api/ai/**"           // AI 관련 API
"/api/users/**"         // 사용자 관련 API
"/api/comments/**"      // 댓글 관련 API
"/api/ai-answers/**"    // AI 답변 관련 API
"/api/questions/**"     // 질문 관련 API
"/api/user-answers/**"  // 사용자 답변 관련 API
"/api/answers/**"       // 답변 관련 API
"/api/storage/**"       // 스토리지 관련 API
```

### 2. 공개 경로

```java
// 보호되지 않는 경로
"/api/auth/**"          // 인증 관련 API
"/api/health"           // 헬스 체크
"/"                     // 루트 경로
"/error"                // 에러 페이지
"/swagger-ui/**"        // Swagger UI
"/v3/api-docs/**"       // API 문서
"/favicon.ico"          // 파비콘
```

## 사용 방법

### 1. 컨트롤러에서 토큰 검증

```java
@PostMapping("/create")
public ResponseEntity<Long> create(
        @RequestBody CreateRequest request,
        @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {

    // JWT 토큰 검증 (Spring Security가 자동으로 처리)
    if (customPrincipal == null) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    // 비즈니스 로직 수행
    Long result = service.create(request, customPrincipal.getUserId());
    return ResponseEntity.ok(result);
}
```

### 2. 커스텀 어노테이션 사용

```java
@JwtTokenRequired(message = "인증이 필요합니다.")
@PostMapping("/create")
public ResponseEntity<Long> create(@RequestBody CreateRequest request) {
    // 자동으로 토큰 검증 수행
    // 비즈니스 로직 수행
}
```

### 3. 수동 토큰 검증

```java
@Autowired
private JwtTokenValidator jwtTokenValidator;

public void someMethod(String token) {
    Long userId = jwtTokenValidator.validateToken(token);
    // 검증된 사용자 ID로 작업 수행
}
```

## 에러 처리

### 1. 인증 관련 에러

- **UNAUTHORIZED**: 토큰이 없거나 유효하지 않음
- **FORBIDDEN_ACTION**: 권한이 없음

### 2. 에러 응답 형식

```json
{
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2025-01-20T10:00:00.000+00:00",
  "errors": []
}
```

## 보안 고려사항

### 1. 토큰 보안

- Access Token은 15분으로 짧게 설정
- Refresh Token은 httpOnly 쿠키로 보호
- 토큰 무효화 시 즉시 DB에서 삭제

### 2. CORS 설정

- 허용된 도메인만 접근 가능
- Credentials 포함 요청 지원
- 보안 헤더 설정

### 3. Rate Limiting

- API 요청 빈도 제한
- 인증 실패 시 추가 제한

## 모니터링 및 로깅

### 1. 로그 레벨

- **DEBUG**: 토큰 검증 성공
- **INFO**: 주요 작업 완료
- **WARN**: 토큰 관련 경고
- **ERROR**: 인증 실패 및 오류

### 2. 모니터링 지표

- 토큰 검증 성공/실패율
- 토큰 만료 빈도
- 인증 실패 패턴

## 문제 해결

### 1. 일반적인 문제

#### 토큰 만료

```
에러: "토큰이 만료되었거나 유효하지 않습니다."
해결: Refresh Token으로 새로운 Access Token 발급
```

#### 권한 부족

```
에러: "해당 사용자 정보에 접근할 권한이 없습니다."
해결: 올바른 사용자로 로그인 또는 권한 확인
```

#### CORS 오류

```
에러: "CORS 정책 위반"
해결: 프론트엔드 도메인을 CORS 설정에 추가
```

### 2. 디버깅 방법

1. **로그 확인**: JWT 관련 로그 메시지 확인
2. **토큰 검증**: `/api/auth/validate` 엔드포인트로 토큰 테스트
3. **헤더 확인**: Authorization 헤더가 올바르게 전송되는지 확인

## 성능 최적화

### 1. 토큰 검증 최적화

- DB 조회 최소화
- 캐싱 전략 적용
- 비동기 처리 고려

### 2. 메모리 사용량

- 토큰 크기 최적화
- 불필요한 클레임 제거
- 정기적인 토큰 정리

## 향후 개선 계획

### 1. 단기 계획

- [ ] 토큰 블랙리스트 구현
- [ ] 다중 디바이스 지원
- [ ] 토큰 사용 통계 추가

### 2. 장기 계획

- [ ] OAuth 2.0 표준 준수 강화
- [ ] 마이크로서비스 아키텍처 지원
- [ ] Zero Trust 보안 모델 적용

## 참고 자료

- [JWT 공식 문서](https://jwt.io/)
- [Spring Security JWT 가이드](https://spring.io/guides/tutorials/spring-security-and-angular-js/)
- [OAuth 2.0 표준](https://tools.ietf.org/html/rfc6749)
