FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="Portfolio Team"
LABEL description="Portfolio Management Service - Phase 1 Migration"
LABEL version="1.0.0"

RUN addgroup -g 1000 portfolio && \
    adduser -u 1000 -G portfolio -s /bin/sh -D portfolio

WORKDIR /app

RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/UTC /etc/localtime && \
    echo "UTC" > /etc/timezone

COPY --chown=portfolio:portfolio target/portfolio-service-*.jar app.jar

RUN mkdir -p /app/logs /app/config && \
    chown -R portfolio:portfolio /app

USER portfolio

EXPOSE 8080 8081

ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=production"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
