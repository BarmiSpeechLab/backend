# 1. 빌드 단계 (Builder)
# openjdk:17-jdk-slim 대신 eclipse-temurin 사용
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Gradle 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여
RUN chmod +x ./gradlew

# 라이브러리 설치
RUN ./gradlew dependencies --no-daemon

# 소스코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2. 실행 단계 (Runner)
# 실행할 때도 동일한 기반 이미지 사용
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 빌드 단계에서 만든 jar 파일만 쏙 가져오기
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]