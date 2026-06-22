FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

COPY src/ src/
RUN ./mvnw package -DskipTests \
    && cp target/*.jar application.jar

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder --chown=1001:1001 /workspace/application.jar application.jar

USER 1001:1001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]
