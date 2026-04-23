FROM amazoncorretto:21-alpine3.23
ARG JAR_FILE=target/aircontrol*.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]