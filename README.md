## QANDDING Backend (Spring Boot + JPA + MySQL via Docker)

### 요구사항
- Java 17
- Spring Boot 3.3.x
- Spring Data JPA
- MySQL 8 (Docker)
- Gradle

### 시작 방법
1) MySQL 컨테이너 실행
```bash
docker compose up -d
```

2) 애플리케이션 설정 (`src/main/resources/application.yml`)
- 기본 DB 접속 정보는 도커 컴포즈와 일치합니다.
- 필요 시 환경변수로 `DB_USERNAME`, `DB_PASSWORD`를 주입할 수 있습니다.

3) 애플리케이션 실행
- 로컬 Gradle 사용 시:
```bash
gradle bootRun
```
- 또는 IDE에서 `QanddingApplication` 실행

4) 헬스 체크
```bash
curl http://localhost:8080/api/health
```

### 디렉토리 구조
```
src/
  main/
    java/
      com/
        qandding/
          QanddingApplication.java
          api/
            HealthController.java
    resources/
      application.yml
  test/
    java/
      com/
        qandding/
          QanddingApplicationTests.java
```

### 참고
- JPA `ddl-auto=update`는 개발 편의를 위한 설정입니다. 운영 환경에서는 마이그레이션 도구(Flyway/Liquibase) 사용을 권장합니다.
