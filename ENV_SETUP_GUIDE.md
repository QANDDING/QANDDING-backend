# 환경 변수 설정 가이드 (하이브리드 방식)

## 🔐 **필수 환경 변수**

### **1. OAuth2 설정**

```bash
# Google OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# OAuth2 리다이렉트 설정
APP_OAUTH_ALLOWED_DOMAIN=your_domain.com
APP_OAUTH_REDIRECT_URL=https://your_frontend_domain.com
```

### **2. Redis 설정**

```bash
# Redis 연결 정보
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Docker 환경
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=
```

### **3. JWT 설정**

```bash
# JWT 시크릿 키
APP_JWT_SECRET=your_super_secret_jwt_key_2024

# 토큰 만료 시간 (밀리초)
APP_JWT_ACCESS_EXPIRATION=900000    # 15분
APP_JWT_REFRESH_EXPIRATION=604800000 # 7일

# JWT 쿠키 설정
APP_JWT_COOKIE_DOMAIN=your_domain.com  # 쿠키 도메인 (빈 값이면 현재 도메인)
APP_JWT_COOKIE_SECURE=true              # HTTPS 환경에서만 true, 개발환경은 false
```

### **4. CORS 설정**

```bash
# CORS 허용 설정
APP_CORS_ALLOWED_ORIGINS=https://your_frontend_domain.com
APP_CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,PATCH,OPTIONS
APP_CORS_ALLOWED_HEADERS=*
APP_CORS_EXPOSED_HEADERS=*
APP_CORS_ALLOW_CREDENTIALS=true
APP_CORS_MAX_AGE_SECONDS=3600
```

### **5. 데이터베이스 설정**

```bash
# MySQL 연결 정보
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/qandding
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
```

## 🚀 **환경별 설정**

### **개발 환경 (.env)**

```bash
# OAuth2
GOOGLE_CLIENT_ID=your_dev_google_client_id
GOOGLE_CLIENT_SECRET=your_dev_google_client_secret
APP_OAUTH_ALLOWED_DOMAIN=your_dev_domain.com
APP_OAUTH_REDIRECT_URL=http://localhost:3000

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
APP_JWT_SECRET=dev_jwt_secret_key_2024
APP_JWT_ACCESS_EXPIRATION=900000
APP_JWT_REFRESH_EXPIRATION=604800000
APP_JWT_COOKIE_DOMAIN=localhost
APP_JWT_COOKIE_SECURE=false

# CORS
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
APP_CORS_ALLOW_CREDENTIALS=true

# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/qandding_dev
DB_USERNAME=dev_user
DB_PASSWORD=dev_password
```

### **프로덕션 환경 (.env.production)**

```bash
# OAuth2
GOOGLE_CLIENT_ID=your_prod_google_client_id
GOOGLE_CLIENT_SECRET=your_prod_google_client_secret
APP_OAUTH_ALLOWED_DOMAIN=your_prod_domain.com
APP_OAUTH_REDIRECT_URL=https://your_prod_frontend_domain.com

# Redis
REDIS_HOST=your_redis_host
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# JWT
APP_JWT_SECRET=your_super_secure_prod_jwt_key_2024
APP_JWT_ACCESS_EXPIRATION=900000
APP_JWT_REFRESH_EXPIRATION=604800000

# CORS
APP_CORS_ALLOWED_ORIGINS=https://your_prod_frontend_domain.com
APP_CORS_ALLOW_CREDENTIALS=true

# Database
SPRING_DATASOURCE_URL=jdbc:mysql://your_db_host:3306/qandding_prod
DB_USERNAME=prod_user
DB_PASSWORD=prod_password
```

### **Docker 환경 (docker-compose.yaml)**

```yaml
version: '3.8'
services:
  app:
    environment:
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/qandding
      - DB_USERNAME=root
      - DB_PASSWORD=password
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - APP_JWT_SECRET=docker_jwt_secret_2024
      - APP_JWT_ACCESS_EXPIRATION=900000
      - APP_JWT_REFRESH_EXPIRATION=604800000
      - APP_CORS_ALLOW_CREDENTIALS=true
```

## 🔧 **설정 검증 방법**

### **1. Redis 연결 확인**

```bash
# Redis CLI로 연결 테스트
redis-cli -h localhost -p 6379
> ping
PONG
> exit
```

### **2. JWT 토큰 생성 테스트**

```bash
# 애플리케이션 시작 후 로그 확인
# "토큰 쌍 생성 완료" 메시지 확인
```

### **3. CORS 설정 확인**

```bash
# 브라우저 개발자 도구에서 CORS 오류 확인
# Network 탭에서 preflight 요청 성공 확인
```

### **4. 쿠키 설정 확인**

```bash
# 브라우저 개발자 도구 > Application > Cookies
# access_token 쿠키가 httpOnly로 설정되었는지 확인
```

## ⚠️ **보안 주의사항**

### **1. JWT 시크릿 키**

- 최소 32자 이상의 복잡한 문자열 사용
- 프로덕션 환경에서는 환경 변수로 관리
- 정기적으로 변경

### **2. Redis 보안**

- 프로덕션 환경에서는 비밀번호 설정
- 네트워크 접근 제한
- SSL/TLS 사용 고려

### **3. HTTPS 설정**

- 프로덕션 환경에서는 반드시 HTTPS 사용
- Secure 쿠키 설정으로 HTTP 전송 방지

### **4. 도메인 제한**

- OAuth2 허용 도메인을 실제 사용 도메인으로 제한
- CORS 허용 도메인을 필요한 도메인만 허용

## 🚨 **문제 해결**

### **1. Redis 연결 실패**

```bash
# Redis 서비스 상태 확인
systemctl status redis

# Redis 포트 확인
netstat -tlnp | grep 6379

# Redis 설정 파일 확인
cat /etc/redis/redis.conf
```

### **2. CORS 오류**

```bash
# CORS 설정 확인
# allow-credentials가 true로 설정되었는지 확인
# allowed-origins에 프론트엔드 도메인이 포함되었는지 확인
```

### **3. 쿠키 설정 실패**

```bash
# 쿠키 설정 확인
# httpOnly, secure, path 설정 확인
# 브라우저 개발자 도구에서 쿠키 상태 확인
```

### **4. JWT 토큰 생성 실패**

```bash
# JWT 시크릿 키 확인
# 환경 변수가 올바르게 설정되었는지 확인
# 애플리케이션 로그 확인
```
