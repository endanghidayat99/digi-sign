# syntax=docker/dockerfile:1.7
# =============================================================================
# DigiSign — satu image berisi backend Spring Boot + frontend Vuetify (static)
# Build:  ./build.sh   (paket deploy)  atau  docker build -t digisign:dev .
# =============================================================================

# ---------- 1. Build frontend (Vue 3 + Vuetify) ----------
# Stage build jalan di platform mesin build (hasilnya jar/static, tidak tergantung arsitektur)
# → tidak diemulasi saat build linux/amd64 di Mac ARM.
FROM --platform=$BUILDPLATFORM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---------- 2. Build backend (Spring Boot) ----------
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY backend/src ./src
# hasil build frontend disajikan Spring Boot sebagai static resource
COPY --from=frontend /app/dist ./src/main/resources/static
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/digisign.jar extract --layers --launcher --destination /app/extracted

# ---------- 3. Runtime ----------
FROM eclipse-temurin:21-jre
ARG APP_VERSION=dev
LABEL org.opencontainers.image.title="digisign" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.description="Tanda tangan digital dokumen internal dengan QR Code verifikasi"

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --home /app app \
    && mkdir -p /data/storage && chown -R app:app /data

WORKDIR /app
COPY --from=backend --chown=app:app /app/extracted/dependencies/ ./
COPY --from=backend --chown=app:app /app/extracted/spring-boot-loader/ ./
COPY --from=backend --chown=app:app /app/extracted/snapshot-dependencies/ ./
COPY --from=backend --chown=app:app /app/extracted/application/ ./

ENV APP_STORAGE_DIR=/data/storage \
    TZ=Asia/Makassar \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"

USER app
VOLUME ["/data"]
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
