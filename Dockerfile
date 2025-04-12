FROM eclipse-temurin:21-jdk as build

WORKDIR /app

COPY . .
COPY settings.xml /app/settings.xml

RUN chmod +x ./mvnw

ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

RUN ./mvnw clean package -U -DskipTests -s /app/settings.xml \
    -Dnexus.username=${NEXUS_USERNAME} \
    -Dnexus.password=${NEXUS_PASSWORD}

FROM eclipse-temurin:21-jre

ARG PORT=8080
ENV PORT=${PORT}

COPY --from=build /app/target/*.jar app.jar

RUN useradd runtime
USER runtime

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]