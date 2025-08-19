# 프론트엔드 통합 가이드 (하이브리드 방식)

## 🔐 **토큰 구조 (하이브리드 방식)**

### **Access Token**

- **저장 위치**: httpOnly 쿠키 (자동 전송)
- **만료 시간**: 15분
- **용도**: API 요청 시 인증
- **보안**: XSS 공격으로부터 안전

### **Refresh Token**

- **저장 위치**: localStorage (토큰 갱신용)
- **만료 시간**: 7일
- **용도**: Access Token 갱신
- **보안**: Redis에 저장되어 서버에서 관리

## 구글 소셜 로그인 플로우

### 1. 로그인 버튼 구현

```javascript
// 구글 로그인 버튼 클릭 시
const handleGoogleLogin = () => {
  // 백엔드 OAuth2 엔드포인트로 리다이렉트
  window.location.href = 'https://qandding.store/login/oauth2/code/google';
};
```

### 2. JWT 토큰 자동 수신

백엔드에서 구글 로그인 성공 시:

- **Access Token**: httpOnly 쿠키에 자동 저장
- **Refresh Token**: URL 파라미터로 전달

URL 예시: `https://your-frontend.com?success=true&needsProfile=false&refreshToken=eyJhbGciOiJIUzUxMiJ9...`

### 3. 토큰 저장 및 관리

```javascript
// 리다이렉트된 페이지에서 Refresh Token만 추출
const urlParams = new URLSearchParams(window.location.search);
const refreshToken = urlParams.get('refreshToken');
const needsProfile = urlParams.get('needsProfile') === 'true';

if (refreshToken) {
  // Refresh Token만 localStorage에 저장
  localStorage.setItem('refresh_token', refreshToken);

  // Access Token은 httpOnly 쿠키에 자동 저장됨

  // 프로필 완성이 필요한 경우
  if (needsProfile) {
    // 프로필 완성 페이지로 이동
    navigate('/complete-profile');
  } else {
    // 메인 페이지로 이동
    navigate('/');
  }
}
```

### 4. API 요청 시 자동 토큰 첨부 및 갱신

```javascript
// axios 설정
import axios from 'axios';

// 쿠키 자동 포함 설정 (Access Token 자동 전송)
axios.defaults.withCredentials = true;

// 요청 인터셉터 (별도 설정 불필요)
axios.interceptors.request.use(
  (config) => {
    // Access Token은 쿠키에서 자동으로 포함되므로 별도 설정 불필요
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터 (토큰 만료 시 자동 갱신)
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // localStorage에서 Refresh Token 가져오기
        const refreshToken = localStorage.getItem('refresh_token');

        if (refreshToken) {
          // 서버로 Refresh Token 명시적 전달
          const response = await axios.post('/api/auth/refresh', {
            refreshToken: refreshToken,
          });

          if (response.data.message === 'ACCESS_TOKEN_REFRESHED') {
            // 새로운 Access Token이 쿠키에 자동 설정됨
            // 원래 요청 재시도 (쿠키에서 자동으로 토큰 추출)
            return axios(originalRequest);
          }
        }
      } catch (refreshError) {
        // Refresh Token도 만료된 경우
        localStorage.removeItem('refresh_token');
        // 로그인 페이지로 리다이렉트
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
```

### 5. 로그아웃 처리

```javascript
const handleLogout = async () => {
  try {
    // 백엔드 로그아웃 API 호출 (현재 사용자의 모든 토큰 무효화)
    await axios.post('/api/auth/logout');
    // Access Token은 서버에서 쿠키 만료 처리
  } catch (error) {
    console.error('로그아웃 중 오류:', error);
  } finally {
    // Refresh Token만 localStorage에서 제거
    localStorage.removeItem('refresh_token');
    // 로그인 페이지로 이동
    navigate('/login');
  }
};

// 특정 토큰만 로그아웃 (다중 기기 지원)
const logoutSpecificToken = async (token) => {
  try {
    await axios.post('/api/auth/logout-token', { token });
    // 해당 토큰만 블랙리스트에 추가
  } catch (error) {
    console.error('토큰 로그아웃 중 오류:', error);
  }
};
```

## 주요 API 엔드포인트

### 인증 관련

- `POST /api/auth/refresh` - Access Token 재발급
- `POST /api/auth/logout` - 현재 사용자 모든 토큰 무효화
- `POST /api/auth/logout-token` - 특정 토큰 블랙리스트 추가
- `POST /api/auth/check` - 인증 상태 확인

### 사용자 관련

- `GET /api/users/me` - 내 정보 조회
- `PATCH /api/users/me` - 프로필 업데이트
- `PUT /api/users/complete-profile` - 프로필 완성
- `DELETE /api/users/me` - 회원 탈퇴

### 질문 관련

- `POST /api/questions` - 질문 생성
- `GET /api/questions` - 질문 목록 조회
- `GET /api/questions/{id}` - 질문 상세 조회
- `DELETE /api/questions/{id}` - 질문 삭제

