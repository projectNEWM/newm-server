#
# Build
#

FROM gradle:7-jdk21 AS build

RUN mkdir -p /home/gradle/newm-server
COPY --chown=gradle:gradle . /home/gradle/newm-server
WORKDIR /home/gradle/newm-server
RUN ./gradlew :newm-chain:shadowJar --no-daemon

#
# Run
#
FROM amazoncorretto:21-alpine

RUN adduser -D -g '' newm-chain
RUN mkdir -p /opt/newm/newm-chain  \
    && chown -R newm-chain /opt/newm/newm-chain  \
    && chmod -R 755 /opt/newm

USER newm-chain

COPY --from=build /home/gradle/newm-server/newm-chain/build/libs/*.jar /opt/newm/newm-chain/newm-chain.jar

WORKDIR /opt/newm/newm-chain


EXPOSE 3737/tcp
ENTRYPOINT ["java", "-XX:+DisableAttachMechanism", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "-Xmx16384m", "-jar", "newm-chain.jar"]

LABEL org.opencontainers.image.title="NEWM-CHAIN"
LABEL org.opencontainers.image.description="A gRPC interface for communicating with the Cardano Blockchain"
LABEL org.opencontainers.image.url="https://newm.io"
LABEL org.opencontainers.image.source="https://github.com/projectNEWM/newm-server"
LABEL org.opencontainers.image.documentation="https://github.com/projectNEWM/newm-server"
LABEL org.opencontainers.image.authors="Project NEWM<connect@newm.io>"
LABEL org.opencontainers.image.vendor="Project NEWM<connect@newm.io>"

