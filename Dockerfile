FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar --no-daemon
RUN cp build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre

WORKDIR /application

COPY --from=build --chown=10001:10001 /workspace/extracted/dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=10001:10001 /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/extracted/application/ ./

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]
