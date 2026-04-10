# ==========================================
# Stage 1: Build
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

# 容器里的工作目录切换到 /app
WORKDIR /app
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B

# 复制源码
COPY src ./src
# 打包 jar
RUN mvn package -DskipTests -B

# ==========================================
# Stage 2: Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非 root 用户 (安全实践：不要让应用以 root 身份运行)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从前面那个 build 阶段，把打好的 Jar 拿过来。
COPY --from=build /app/target/*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata

USER appuser

# 声明容器里应用监听 8080
EXPOSE 8080

# 容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]
