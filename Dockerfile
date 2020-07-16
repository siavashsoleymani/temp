#
# Build stage
#
FROM maven:3.6.0-jdk-8-slim AS builder

WORKDIR /build
COPY pom.xml .

#Copy source code
COPY src ./src
# Build application
RUN mvn -Dmaven.test.skip=true package


#
# Package stage
#
FROM openjdk:8-jre-slim
ENV JAVA_OPTS=""
ENV APP_OPTS=""

WORKDIR /opt/app

COPY --from=builder /build/target/*.jar app.jar

RUN chmod -R 777 /opt/app/

EXPOSE 8080
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS $APP_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar" ]