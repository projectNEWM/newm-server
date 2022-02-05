FROM gradle:jdk17
EXPOSE 3939:3939

WORKDIR /app
COPY . .
USER root
RUN chown -R gradle .
USER gradle
RUN gradle build

ENTRYPOINT ["gradle","run"]
