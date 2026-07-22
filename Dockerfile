# Package a PRE-BUILT jar into a slim runtime image.
#
# The jar is built on the HOST (setup.sh runs `./gradlew :scrum-mcp-server:bootJar`) — NOT inside
# Docker. Building Java in the container downloads Gradle + all deps over the network, which is slow
# and fails on locked-down / proxied Docker networks. Copying a ready jar needs no network and builds
# in seconds.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY scrum-mcp-server/build/libs/scrum-mcp-server-0.0.1-SNAPSHOT.jar app.jar

# The capability skills and the Scrum Master agent ride along as a PAYLOAD, not as something the
# server serves. A harness discovers both on the filesystem of the machine it runs on — the user's
# laptop — so shipping the image alone would deliver the MCP tools with no guidance on how to use
# them and no dedicated mode to use them from. Carrying them here keeps it to ONE artifact to move;
# `./scripts/docker.sh install-skills` copies them back out onto the user's disk, each to the
# directory its harness scans. Never read by the running server.
COPY .github/skills /app/skills
COPY .github/agents /app/agents

EXPOSE 8097
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
