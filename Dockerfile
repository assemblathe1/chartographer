FROM adoptopenjdk/openjdk11:alpine-jre
ARG JAR_FILE=target/chartographer-1.0.0.jar
WORKDIR /opt
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar", "./pictures"]