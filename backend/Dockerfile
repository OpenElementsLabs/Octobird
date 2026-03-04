FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve
COPY src/ src/
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/octobird-*.jar app.jar
COPY --from=build /app/target/libs/ libs/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
