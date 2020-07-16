#
# Build stage
#
FROM maven:3.6.0-jdk-8-slim AS builder

WORKDIR /build

#Copy source code
COPY engine/src ./engine/src
COPY engine/pom.xml ./engine
# Build application
RUN cd engine && mvn -Dmaven.test.skip=true package install

#Copy source code
COPY mafia-telegram-bot/src ./mafia-telegram-bot/src
COPY mafia-telegram-bot/pom.xml ./mafia-telegram-bot
# Build application
RUN cd mafia-telegram-bot && mvn -Dmaven.test.skip=true package

#
# Package stage
#
FROM openjdk:8-jre-slim
ENV JAVA_OPTS=""
ENV APP_OPTS=""

WORKDIR /opt/app

COPY --from=builder /build/mafia-telegram-bot/target/*.jar app.jar

RUN chmod -R 777 /opt/app/

EXPOSE 6666
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS $APP_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar" ]