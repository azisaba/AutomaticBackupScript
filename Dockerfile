FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:25-jre AS runner
WORKDIR /app
COPY --from=builder /app/build/libs/AutomaticBackupScript.jar /
ENTRYPOINT [ "java", "-jar", "/AutomaticBackupScript.jar"]
