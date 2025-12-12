FROM eclipse-temurin:8-jre-alpine

WORKDIR /app

ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata curl \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

COPY target/rule_engine-*.jar app.jar

RUN addgroup -S spring && adduser -S rule_engine -G spring \
    && mkdir -p /var/log/rule_engine \
    && chown -R rule_engine:spring /var/log/rule_engine
USER rule_engine:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV LOG_DIR="/var/log/rule_engine"

ENTRYPOINT ["sh", "-c", "mkdir -p ${LOG_DIR} && java $JAVA_OPTS -jar /app/app.jar"]
