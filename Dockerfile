FROM openjdk:17
EXPOSE 3939:3939
RUN mkdir /app
COPY ./newm-server/build/install/newm-server /app/
WORKDIR app/bin
CMD ["./newm-server"]