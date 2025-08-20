@echo off
echo 🚀 QANDDING Backend 서버 시작 중...

REM Java 버전 확인
echo 📋 Java 버전 확인 중...
java -version

REM Gradle 빌드 및 실행
echo 🔧 Gradle 빌드 중...
gradlew.bat clean build -x test

REM 서버 실행
echo 🌟 Spring Boot 서버 시작 중...
gradlew.bat bootRun

pause
