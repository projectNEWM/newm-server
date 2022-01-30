FROM openjdk:17
EXPOSE 3939:3939
RUN mkdir /app
COPY ./build/install/docker /app/
WORKDIR app/bin
CMD ["./docker"]