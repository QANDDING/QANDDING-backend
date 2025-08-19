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
- **Authorization 헤더에서 Bearer 토큰 추출**
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

### 1. 토큰 추출 방식

**Authorization 헤더** (유일한 방식)

```
Authorization: Bearer <access_token>
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
"/api/answers/**"       // 답변 관련 API
"/api/user-answers/**"  // 사용자 답변 관련 API
"/api/storage/**"       // 파일 저장소 관련 API
```

### 2. 공개 경로

```java
"/"                     // 루트 경로
"/api/health"           // 헬스 체크
"/api/auth/**"          // 인증 관련 API
"/api/subjects/**"      // 과목 검색 API (공개)
"/api/professors/**"    // 교수 검색 API (공개)
```

## 프론트엔드 연동

### 1. 토큰 전송

```javascript
// API 요청 시 Authorization 헤더에 토큰 포함
const headers = {
  Authorization: `Bearer ${accessToken}`,
  'Content-Type': 'application/json',
};

fetch('/api/users/me', { headers })
  .then((response) => response.json())
  .then((data) => console.log(data));
```

### 2. 토큰 갱신

```javascript
// Refresh Token으로 새로운 Access Token 발급
const refreshToken = localStorage.getItem('refreshToken');

fetch('/api/auth/refresh', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ refreshToken }),
})
  .then((response) => response.json())
  .then((data) => {
    localStorage.setItem('accessToken', data.accessToken);
  });
```

### 3. 로그아웃

```javascript
// 서버에 로그아웃 요청
fetch('/api/auth/logout', {
  method: 'POST',
  headers: { Authorization: `Bearer ${accessToken}` },
}).then(() => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
});
```

## 보안 고려사항

### 1. 토큰 저장

- **Access Token**: 메모리 또는 임시 저장소 (15분 만료)
- **Refresh Token**: 안전한 저장소 (localStorage, sessionStorage 등)

### 2. 토큰 전송

- HTTPS 통신 필수
- Authorization 헤더 사용
- 쿠키 사용하지 않음

### 3. 토큰 만료 처리

- 401 응답 시 자동으로 토큰 갱신 시도
- 갱신 실패 시 로그인 페이지로 리다이렉트

## 에러 처리

### 1. 인증 실패 (401)

```json
{
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다.",
  "timestamp": "2025-08-19T10:00:00.000+00:00",
  "errors": []
}
```

### 2. 권한 없음 (403)

```json
{
  "code": "FORBIDDEN_ACTION",
  "message": "권한이 없습니다.",
  "timestamp": "2025-08-19T10:00:00.000+00:00",
  "errors": []
}
```

## 개발 환경 설정

### 1. 환경 변수

```yaml
app:
  jwt:
    secret: ${APP_JWT_SECRET}
    access-expiration: ${APP_JWT_ACCESS_EXPIRATION}
    refresh-expiration: ${APP_JWT_REFRESH_EXPIRATION}
```

### 2. CORS 설정

```yaml
app:
  cors:
    allowed-headers: 'Authorization,Content-Type,X-Requested-With'
    exposed-headers: 'Authorization'
```

## 테스트

### 1. 토큰 검증 테스트

```bash
# 유효한 토큰으로 API 호출
curl -H "Authorization: Bearer <access_token>" \
     http://localhost:8080/api/users/me

# 토큰 없이 API 호출 (401 예상)
curl http://localhost:8080/api/users/me
```

### 2. 토큰 갱신 테스트

```bash
# Refresh Token으로 새로운 Access Token 발급
curl -X POST \
     -H "Content-Type: application/json" \
     -d '{"refreshToken":"<refresh_token>"}' \
     http://localhost:8080/api/auth/refresh
```
