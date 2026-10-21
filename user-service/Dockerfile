FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./pom.xml
COPY event-contracts/pom.xml ./event-contracts/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml

COPY event-contracts/src ./event-contracts/src
COPY user-service/src ./user-service/src

COPY notification-service/pom.xml ./notification-service/pom.xml

RUN mvn clean package -pl user-service -am -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/user-service/target/user-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]