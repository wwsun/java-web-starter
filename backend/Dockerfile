# ==========================================
# Stage 1: Build
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ==========================================
# Stage 2: Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
