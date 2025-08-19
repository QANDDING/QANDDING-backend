# 프론트엔드 통합 가이드

## 개요

이 문서는 Qandding 백엔드와 프론트엔드 간의 JWT 토큰 기반 인증 통합 방법을 설명합니다.

## JWT 토큰 인증 시스템

### 1. 토큰 구조

- **Access Token**: 15분 만료, API 요청 시 사용
- **Refresh Token**: 7일 만료, Access Token 갱신 시 사용

### 2. 토큰 전송 방식

**Authorization 헤더 사용** (쿠키 사용하지 않음)

```javascript
const headers = {
  Authorization: `Bearer ${accessToken}`,
  'Content-Type': 'application/json',
};
```

## OAuth2 로그인 플로우

### 1. 로그인 시작

```javascript
// Google OAuth2 로그인 시작
window.location.href = 'https://qandding.store/login/oauth2/authorization/google';
```

### 2. 로그인 성공 후 처리

OAuth2 로그인 성공 시 백엔드에서 리다이렉트되는 URL에서 토큰을 추출합니다.

```javascript
// URL 파라미터에서 토큰 추출
const urlParams = new URLSearchParams(window.location.search);
const success = urlParams.get('success');
const needsProfile = urlParams.get('needsProfile') === 'true';
const accessToken = urlParams.get('accessToken');
const refreshToken = urlParams.get('refreshToken');

if (success === 'true' && accessToken && refreshToken) {
  // 토큰을 안전한 저장소에 저장
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);

  // 프로필 완성 필요 여부에 따른 처리
  if (needsProfile) {
    // 프로필 완성 페이지로 이동
    navigate('/complete-profile');
  } else {
    // 메인 페이지로 이동
    navigate('/');
  }
}
```

## API 요청 처리

### 1. 기본 API 요청 함수

```javascript
class ApiService {
  constructor() {
    this.baseURL = 'https://qandding.store/api';
  }

  // Authorization 헤더가 포함된 요청
  async request(endpoint, options = {}) {
    const accessToken = localStorage.getItem('accessToken');

    if (!accessToken) {
      throw new Error('Access token not found');
    }

    const headers = {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
      ...options.headers,
    };

    const response = await fetch(`${this.baseURL}${endpoint}`, {
      ...options,
      headers,
    });

    // 401 응답 시 토큰 갱신 시도
    if (response.status === 401) {
      const refreshed = await this.refreshToken();
      if (refreshed) {
        // 새로운 토큰으로 재시도
        return this.request(endpoint, options);
      } else {
        // 로그인 페이지로 리다이렉트
        this.redirectToLogin();
        return;
      }
    }

    return response;
  }

  // 토큰 갱신
  async refreshToken() {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        return false;
      }

      const response = await fetch(`${this.baseURL}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken }),
      });

      if (response.ok) {
        const data = await response.json();
        localStorage.setItem('accessToken', data.accessToken);
        return true;
      }
    } catch (error) {
      console.error('Token refresh failed:', error);
    }

    return false;
  }

  // 로그인 페이지로 리다이렉트
  redirectToLogin() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = 'https://qandding.store/login';
  }
}

export const apiService = new ApiService();
```

### 2. 사용자 정보 조회

```javascript
// 내 정보 조회
export const getMyInfo = async () => {
  const response = await apiService.request('/users/me');
  if (response.ok) {
    return await response.json();
  }
  throw new Error('Failed to fetch user info');
};
```

### 3. 질문 생성

```javascript
// 질문 생성 (파일 업로드 포함)
export const createQuestion = async (formData) => {
  const accessToken = localStorage.getItem('accessToken');

  const response = await fetch(`${apiService.baseURL}/questions`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      // Content-Type은 multipart/form-data로 자동 설정됨
    },
    body: formData,
  });

  if (response.ok) {
    return await response.json();
  }
  throw new Error('Failed to create question');
};
```

### 4. 답변 생성

```javascript
// 답변 생성
export const createAnswer = async (formData) => {
  const accessToken = localStorage.getItem('accessToken');

  const response = await fetch(`${apiService.baseURL}/user-answers`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    body: formData,
  });

  if (response.ok) {
    return await response.json();
  }
  throw new Error('Failed to create answer');
};
```

## 에러 처리

### 1. 인증 에러 처리

```javascript
// API 요청 시 에러 처리
try {
  const data = await apiService.request('/users/me');
  // 성공 처리
} catch (error) {
  if (error.message === 'Access token not found') {
    // 로그인 필요
    navigate('/login');
  } else if (error.message.includes('401')) {
    // 인증 실패
    apiService.redirectToLogin();
  } else {
    // 기타 에러
    console.error('API request failed:', error);
  }
}
```

### 2. 토큰 만료 처리

```javascript
// 인터셉터를 사용한 자동 토큰 갱신
const setupAxiosInterceptors = (axiosInstance) => {
  axiosInstance.interceptors.response.use(
    (response) => response,
    async (error) => {
      if (error.response?.status === 401) {
        const refreshed = await apiService.refreshToken();
        if (refreshed) {
          // 원래 요청 재시도
          const config = error.config;
          config.headers.Authorization = `Bearer ${localStorage.getItem('accessToken')}`;
          return axiosInstance.request(config);
        } else {
          apiService.redirectToLogin();
        }
      }
      return Promise.reject(error);
    }
  );
};
```

## 보안 고려사항

### 1. 토큰 저장

```javascript
// 안전한 토큰 저장
class TokenStorage {
  static setAccessToken(token) {
    // Access Token은 메모리 또는 임시 저장소에 저장
    sessionStorage.setItem('accessToken', token);
  }

