#
# Build
#

FROM gradle:7-jdk21 AS build

RUN mkdir -p /home/gradle/newm-server
COPY --chown=gradle:gradle . /home/gradle/newm-server
WORKDIR /home/gradle/newm-server
RUN ./gradlew :newm-server:shadowJar --no-daemon

#
# Run
#
FROM amazoncorretto:21-alpine

RUN adduser -D -h /home/newm-server -g '' newm-server
RUN mkdir -p /opt/newm/newm-server  \
    && chown -R newm-server /opt/newm/newm-server  \
    && chmod -R 755 /opt/newm

USER newm-server

COPY --from=build /home/gradle/newm-server/newm-server/build/libs/*.jar /opt/newm/newm-server/newm-server.jar

WORKDIR /opt/newm/newm-server


EXPOSE 3939/tcp
ENTRYPOINT ["java", "-XX:+DisableAttachMechanism", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC", "-Xmx16384m", "-jar", "newm-server.jar"]

LABEL org.opencontainers.image.title="NEWM-SERVER"
LABEL org.opencontainers.image.description="Backend components to support mobile apps and the artist portal."
LABEL org.opencontainers.image.url="https://newm.io"
LABEL org.opencontainers.image.source="https://github.com/projectNEWM/newm-server"
LABEL org.opencontainers.image.documentation="https://github.com/projectNEWM/newm-server"
LABEL org.opencontainers.image.authors="Project NEWM<connect@newm.io>"
LABEL org.opencontainers.image.vendor="Project NEWM<connect@newm.io>"

