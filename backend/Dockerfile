# 文档 6.1：Docker 容器化部署
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

# 复制 JAR（构建阶段产出）
ARG JAR_FILE=target/express-delivery-*.jar
COPY ${JAR_FILE} app.jar

ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && cp /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 8080

# 文档 6.1：健康检查（本项目 context-path=/api）
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