  static setRefreshToken(token) {
    // Refresh Token은 안전한 저장소에 저장
    localStorage.setItem('refreshToken', token);
  }

  static getAccessToken() {
    return sessionStorage.getItem('accessToken');
  }

  static getRefreshToken() {
    return localStorage.getItem('refreshToken');
  }

  static clearTokens() {
    sessionStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }
}
```

### 2. XSS 방지

```javascript
// 토큰 값 검증
const validateToken = (token) => {
  if (!token || typeof token !== 'string') {
    return false;
  }

  // JWT 형식 검증 (3개 부분으로 구성)
  const parts = token.split('.');
  if (parts.length !== 3) {
    return false;
  }

  return true;
};

// 토큰 저장 전 검증
if (validateToken(accessToken)) {
  TokenStorage.setAccessToken(accessToken);
} else {
  console.error('Invalid access token format');
}
```

## React Hook 예시

### 1. 인증 상태 관리

```javascript
import { useState, useEffect, createContext, useContext } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const accessToken = localStorage.getItem('accessToken');
      if (accessToken) {
        const userInfo = await getMyInfo();
        setUser(userInfo);
      }
    } catch (error) {
      console.error('Auth check failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const login = (accessToken, refreshToken) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    checkAuthStatus();
  };

  const logout = async () => {
    try {
      await apiService.request('/auth/logout', { method: 'POST' });
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setUser(null);
    }
  };

  return <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

### 2. 보호된 라우트

```javascript
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';

export const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!user) {
    return <Navigate to='/login' state={{ from: location }} replace />;
  }

  return children;
};
```

## 로그아웃 처리

### 1. 서버 로그아웃

```javascript
export const logout = async () => {
  try {
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      await fetch(`${apiService.baseURL}/auth/logout`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });
    }
  } catch (error) {
    console.error('Logout failed:', error);
  } finally {
    // 클라이언트 측 토큰 정리
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');

    // 로그인 페이지로 리다이렉트
    window.location.href = '/login';
  }
};
```

## 테스트

### 1. 토큰 검증 테스트

```javascript
// 토큰 유효성 검증
export const validateToken = async (token) => {
  try {
    const response = await fetch(`${apiService.baseURL}/auth/validate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ token }),
    });

    if (response.ok) {
      const data = await response.json();
      return data.valid;
    }
    return false;
  } catch (error) {
    console.error('Token validation failed:', error);
    return false;
  }
};
```

### 2. API 요청 테스트

```javascript
// API 요청 테스트
const testApiRequest = async () => {
  try {
    const userInfo = await getMyInfo();
    console.log('User info:', userInfo);
  } catch (error) {
    console.error('API test failed:', error);
  }
};
```

## 문제 해결

### 1. CORS 오류

```javascript
// CORS 설정 확인
const corsTest = async () => {
  try {
    const response = await fetch(`${apiService.baseURL}/health`);
    console.log('CORS test successful:', response.status);
  } catch (error) {
    console.error('CORS test failed:', error);
  }
};
```

### 2. 토큰 만료 처리

```javascript
// 토큰 만료 시 자동 갱신
const handleTokenExpiry = async () => {
  const refreshed = await apiService.refreshToken();
  if (refreshed) {
    console.log('Token refreshed successfully');
    // 원래 작업 재시도
  } else {
    console.log('Token refresh failed, redirecting to login');
    apiService.redirectToLogin();
  }
};
```

## 성능 최적화

### 1. 토큰 캐싱

```javascript
// 토큰 정보 캐싱
class TokenCache {
  constructor() {
    this.cache = new Map();
    this.ttl = 5 * 60 * 1000; // 5분
  }

  set(key, value) {
    this.cache.set(key, {
      value,
      timestamp: Date.now(),
    });
  }

  get(key) {
    const item = this.cache.get(key);
    if (item && Date.now() - item.timestamp < this.ttl) {
      return item.value;
    }
    this.cache.delete(key);
    return null;
  }

  clear() {
    this.cache.clear();
  }
}
```

### 2. 요청 중복 방지

```javascript
// 동일한 요청 중복 방지
class RequestDeduplicator {
  constructor() {
    this.pendingRequests = new Map();
  }

  async deduplicate(key, requestFn) {
    if (this.pendingRequests.has(key)) {
      return this.pendingRequests.get(key);
    }

    const promise = requestFn();
    this.pendingRequests.set(key, promise);

    try {
      const result = await promise;
      return result;
    } finally {
      this.pendingRequests.delete(key);
    }
  }
}
```

이 가이드를 따라 JWT 토큰 기반 인증을 구현하면 안전하고 효율적인 프론트엔드-백엔드 통합이 가능합니다.
