# ================================================================
#  CoreNova Bank – Backend Dockerfile
#  PDF Phase 6: Docker & Deployment
#
#  Multi-stage build:
#    Stage 1 (builder) : Maven + JDK 17 → compiles & packages the jar
#    Stage 2 (runtime) : Eclipse Temurin JRE 17 (slim) → runs the jar
#
#  Result: final image excludes Maven, source code, and build cache —
#  typically ~220MB vs ~650MB for a single-stage JDK build.
# ================================================================

# ── Stage 1: Build ────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only the POM first — leverages Docker layer caching.
# Dependencies are re-downloaded only when pom.xml changes,
# not on every source code change.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ───────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Run as non-root user (security best practice)
RUN addgroup -S corenova && adduser -S corenova -G corenova

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create logs directory (mounted as volume in docker-compose)
RUN mkdir -p /app/logs && chown -R corenova:corenova /app

USER corenova

# Expose Spring Boot's default port
EXPOSE 8080

# JVM tuning for containerized environments:
#  -XX:+UseContainerSupport  : respects container memory limits (default since JDK 10, explicit here for clarity)
#  -XX:MaxRAMPercentage=75.0 : use up to 75% of container memory for heap
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check — hits Actuator's health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
