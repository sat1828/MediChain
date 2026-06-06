FROM eclipse-temurin:21-jre-jammy AS base
WORKDIR /app
EXPOSE 8080

FROM base AS final
COPY target/medichain-*.jar app.jar
RUN useradd -m -u 1000 medichain && chown -R medichain:medichain /app
USER medichain

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
