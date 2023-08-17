#
# Build
#

FROM gradle:7-jdk17 AS build
RUN mkdir -p /home/gradle/newm-server
COPY --chown=gradle:gradle . /home/gradle/newm-server
WORKDIR /home/gradle/newm-server
RUN ./gradlew :newm-chain:shadowJar --no-daemon

#
# Run
#
FROM amazoncorretto:17-alpine

RUN adduser -D -g '' newm-chain

RUN mkdir -p /opt/newm/newm-chain
RUN chown -R newm-chain /opt/newm/newm-chain
RUN chmod -R 755 /opt/newm

USER newm-chain

COPY --from=build /home/gradle/newm-server/newm-chain/build/libs/*.jar /opt/newm/newm-chain/newm-chain.jar

WORKDIR /opt/newm/newm-chain


EXPOSE 3737:3737
ENTRYPOINT exec java -XX:+DisableAttachMechanism -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xmx16384m -jar newm-chain.jar
