# 1단계: 빌드 단계
FROM gradle:8.5.0-jdk21 AS builder
WORKDIR /app

# Gradle 캐시 활용
COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle build -x test --no-daemon || return 0

# 소스 복사 후 빌드
COPY . .
RUN gradle clean build -x test --no-daemon

# 2단계: 실행 단계
FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
