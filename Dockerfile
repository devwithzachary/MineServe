# Stage 1: Build MineServe Linux Engine & Web Admin
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY core ./core
COPY designsystem ./designsystem
COPY desktop ./desktop
COPY app/build.gradle.kts ./app/build.gradle.kts
COPY app/src/main/assets/webadmin ./app/src/main/assets/webadmin
COPY app/src/main/res/drawable/ic_launcher_logo.png ./app/src/main/res/drawable/ic_launcher_logo.png

RUN chmod +x ./gradlew && ./gradlew :desktop:installDist -PexcludeAndroid=true --no-daemon

# Stage 2: Lightweight Production Headless Container
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="MineServe" \
      org.opencontainers.image.description="Native Linux Minecraft Java, Modded, Proxy & Bedrock Server Manager" \
      org.opencontainers.image.url="https://mineserve.app" \
      org.opencontainers.image.source="https://github.com/devwithzachary/MineServe" \
      org.opencontainers.image.licenses="GPL-3.0"

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    iproute2 \
    procps \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/desktop/build/install/desktop /opt/mineserve
RUN ln -sf /opt/mineserve/bin/mineserve /usr/local/bin/mineserve && \
    mkdir -p /data

VOLUME ["/data"]

# 8080: MineServe Web Remote Admin UI
# 8081: Squaremap Live Web Map
# 25565: Minecraft Java Edition (TCP/UDP)
# 19132: Minecraft Bedrock Edition (UDP)
EXPOSE 8080/tcp 8081/tcp 25565/tcp 25565/udp 19132/udp

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/ >/dev/null || exit 1

ENTRYPOINT ["/opt/mineserve/bin/mineserve"]
CMD ["--headless", "--bind", "0.0.0.0", "--port", "8080", "--data-dir", "/data"]
