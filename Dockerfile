FROM openjdk:17-jdk-slim

WORKDIR /app

# Gradle wrapper와 build.gradle 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew build -x test --no-daemon

# 실행 가능한 JAR 파일 추출
RUN mkdir -p build/libs
RUN find build -name "*.jar" -exec cp {} build/libs/ \;

# 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "build/libs/qandding-backend-0.0.1-SNAPSHOT.jar"]
