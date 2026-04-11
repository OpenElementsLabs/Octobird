FROM eclipse-temurin:21 AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw dependency:resolve
COPY src/ src/
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/octobird-*.jar app.jar
COPY --from=build /app/target/libs/ libs/
EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
