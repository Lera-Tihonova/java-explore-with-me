FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=target/*.jar
# Пересборка: 2026-08-26
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]