### 답변 관련

- `POST /api/user-answers` - 답변 생성
- `GET /api/user-answers` - 답변 목록 조회
- `DELETE /api/user-answers/{id}` - 답변 삭제

## 보안 기능

### 1. **토큰 블랙리스트**

- 로그아웃된 토큰은 즉시 무효화
- Redis에 저장되어 만료 시간까지 관리

### 2. **다중 기기 지원**

- 사용자별로 여러 토큰 동시 사용 가능
- 특정 기기만 로그아웃 가능

### 3. **자동 토큰 갱신**

- Access Token 만료 시 자동으로 Refresh Token 사용
- 사용자 경험 향상

### 4. **XSS 공격 방지**

- Access Token은 httpOnly 쿠키로 보호
- JavaScript로 접근 불가

### 5. **CSRF 공격 방지**

- SameSite=Strict 설정
- 다른 도메인에서의 요청 시 쿠키 전송 안됨

## 보안 주의사항

1. **토큰 보안**

   - Access Token: httpOnly 쿠키로 XSS 공격 방지
   - Refresh Token: localStorage 사용 (7일마다 갱신)
   - Redis에 안전하게 저장

2. **HTTPS 사용**

   - 프로덕션 환경에서는 반드시 HTTPS 사용
   - Secure 쿠키 설정으로 HTTP 전송 방지

3. **토큰 만료 처리**

   - 401 응답 시 자동으로 Refresh Token 사용
   - Refresh Token도 만료된 경우 자동 로그아웃

## 개발 환경 설정

### 환경 변수

```bash
# 프론트엔드 .env 파일
REACT_APP_API_BASE_URL=https://qandding.store
REACT_APP_OAUTH_REDIRECT_URL=https://your-frontend.com

# 백엔드 .env 파일 (Redis 설정)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT 설정
APP_JWT_ACCESS_EXPIRATION=900000    # 15분
APP_JWT_REFRESH_EXPIRATION=604800000 # 7일
```

### CORS 설정

백엔드에서 이미 CORS가 설정되어 있어 별도 설정 불필요합니다.

## 테스트

### 로그인 테스트

1. 구글 로그인 버튼 클릭
2. 구글 계정으로 로그인
3. 프론트엔드로 리다이렉트 확인
4. Refresh Token이 URL 파라미터로 전달되는지 확인
5. Refresh Token이 localStorage에 저장되는지 확인
6. Access Token이 쿠키에 저장되는지 확인 (개발자 도구 > Application > Cookies)

### 토큰 갱신 테스트

1. Access Token 만료 시점까지 대기 (15분)
2. API 호출 시 자동으로 Refresh Token 사용하여 갱신되는지 확인
3. 새로운 Access Token이 쿠키에 설정되는지 확인

### 로그아웃 테스트

1. 로그아웃 후 해당 토큰으로 API 호출 시 401 응답 확인
2. 블랙리스트에 등록된 토큰인지 확인
3. 쿠키가 만료되었는지 확인

## 문제 해결

### 자주 발생하는 문제들

1. **CORS 오류**

   - 백엔드 CORS 설정 확인
   - 프론트엔드 도메인이 허용 목록에 포함되어 있는지 확인

2. **토큰이 전달되지 않는 문제**

   - 백엔드 OAuth2 설정 확인
   - 리다이렉트 URL 설정 확인

3. **401 Unauthorized 오류**

   - Access Token이 쿠키에 올바르게 저장되었는지 확인
   - Refresh Token으로 자동 갱신이 작동하는지 확인

4. **토큰 갱신 실패**

   - Refresh Token이 유효한지 확인
   - Redis 연결 상태 확인

5. **쿠키 관련 문제**

   - `withCredentials: true` 설정 확인
   - HTTPS 환경에서만 Secure 쿠키 작동
   - SameSite 설정 확인

### 디버깅 팁

- 브라우저 개발자 도구의 Network 탭에서 요청/응답 확인
- 개발자 도구의 Application > Cookies에서 쿠키 상태 확인
- localStorage에서 Refresh Token 저장 상태 확인
- 백엔드 로그에서 인증 처리 과정 확인
- Redis에서 토큰 저장 상태 확인

## 구현 시 주의사항

### 1. **쿠키 설정**

- `httpOnly: true` - JavaScript 접근 방지
- `secure: true` - HTTPS에서만 전송
- `sameSite: "Strict"` - CSRF 공격 방지
- `path: "/"` - 모든 경로에서 사용

### 2. **axios 설정**

- `withCredentials: true` 필수
- CORS 설정에서 `allowCredentials: true` 필요

### 3. **토큰 갱신 로직**

- Refresh Token은 명시적으로 서버로 전달
- 새로운 Access Token은 쿠키에 자동 설정
- 원래 요청 재시도 시 쿠키에서 자동 토큰 추출
