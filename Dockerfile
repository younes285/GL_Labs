FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S library && adduser -S library -G library
COPY --from=build --chown=library:library /build/target/library-api-0.0.1-SNAPSHOT.jar /app/library-api.jar
USER library
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "/app/library-api.jar"]
